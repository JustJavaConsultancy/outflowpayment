package net.techcrunch.outflowPayment.transfer;

import net.techcrunch.outflowPayment.account.AuthenticationManager;
import net.techcrunch.outflowPayment.accounting.AccountService;
import net.techcrunch.outflowPayment.payment.PaymentService;
import lombok.RequiredArgsConstructor;
import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Service("transferService")
@RequiredArgsConstructor
public class TransferService {
    private final AuthenticationManager authenticationManager;
    private final PaymentService paymentService;
    private final BeneficiaryMapper beneficiaryMapper;
    private final BeneficiaryRepository beneficiaryRepository;
    private final AccountService accountingService;

    public void registerBeneficiary(DelegateExecution execution) {
        Map<String, String> fullName = getNames((String) execution.getVariable("beneficiaryName"));
        BeneficiaryDTO beneficiaryDTO = BeneficiaryDTO.builder()
                .firstName(fullName.get("firstName"))
                .lastName(fullName.get("lastName"))
                .accountNumber((String) execution.getVariable("accNumber"))
                .bankName((String) execution.getVariable("bankName"))
                .code((String) execution.getVariable("confirmCode"))
                .build();
        Beneficiary beneficiary = beneficiaryMapper.toEntity(beneficiaryDTO);
        beneficiaryRepository.save(beneficiary);
        System.out.println("Registers a new beneficiary===" + execution.getVariables());
    }

    public void verifyBeneficiary(DelegateExecution execution) {
        System.out.println("Verifies new beneficiary===" + execution.getVariables());
    }

    /*public Map<String, Object> complianceCheck(DelegateExecution execution) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("fraudResponsse", paymentService.checkAml(execution));
        variables.put("valid",sufficientBalance(execution));
        return variables;
    }
    private Boolean sufficientBalance(DelegateExecution execution) {
        boolean valid =true;
        Map<String, Object> variables = execution.getVariables();
        Double amountToSend = Double.parseDouble((String) variables.get("amountToSend"));
        System.out.println("amountToSend=" + amountToSend);
        if (amountToSend > (Double) authenticationManager.get("balance")){
            valid=false;
        }
        return valid;
    }*/
    public Map<String, String> checkBalance(DelegateExecution execution) {
        Map<String, String> bool = new HashMap<>();
        System.out.println("Check balance===" + execution.getVariable("balance"));
        System.out.println("Check amount===" + execution.getVariable("amountToSend"));
        BigDecimal balance = new BigDecimal(String.valueOf(execution.getVariable("balance")));
        BigDecimal amount = new BigDecimal(String.valueOf(execution.getVariable("amountToSend")));
        System.out.println("Check balance===" +balance +" "+ execution.getVariables());
        if(balance.compareTo(amount) > 0) {
            bool.put("bool", "true");
            System.out.println(bool);
            return bool;
        }
        else {
            bool.put("bool", "false");
            System.out.println(bool);
            return bool;
        }
//        return "true";
    }

    public void debitAccount(DelegateExecution execution) {
        accountingService.merchantPaymentJournalEntry(execution);
        System.out.println("Debits Merchant Account===" + execution.getVariables());
    }

    public void transferFund(DelegateExecution execution) {
        System.out.println("Transfers funds to beneficiary===" + execution.getVariables());
    }
    public void merchantAPI(DelegateExecution execution) {
        System.out.println("Merchant API===" + execution.getVariables());
    }


    public void sendNotification(DelegateExecution execution){

        System.out.println(String.format("\n\n" + " Sending notification; task...%s ...execution variable==%s",
                execution.getCurrentActivityName(), execution.getVariables()));
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
