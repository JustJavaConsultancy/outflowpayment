package net.techcrunch.outflowPayment.transfer;

import net.techcrunch.outflowPayment.account.AuthenticationManager;
import net.techcrunch.outflowPayment.accounting.AccountService;
import net.techcrunch.outflowPayment.payment.PaymentService;
import lombok.RequiredArgsConstructor;
import org.flowable.engine.delegate.DelegateExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Service("transferService")
@RequiredArgsConstructor
public class TransferService {
    private static final Logger log = LoggerFactory.getLogger(TransferService.class);

    private final AuthenticationManager authenticationManager;
    private final PaymentService paymentService;
    private final BeneficiaryMapper beneficiaryMapper;
    private final BeneficiaryRepository beneficiaryRepository;
    private final AccountService accountingService;

    public void registerBeneficiary(DelegateExecution execution) {
        Map<String, String> fullName = getNames((String) execution.getVariable("beneficiaryName"));
        String accountNumber = (String) execution.getVariable("accNumber");
        String referenceCode = String.valueOf(System.currentTimeMillis());
        String merchantId = (String) execution.getVariable("merchantId");
        BeneficiaryDTO beneficiaryDTO = BeneficiaryDTO.builder()
                .firstName(fullName.get("firstName"))
                .lastName(fullName.get("lastName"))
                .accountNumber(accountNumber)
                .bankName((String) execution.getVariable("bankName"))
                .code(referenceCode)
                .merchantId(merchantId)
                .build();
        Beneficiary singleBeneficiary = beneficiaryRepository.findByAccountNumberAndMerchantId(accountNumber, merchantId).orElse(null);
        if (singleBeneficiary == null){
            beneficiaryRepository.save(beneficiaryMapper.toEntity(beneficiaryDTO));
        }

        log.info("Registers a new beneficiary=== {}",execution.getVariables());
    }

    public void verifyBeneficiary(DelegateExecution execution) {
        log.info("Verifies new beneficiary=== {}",execution.getVariables());
    }

    public void checkBalance(DelegateExecution execution) {
        log.info("Check balance=== {}",execution.getVariable("balance"));
        log.info("Check amount=== {}",execution.getVariable("amountToSend"));
        BigDecimal balance = new BigDecimal(String.valueOf(execution.getVariable("balance")));
        BigDecimal amount = new BigDecimal(String.valueOf(execution.getVariable("amountToSend")));

        if(balance.compareTo(amount) > 0) {
            execution.setVariable("isBalance", true);
        }
        else {
            execution.setVariable("isBalance", false);
        }
    }

    public void debitAccount(DelegateExecution execution) {
        accountingService.merchantPaymentJournalEntry(execution);
        log.info("Debits Merchant Account=== {}",execution.getVariables());
    }

    public void transferFund(DelegateExecution execution) {
        log.info("Transfers funds to beneficiary=== {}",execution.getVariables());
    }
    public void merchantAPI(DelegateExecution execution) {
        log.info("Merchant API=== {}",execution.getVariables());
    }


    public void sendNotification(DelegateExecution execution){
        log.info( "\n\nSending notification; task... {} ...execution variable== {}",
                execution.getCurrentActivityName(), execution.getVariables());
    }

    private Map<String, String> getNames(String fullName){
        Map<String, String> map = new HashMap<>();
        String[] names = fullName.split(" ");
        if (names.length > 0) {
            map.put("firstName", names[0]);
        }
        if (names.length > 1) {
            map.put("lastName", names[1]);
        } else {
            map.put("lastName", "");
        }
        return map;
    }
}
