package net.techcrunch.outflowPayment.accounting;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.techcrunch.outflowPayment.account.AuthenticationManager;
import net.techcrunch.outflowPayment.invoice.Status;
import net.techcrunch.outflowPayment.merchant.MerchantDto;
import net.techcrunch.outflowPayment.transaction.PaymentType;
import net.techcrunch.outflowPayment.transaction.Transaction;
import net.techcrunch.outflowPayment.transaction.TransactionDTO;
import net.techcrunch.outflowPayment.transaction.TransactionService;
import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

@Service("accountingService")
public class AccountService {
    private final AuthenticationManager authenticationManager;
    private final AccountRepository accountRepository;
    private final JournalLineRepository journalLineRepository;
    private final AccountMapper accountMapper;
    private final TransactionService transactionService;
    private final ObjectMapper objectMapper;
    private final AccountingPostingService accountingPostingService;

    public AccountService(AccountRepository accountRepository, AuthenticationManager authenticationManager,
                          JournalLineRepository journalLineRepository, AccountMapper accountMapper,
                          TransactionService transactionService,
                          ObjectMapper objectMapper,
                          AccountingPostingService accountingPostingService) {
        this.accountRepository = accountRepository;
        this.journalLineRepository = journalLineRepository;
        this.accountMapper = accountMapper;
        this.transactionService = transactionService;
        this.authenticationManager = authenticationManager;
        this.objectMapper = objectMapper;
        this.accountingPostingService = accountingPostingService;
    }

    public AccountDTO get(String id) {
        return accountMapper.toDto(accountRepository.findById(id).orElseThrow());
    }

    public AccountDTO create(AccountDTO accountDTO) {
        return accountMapper.toDto(accountRepository.save(accountMapper.toEntity(accountDTO)));
    }

    public AccountDTO getByCode(String code) {
        return accountMapper.toDto(accountRepository.findByCode(code).orElseThrow());
    }

    public Transaction merchantPaymentJournalEntry(DelegateExecution execution) {
        String loginUser = execution.getVariable("merchantId").toString();
        Map<String,Object> variables = execution.getVariables();
        Map<String,Object> transactionDetails = objectMapper.convertValue(variables, Map.class);
        BigDecimal amount = new BigDecimal(execution.getVariable("amountToSend").toString());
        String postingKey = accountingPostingService.merchantOutflowPostingKey(execution);
        Optional<Map<String, Object>> completedPosting = accountingPostingService.findCompletedResponse(
                AccountingPostingService.MERCHANT_OUTFLOW_DEBIT,
                postingKey
        );
        if (completedPosting.isPresent()) {
            return objectMapper.convertValue(completedPosting.get(), Transaction.class);
        }

        AccountingPostingRecord postingRecord = accountingPostingService.begin(
                AccountingPostingService.MERCHANT_OUTFLOW_DEBIT,
                postingKey,
                amount,
                execution,
                transactionDetails
        );
        String ref = accountingPostingService.stableReference("out", postingKey);

        TransactionDTO transactionDTO=TransactionDTO.builder()
                .amount(amount)
                .beneficiaryAccount("Payment Gateway Account")
                .reference(ref)
                .externalReference(ref)
                .paymentType(PaymentType.OUTFLOW)
                .channel("merchant-payment")
                .sourceAccount(execution.getVariable("beneficiaryName").toString())
                .transactionOwner(loginUser)
                .transactionDetails(transactionDetails)
                .status(Status.PAID)
                .build();

        try {
            Transaction transaction=transactionService.createEntity(transactionDTO);
            Account pgBnkAccount=getPGClearingAccount();

            List<Account> allMerchantAccounts=getMerchantBankAccount(loginUser);
            Account merchantAccount = allMerchantAccounts.getFirst();

            //First Entry
            debitCredit(merchantAccount,pgBnkAccount,transaction);
            //Second Entry charge 10%
            charge10(merchantAccount, getPGIncomeAccount(),transaction);
            accountingPostingService.markCompleted(
                    postingRecord,
                    transaction.getReference(),
                    objectMapper.convertValue(transaction, Map.class)
            );
            return transaction;
        } catch (RuntimeException exception) {
            accountingPostingService.markFailed(postingRecord, Map.of("error", exception.getMessage()));
            throw exception;
        }
    }

