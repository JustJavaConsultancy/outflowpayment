package net.techcrunch.outflowPayment.payment;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.techcrunch.outflowPayment.processes.CustomProcessService;
import net.techcrunch.outflowPayment.product.ProductDTO;
import net.techcrunch.outflowPayment.product.ProductService;
import org.flowable.engine.delegate.DelegateExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

@Component("paymentService")
public class PaymentService {
    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final CustomProcessService processService;
    private final ProductService productService;
    private final ObjectMapper objectMapper;


    public PaymentService(CustomProcessService processService,
                          ProductService productService,
                          ObjectMapper objectMapper) {
        this.processService = processService;
        this.productService = productService;
        this.objectMapper = objectMapper;
    }

    public Map<String, Object> checkFraud(DelegateExecution execution){
        FraudResponse fraudResponse = new FraudResponse();
        fraudResponse.setCode("00");
        fraudResponse.setMessage("Successful");
        return objectMapper.convertValue(fraudResponse, new TypeReference<Map<String, Object>>() {});
    }

    public Map<String, Object> checkAml(DelegateExecution execution){
        FraudResponse fraudResponse = new FraudResponse();
        fraudResponse.setCode("00");
        fraudResponse.setMessage("Successful");
        return objectMapper.convertValue(fraudResponse, new TypeReference<Map<String, Object>>() {});
    }

    public Map<String, Object> pgAdminApproval(DelegateExecution execution){
        AdminApproval adminApproval = new AdminApproval();
        adminApproval.setPaymentApprovalStatus("true");
        return objectMapper.convertValue(adminApproval, new TypeReference<Map<String, Object>>() {});
    }

    public Map<String, Object> authorize(DelegateExecution execution){
        AuthorizationResponse authorizationResponse = new AuthorizationResponse();
        authorizationResponse.setCode("00");
        authorizationResponse.setMessage("Successful");
        return objectMapper.convertValue(authorizationResponse, new TypeReference<Map<String, Object>>() {});
    }

    public void settlement(DelegateExecution execution){
        log.info("The execution while settlement== {}",execution);
    }

    public void reconciliation(DelegateExecution execution){
        log.info("The execution while reconciliation== {}",execution);
    }

    public void startPaymentProcess(Map<String,Object> variables,String merchantId){
        processService.startProcessByMessageStartEvent(merchantId,
                "processPayment",variables);
    }

    public void startPurchaseProcessWithCard(Map<String, String> cardInfo,
                                             Map<String,Object> payerInfo){
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
        Map<String,Object> variables=objectMapper.convertValue(paymentDTO, new TypeReference<Map<String, Object>>() {});
        variables.put("productName",product.getName());
        variables.put("productId",product.getId());
        variables.put("merchantId",product.getMerchantId());
        startPaymentProcess(variables, product.getMerchantId());
    }
}
