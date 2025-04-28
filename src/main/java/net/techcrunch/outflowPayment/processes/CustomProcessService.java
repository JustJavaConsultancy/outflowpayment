package net.techcrunch.outflowPayment.processes;

import org.flowable.engine.RuntimeService;
import org.flowable.engine.runtime.ProcessInstance;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class CustomProcessService {

    private final RuntimeService runtimeService;

    public CustomProcessService(RuntimeService runtimeService, JdbcTemplate jdbcTemplate) {
        this.runtimeService = runtimeService;
    }

    public ProcessInstance startProcess(String procesKey,String businessKey,
                                        Map<String,Object> variables){

        ProcessInstance processInstance=getProcessInstanceByBusinessKey(businessKey);
        if(processInstance!=null)
            return processInstance;

        return runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey(procesKey)
                .businessKey(businessKey)
                .variables(variables)
                .start();
    }
    public ProcessInstance getProcessInstanceByBusinessKey(String businessKey){

        return runtimeService
                .createProcessInstanceQuery()
                .processInstanceBusinessKey(businessKey)
                .processDefinitionKey("merchantOnboardingProcess")
                .singleResult();
    }
    public Map getProcessInstanceVariables(String processInstanceId){
        return runtimeService.getVariables(processInstanceId);
    }

    public ProcessInstance starSimpleProcess(String procesKey){
        return runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey(procesKey)
                .start();
    }
    public ProcessInstance startProcessByMessageStartEvent(String businessKey,
                                                           String messageName,
                                                           Map<String,Object> variables){
        return runtimeService
                .startProcessInstanceByMessage(messageName,businessKey, variables);
    }
}