    public Transaction reverseMerchantPaymentJournalEntry(DelegateExecution execution) {
        String loginUser = execution.getVariable("merchantId").toString();
        Map<String,Object> variables = execution.getVariables();
        Map<String,Object> transactionDetails = objectMapper.convertValue(variables, Map.class);
        BigDecimal amount = new BigDecimal(execution.getVariable("amountToSend").toString());
        String postingKey = accountingPostingService.outflowReversalPostingKey(execution);
        Optional<Map<String, Object>> completedPosting = accountingPostingService.findCompletedResponse(
                AccountingPostingService.OUTFLOW_REVERSAL,
                postingKey
        );
        if (completedPosting.isPresent()) {
            return objectMapper.convertValue(completedPosting.get(), Transaction.class);
        }

        AccountingPostingRecord postingRecord = accountingPostingService.begin(
                AccountingPostingService.OUTFLOW_REVERSAL,
                postingKey,
                amount,
                execution,
                transactionDetails
        );
        String ref = accountingPostingService.stableReference("rev", postingKey);

        TransactionDTO transactionDTO = TransactionDTO.builder()
                .amount(amount)
                .beneficiaryAccount("Merchant Account")
                .reference(ref)
                .externalReference(ref)
                .paymentType(PaymentType.OUTFLOW)
                .channel("merchant-payment-reversal")
                .sourceAccount("Payment Gateway Account")
                .transactionOwner(loginUser)
                .transactionDetails(transactionDetails)
                .status(Status.PAID)
                .build();

        try {
            Transaction reversalTransaction = transactionService.createEntity(transactionDTO);
            Account pgBnkAccount = getPGClearingAccount();
            Account merchantAccount = getMerchantBankAccount(loginUser).getFirst();

            debitCredit(pgBnkAccount, merchantAccount, reversalTransaction);
            reverseCharge10(getPGIncomeAccount(), merchantAccount, reversalTransaction);

            accountingPostingService.markCompleted(
                    postingRecord,
                    reversalTransaction.getReference(),
                    objectMapper.convertValue(reversalTransaction, Map.class)
            );
            return reversalTransaction;
        } catch (RuntimeException exception) {
            accountingPostingService.markFailed(postingRecord, Map.of("error", exception.getMessage()));
            throw exception;
        }
    }

