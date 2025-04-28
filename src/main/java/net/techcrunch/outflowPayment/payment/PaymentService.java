package net.techcrunch.outflowPayment.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.techcrunch.outflowPayment.processes.CustomProcessService;
import net.techcrunch.outflowPayment.product.ProductDTO;
import net.techcrunch.outflowPayment.product.ProductService;
import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

@Component("paymentService")
public class PaymentService {

    private final CustomProcessService processService;
    private final ProductService productService;

    @Autowired
    private ObjectMapper objectMapper;

    public PaymentService(CustomProcessService processService, ProductService productService) {
        this.processService = processService;
        this.productService = productService;
    }

    public FraudResponse checkFraud(DelegateExecution execution){
        System.out.println("checkFraud execution====="+execution.getVariables());
        FraudResponse fraudResponse = new FraudResponse();
        fraudResponse.setCode("00");
        fraudResponse.setMessage("Successful");
        //execution.setVariable("fraudResponse",fraudResponse);
        return fraudResponse;
    }

    public FraudResponse checkAml(DelegateExecution execution){
        FraudResponse fraudResponse = new FraudResponse();
        fraudResponse.setCode("00");
        fraudResponse.setMessage("Successful");
        return fraudResponse;
    }

    public AdminApproval pgAdminApproval(DelegateExecution execution){
        AdminApproval adminApproval = new AdminApproval();
        adminApproval.setPaymentApprovalStatus("true");
        return adminApproval;
    }

    public AuthorizationResponse authorize(DelegateExecution execution){

        System.out.println(" I'm authorizing here......"+execution.getVariables());
        AuthorizationResponse authorizationResponse = new AuthorizationResponse();
        authorizationResponse.setCode("00");

        authorizationResponse.setMessage("Successful");
        return authorizationResponse;
    }

    public void settlement(DelegateExecution execution){
        System.out.println("The execution while settlement=="+execution);
    }
    public void reconciliation(DelegateExecution execution){
        System.out.println("The execution while reconciliation=="+execution);
    }
    public void startPaymentProcess(Map<String,Object> variables,String merchantId){
        processService.startProcessByMessageStartEvent(merchantId,
                "processPayment",variables);
    }
    public void startPurchaseProcessWithCard(Map<String, String> cardInfo,
                                             Map<String,Object> payerInfo){


        System.out.println("Inside startPurchaseProcessWithCard==cardInfo=="+
                cardInfo +" payerInfo=="+payerInfo);

        BigDecimal amount = new BigDecimal(String.valueOf(payerInfo.get("price")).replace(",",""));

        Long productId = Long.parseLong(String.valueOf(payerInfo.get("productID")));
        PaymentDTO paymentDTO=PaymentDTO.builder()
                .amount(amount)
                .cardCvv(cardInfo.get("cvv"))
                .channel("card")
                .cardExpirationDate(cardInfo.get("expiryDate"))
                .cardHolderName(payerInfo.get("firstname") + " " + payerInfo.get("lastname"))
                .invoiceId(1L)
                .cardNumber( cardInfo.get("cardNumber"))
                .currency("NIG")
                .payerEmail((String) payerInfo.get("email"))
                .payerPhoneNumber((String) payerInfo.get("phoneNumber"))
                .build();
        ProductDTO product = productService.get(productId);
        Map<String,Object> variables=objectMapper.convertValue(paymentDTO,Map.class);
        variables.put("productName",product.getName());
        variables.put("productId",product.getId());
        variables.put("merchantId",product.getMerchantId());
        startPaymentProcess(variables, product.getMerchantId());
    }
}
