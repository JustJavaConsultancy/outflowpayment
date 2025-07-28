package net.techcrunch.outflowPayment.merchant;

import net.techcrunch.outflowPayment.account.AuthenticationManager;
import net.techcrunch.outflowPayment.accounting.Account;
import net.techcrunch.outflowPayment.accounting.AccountService;
import net.techcrunch.outflowPayment.accounting.JournalLine;
import net.techcrunch.outflowPayment.processes.CustomProcessService;
import net.techcrunch.outflowPayment.product.ProductDTO;
import net.techcrunch.outflowPayment.product.ProductRepository;
import net.techcrunch.outflowPayment.product.ProductService;

import net.techcrunch.outflowPayment.transaction.TransactionDTO;
import net.techcrunch.outflowPayment.transaction.TransactionService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service("merchantService")
public class MerchantService {
    private final AuthenticationManager authenticationManager;
    private final RuntimeService runtimeService;
    private final CustomProcessService processService;

    private final MerchantRepository merchantRepository;
    private final MerchantMapper merchantMapper;
    private final AccountService accountService;
    private final OrderRepository orderRepository;
    private final TransactionService transactionService;
    private final ProductService productService;
    private final AmqpTemplate rabbitTemplate;

    public MerchantService(AuthenticationManager authenticationManager,
                           RuntimeService runtimeService,
                           CustomProcessService processService,
                           AmqpTemplate rabbitTemplate,
                           MerchantRepository merchantRepository,
                           MerchantMapper merchantMapper, AccountService accountService,
                           OrderRepository orderRepository, TransactionService transactionService, ProductRepository productRepository, ProductService productService, AmqpTemplate rabbitTemplate1) {
        this.authenticationManager = authenticationManager;
        this.runtimeService = runtimeService;
        this.processService = processService;
        this.merchantRepository = merchantRepository;
        this.merchantMapper = merchantMapper;
        this.accountService = accountService;
        this.orderRepository = orderRepository;
        this.transactionService = transactionService;
        this.productService = productService;
        this.rabbitTemplate = rabbitTemplate;
    }
    public Map<String,Object> getMerchantStatus(String merchantId){
        Map<String,Object> result=new HashMap<String,Object>();
        // Check for active process instance

        ProcessInstance activeProcessInstance=processService
                .getProcessInstanceByBusinessKey(merchantId);
        if(activeProcessInstance==null) {
            result.put("status", "NEW");
            result.put("variables",new HashMap<>());
            return result;
        }else {
            Map<String,Object> processVariables=processService
                    .getProcessInstanceVariables(activeProcessInstance.getProcessInstanceId());
/*            System.out.println(" activeProcessInstance.getProcessVariables()=="+
                    processVariables);*/
            result.put("status",processVariables.get("onboardStatus"));
            result.put("variables",processVariables);
        }
        return result;
    }
    public ProcessInstance submitMyDetail(Map<String,Object> data){

        data.put("onboardStatus","SUBMITTED");
        String loginUser= (String) authenticationManager.get("sub");
        return processService
                .startProcess("merchantOnboardingProcess",loginUser,data);
    }
    public void startNewTransferProcess(Map<String, Object> data){
        String loginUser = (String) authenticationManager.get("sub");

        BigDecimal balanceTotal = myBankAccount().stream()
                .map(Account::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        data.put("balance",balanceTotal);
        rabbitTemplate.convertAndSend(
                "flowable.message.exchange",  // Exchange
                "outflowPayment.routingKey", // Routing key
                data);
//        return processService.startProcess("merchantInitiatedPayoutProcess", loginUser,data);
    }



    public MerchantDto get(Long id) {
        return merchantMapper.toDto(merchantRepository.findById(id).orElseThrow());
    }
    public MerchantDto create(MerchantDto merchantDto) {
        return  merchantMapper.toDto(merchantRepository.save(merchantMapper.toEntity(merchantDto)));
    }
    public MerchantDto createMerchant(DelegateExecution execution) {

        Map<String,Object> variables = execution.getVariables();

        MerchantDto  merchantDto = MerchantDto.builder()
                .businessIdentity((String) variables.get("initiator"))
                .businessName((String) variables.get("businessName"))
                .mode("test")
                .build();

        merchantDto=create(merchantDto);
        accountService.createMerchantRelevantAccounts(merchantDto,variables);
        System.out.println(" The execution at this stage=="+execution.getVariables()
        + " The merchantDto created =="+merchantDto);
        return merchantDto;
    }
    public MerchantDto updateMerchant(DelegateExecution execution) {
        Map<String,Object> variables = execution.getVariables();

        MerchantDto  merchantDto = (MerchantDto) variables.get("merchant");
        merchantDto.setMode("live");
        merchantDto=update(merchantDto.getId(),merchantDto);
        return merchantDto;
    }
    public void createMerchantTest(Map<String,Object> variables) {
        MerchantDto  merchantDto = MerchantDto.builder()
                .businessIdentity((String) variables.get("initiator"))
                .businessName((String) variables.get("businessName"))
                .build();

        merchantDto=create(merchantDto);
        accountService.createMerchantRelevantAccounts(merchantDto,variables);
/*        System.out.println(" The execution at this stage=="+variables
                + " The merchantDto created =="+merchantDto);*/
    }
    public MerchantDto update(Long id, MerchantDto merchantDto) {
        Merchant merchant = merchantRepository.findById(id).orElseThrow();
        merchantMapper.partialUpdate(merchantDto, merchant);
        return merchantMapper.toDto(merchantRepository.save(merchant));

    }
    public List<Order> myOrders(){
        String loginUser= (String) authenticationManager.get("sub");
        return orderRepository.findByMerchantIdOrderByDateCreatedAsc(loginUser);
    }
    public List<ProductDTO> myProducts(){
        String loginUser= (String) authenticationManager.get("sub");
        return productService.findAllByMerchantId(loginUser);
    }
    public List<JournalLine> myBalances(){
        String loginUser= (String) authenticationManager.get("sub");
        return accountService.getMerchantJournalLinesByCode(loginUser,"payable");
    }
    public List<JournalLine> myAllAccountEntries(){
        String loginUser= (String) authenticationManager.get("sub");
        return accountService.getAllMerchantJournalLines(loginUser);
    }
    public List<Account> myBankAccount(){
        String loginUser= (String) authenticationManager.get("sub");
        return accountService.getMerchantBankAccount(loginUser);
    }
    public Account myPayableAccount(){
        String loginUser= (String) authenticationManager.get("sub");
        return accountService.getMerchantPayableAccount(loginUser);
    }

    public List<TransactionDTO> myTransactions(){
        String loginUser = (String) authenticationManager.get("sub");
//        System.out.println(loginUser);
        return transactionService.getMerchantTransactions(loginUser);
    }

    public TransactionDTO singleTransaction(Long id){
        return transactionService.get(id);
    }
    public void monitorReceived(DelegateExecution execution){
        System.out.println(" The Delegate Task Here " +
                "in monitoring ===");
    }

}