    public void settlementJournalEntry(DelegateExecution execution){
        Map<String,Object> variables = execution.getVariables();
        System.out.println(" The execution in settlementJournalEntry==="+variables);
    }
    private void debitCredit(Account debit,Account credit,Transaction transaction){

        //set local date to lagos timezone
        LocalDate today = LocalDate.now(ZoneId.of("Africa/Lagos"));

        //set journal line debit
        JournalLine journalLineDebit=new JournalLine();
        journalLineDebit.setAccount(debit);
        journalLineDebit.setAmount(transaction.getAmount());
        journalLineDebit.setCurrentBalance(debit.getBalance());
        journalLineDebit.setAccountEntryType(AccountEntryType.DEBIT);
        journalLineDebit.setTransaction(transaction);
        journalLineDebit.setExternalReference(transaction.getExternalReference());
        journalLineDebit.setBusinessDate(today);
        journalLineDebit.setCurrency("NGN");
        journalLineDebit.setEntryCategory(JournalEntryCategory.REFUND);
        journalLineDebit.setNarration(
                AccountEntryType.DEBIT + " " + transaction.getAmount() + " via " + transaction.getPaymentType());

        //set journal line credit
        JournalLine journalLineCredit=new JournalLine();
        journalLineCredit.setAccount(credit);
        journalLineCredit.setAmount(transaction.getAmount());
        journalLineCredit.setAccountEntryType(AccountEntryType.CREDIT);
        journalLineCredit.setCurrentBalance(credit.getBalance());
        journalLineCredit.setTransaction(transaction);
        journalLineCredit.setExternalReference(transaction.getExternalReference());
        journalLineCredit.setBusinessDate(today);
        journalLineCredit.setCurrency("NGN");
        journalLineCredit.setEntryCategory(JournalEntryCategory.REFUND);
        journalLineCredit.setNarration(
                AccountEntryType.CREDIT + " " + transaction.getAmount() + " via " + transaction.getPaymentType());

        journalLineRepository.save(journalLineDebit);
        journalLineRepository.save(journalLineCredit);
        debit.setBalance(debit.getBalance().subtract(transaction.getAmount()));
        credit.setBalance(credit.getBalance().add(transaction.getAmount()));

        accountRepository.save(debit);
        accountRepository.save(credit);
    }
    private void charge10(Account debit,Account credit,Transaction transaction){

        BigDecimal chargeAmount=transaction.getAmount().multiply(new BigDecimal(0.1));

        //set local date to lagos timezone
        LocalDate today = LocalDate.now(ZoneId.of("Africa/Lagos"));

        JournalLine journalLineDebit=new JournalLine();
        journalLineDebit.setAccount(debit);
        journalLineDebit.setAmount(chargeAmount);
        journalLineDebit.setCurrentBalance(debit.getBalance());
        journalLineDebit.setAccountEntryType(AccountEntryType.DEBIT);
        journalLineDebit.setTransaction(transaction);
        journalLineDebit.setExternalReference(transaction.getExternalReference());
        journalLineDebit.setBusinessDate(today);
        journalLineDebit.setCurrency("NGN");
        journalLineDebit.setEntryCategory(JournalEntryCategory.REFUND);
        journalLineDebit.setNarration(
                AccountEntryType.DEBIT + " " + transaction.getAmount() + " via " + transaction.getPaymentType() + "-FEE");

        JournalLine journalLineCredit=new JournalLine();
        journalLineCredit.setAccount(credit);
        journalLineCredit.setAmount(chargeAmount);
        journalLineCredit.setAccountEntryType(AccountEntryType.CREDIT);
        journalLineCredit.setCurrentBalance(credit.getBalance());
        journalLineCredit.setTransaction(transaction);
        journalLineCredit.setExternalReference(transaction.getExternalReference());
        journalLineCredit.setBusinessDate(today);
        journalLineCredit.setCurrency("NGN");
        journalLineCredit.setEntryCategory(JournalEntryCategory.REFUND);
        journalLineCredit.setNarration(
                AccountEntryType.CREDIT + " " + transaction.getAmount() + " via " + transaction.getPaymentType() + "-FEE");


        journalLineRepository.save(journalLineDebit);
        journalLineRepository.save(journalLineCredit);
        debit.setBalance(debit.getBalance().subtract(chargeAmount));
        credit.setBalance(credit.getBalance().add(chargeAmount));

        accountRepository.save(debit);
        accountRepository.save(credit);
    }

    private void reverseCharge10(Account debit, Account credit, Transaction transaction) {
        BigDecimal chargeAmount = transaction.getAmount().multiply(BigDecimal.valueOf(0.1));
        TransactionDTO feeReversalDTO = TransactionDTO.builder()
                .amount(chargeAmount)
                .beneficiaryAccount("Merchant Account")
                .reference(transaction.getReference() + "-FEE")
                .externalReference(transaction.getExternalReference() + "-FEE")
                .paymentType(PaymentType.OUTFLOW)
                .channel("merchant-payment-fee-reversal")
                .sourceAccount("Payment Gateway Income Account")
                .transactionOwner(transaction.getTransactionOwner())
                .transactionDetails(transaction.getTransactionDetails())
                .status(Status.PAID)
                .build();

        Transaction feeReversalTransaction = transactionService.createEntity(feeReversalDTO);
        debitCredit(debit, credit, feeReversalTransaction);
    }
    public AccountDTO update(String id, AccountDTO accountDTO) {
        return accountMapper.toDto(accountRepository.save(accountMapper.partialUpdate(accountDTO, accountRepository.findById(id).orElseThrow())));
    }
    public void delete(String id) {

        accountRepository.deleteById(id);
    }

