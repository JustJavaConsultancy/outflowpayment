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
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service("accountingService")
public class AccountService {
    private final AuthenticationManager authenticationManager;
    private final AccountRepository accountRepository;
    private final JournalLineRepository journalLineRepository;
    private final AccountMapper accountMapper;
    private final TransactionService transactionService;
    @Autowired
    ObjectMapper objectMapper;
    public AccountService(AccountRepository accountRepository, AuthenticationManager authenticationManager,JournalLineRepository journalLineRepository, AccountMapper accountMapper, TransactionService transactionService) {
        this.accountRepository = accountRepository;
        this.journalLineRepository = journalLineRepository;
        this.accountMapper = accountMapper;
        this.transactionService = transactionService;
        this.authenticationManager = authenticationManager;
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
    public Transaction customerPaymentJournalEntry(DelegateExecution execution) {
        Map<String,Object> variables = execution.getVariables();
        System.out.println("The execution in customerPaymentJournalEntry=== "+variables);
        Map<String, Object> invoiceMap = objectMapper.convertValue(variables.get("invoice"), Map.class);


        TransactionDTO transactionDTO=TransactionDTO.builder()
                .amount((BigDecimal) invoiceMap.get("amount"))
                .beneficiaryAccount("Payment Gateway Account")
                .reference(invoiceMap.get("id").toString())
                .externalReference(invoiceMap.get("id").toString())
                .paymentType(PaymentType.INFLOW)
                .channel(variables.get("channel").toString())
                .sourceAccount(invoiceMap.get("customerName").toString())
                .transactionOwner(invoiceMap.get("merchantId").toString())
//                .invoice(invoiceDTO)
                .transactionDetails(invoiceMap)
                .status(Status.PAID)
                .build();
        Transaction transaction=transactionService.createEntity(transactionDTO);
        Account pgBnkAccount=getPGClearingAccount();
        Account payableAccount=getMerchantPayableAccount(invoiceMap.get("merchantId").toString());
//        Account payableAccount=getMerchantPayableAccount(invoiceDTO.getMerchantId());

        //First Entry
        debitCredit(payableAccount,pgBnkAccount,transaction);
        //Second Entry charge 10%
        charge10(payableAccount, getPGIncomeAccount(),transaction);
        return transaction;
    }
    public Transaction merchantPaymentJournalEntry(DelegateExecution execution) {
        String loginUser = execution.getVariable("sub").toString();
        Map<String,Object> variables = execution.getVariables();
        System.out.println("The execution in merchantPaymentJournalEntry=== "+variables);
        Map<String,Object> transactionDetails = objectMapper.convertValue(variables, Map.class);
        System.out.println("This is the transactionDetails::: "+ transactionDetails);

        TransactionDTO transactionDTO=TransactionDTO.builder()
                .amount(new BigDecimal(execution.getVariable("amountToSend").toString()))
                .beneficiaryAccount("Payment Gateway Account")
                .reference(execution.getVariable("confirmCode").toString())
                .externalReference(execution.getVariable("confirmCode").toString())
                .paymentType(PaymentType.OUTFLOW)
                .channel("CHANNEL")
                .sourceAccount(execution.getVariable("accNumber").toString())
                .transactionOwner(loginUser)
                .transactionDetails(transactionDetails)
                .status(Status.PAID)
                .build();
        Transaction transaction=transactionService.createEntity(transactionDTO);
        Account pgBnkAccount=getPGClearingAccount();
        Account merchantAccount=getMerchantPayableAccount(loginUser);

        //First Entry
        debitCredit(merchantAccount,pgBnkAccount,transaction);
        //Second Entry charge 10%
        charge10(merchantAccount, getPGIncomeAccount(),transaction);
        return transaction;
    }

    public void settlementJournalEntry(DelegateExecution execution){
        Map<String,Object> variables = execution.getVariables();
        System.out.println(" The execution in settlementJournalEntry==="+variables);
    }
    private void debitCredit(Account debit,Account credit,Transaction transaction){

        JournalLine journalLineDebit=new JournalLine();
        journalLineDebit.setAccount(debit);
        journalLineDebit.setAmount(transaction.getAmount());
        journalLineDebit.setCurrentBalance(debit.getBalance());
        journalLineDebit.setAccountEntryType(AccountEntryType.DEBIT);
        journalLineDebit.setTransaction(transaction);

        JournalLine journalLineCredit=new JournalLine();
        journalLineCredit.setAccount(credit);
        journalLineCredit.setAmount(transaction.getAmount());
        journalLineCredit.setAccountEntryType(AccountEntryType.CREDIT);
        journalLineCredit.setCurrentBalance(credit.getBalance());
        journalLineCredit.setTransaction(transaction);

        journalLineRepository.save(journalLineDebit);
        journalLineRepository.save(journalLineCredit);
        debit.setBalance(debit.getBalance().subtract(transaction.getAmount()));
        credit.setBalance(credit.getBalance().add(transaction.getAmount()));

        accountRepository.save(debit);
        accountRepository.save(credit);
    }
    private void charge10(Account debit,Account credit,Transaction transaction){

        BigDecimal chargeAmount=transaction.getAmount().multiply(new BigDecimal(0.1));
        JournalLine journalLineDebit=new JournalLine();
        journalLineDebit.setAccount(debit);
        journalLineDebit.setAmount(chargeAmount);
        journalLineDebit.setCurrentBalance(debit.getBalance());
        journalLineDebit.setAccountEntryType(AccountEntryType.DEBIT);
        journalLineDebit.setTransaction(transaction);

        JournalLine journalLineCredit=new JournalLine();
        journalLineCredit.setAccount(credit);
        journalLineCredit.setAmount(chargeAmount);
        journalLineCredit.setAccountEntryType(AccountEntryType.CREDIT);
        journalLineCredit.setCurrentBalance(credit.getBalance());
        journalLineCredit.setTransaction(transaction);

        journalLineRepository.save(journalLineDebit);
        journalLineRepository.save(journalLineCredit);
        debit.setBalance(debit.getBalance().subtract(chargeAmount));
        credit.setBalance(credit.getBalance().add(chargeAmount));

        accountRepository.save(debit);
        accountRepository.save(credit);
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
        Optional<Account> pgBankAccount = accountRepository.findByTypeAndOwnerId("BANK",null);
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
    public Account getMerchantBankAccount(String merchantId){
        return accountRepository.findByTypeAndOwnerId("BANK",merchantId)
                .orElseThrow();
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