    public Boolean createMerchantRelevantAccounts(MerchantDto merchantDto,
                                               Map<String, Object> bankDetail){
        AccountDTO receivable = AccountDTO.builder()
                .name(merchantDto.getBusinessName()+" Receivable")
                .balance(new BigDecimal(0.00))
                .code("receivable")
                .currency("NGN")
                .type("CHART_OF_ACCOUNTS")
                .ownerId(merchantDto.getBusinessIdentity())
                .build();
        AccountDTO payable = AccountDTO.builder()
                .name(merchantDto.getBusinessName()+" Payable")
                .balance(new BigDecimal(0.00))
                .code("payable")
                .currency("NGN")
                .type("CHART_OF_ACCOUNTS")
                .ownerId(merchantDto.getBusinessIdentity())
                .build();
        AccountDTO bankAccount  = AccountDTO.builder()
                .name((String) bankDetail.get("accName"))
                .balance(new BigDecimal(0.00))
                .code((String) bankDetail.get("bankName"))
                .currency("NGN")
                .type("BANK")
                .accNumber((String) bankDetail.get("accNumber"))
                .ownerId(merchantDto.getBusinessIdentity())
                .build();

        create(receivable,payable,bankAccount);
        return true;
    }

    private void create(AccountDTO receivable, AccountDTO payable, AccountDTO bankAccount) {
        accountRepository.save(accountMapper.toEntity(receivable));
        accountRepository.save(accountMapper.toEntity(payable));
        accountRepository.save(accountMapper.toEntity(bankAccount));
    }

    public Account getPGClearingAccount(){
        Optional<Account> pgBankAccount = accountRepository.findByCodeAndOwnerId("PG_CLEARING_ACCOUNT",null);
        if(pgBankAccount.isPresent())
            return pgBankAccount.get();
        else {
            Account account = new Account();
            account.setAccNumber("0000000000");
            account.setBalance(new BigDecimal(0.00));
            account.setCode("PG_CLEARING_ACCOUNT");
            account.setCurrency("NGN");
            account.setName("PG Clearing Account");
            account.setType("BANK");
            account.setOwnerId(null);
            account.setDescription("Payment Gateway Clearing Account");
            account=accountRepository.save(account);
            return account;
        }
    }
    public Account getPGIncomeAccount(){
        Optional<Account> pgIncomeAccount = accountRepository.findByCodeAndOwnerId("PG_INCOME_ACCOUNT",null);
        if(pgIncomeAccount.isPresent())
            return pgIncomeAccount.get();
        else {
            Account account = new Account();
            account.setAccNumber("1111111111");
            account.setBalance(new BigDecimal(0.00));
            account.setCode("PG_INCOME_ACCOUNT");
            account.setCurrency("NGN");
            account.setName("PG Income Account");
            account.setType("CHART_OF_ACCOUNTS");
            account.setOwnerId(null);
            account.setDescription("Payment Gateway Income Account");
            account=accountRepository.save(account);
            return account;
        }

    }
    public Account getMerchantPayableAccount(String merchantId){
        return accountRepository.findByCodeAndOwnerId("payable",merchantId.toString()).orElseThrow();
    }
    public List<Account> getMerchantBankAccount(String merchantId){
        return accountRepository.findByTypeAndOwnerIdOrderByIdAsc("BANK",merchantId);
    }
    public List<JournalLine> getMerchantJournalLinesByType(String merchantId, String type){
        return journalLineRepository.findByAccount_TypeAndAccount_OwnerId(type,merchantId);
    }
    public List<JournalLine> getMerchantJournalLinesByCode(String merchantId, String code){
        return journalLineRepository.findByAccount_CodeAndAccount_OwnerId(code,merchantId);
    }
    public List<JournalLine> getAllMerchantJournalLines(String merchantId){
        return journalLineRepository.findByAccount_OwnerId(merchantId);
    }
}
