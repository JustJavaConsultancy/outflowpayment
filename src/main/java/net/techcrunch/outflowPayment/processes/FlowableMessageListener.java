package net.techcrunch.outflowPayment.processes;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
@Component
public class FlowableMessageListener {
    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    TaskService taskService;

    @RabbitListener(
            queues = "${message.ouflowPayment.queue}",
            containerFactory = "rabbitListenerContainerFactory"
    )
    public void handlePaymentMessage(List<Map<String, Object>> variables) {
        System.out.println("\nReceived variables: " + variables);
        int i = 1;
        for (Map<String, Object> variable : variables) {
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> transferDTO = mapper.convertValue(variable.get("TransferDTO"), Map.class);
//            if (transferDTO.get("recipientName") != null)
//                transferDTO.put("beneficiaryName", transferDTO.get("recipientName"));
            if (transferDTO.get("duration") != null){
                transferDTO.put("isRecurrent", true);
            }
            transferDTO.putIfAbsent("isRecurrent", false);
            String businessKey = (String) transferDTO.get("sub");
            runtimeService.startProcessInstanceByMessage(
                    "outflowPaymentMessage",
                    businessKey,
                    transferDTO
            );
            System.out.println("TransferDTO: " + transferDTO);
            System.out.println("\nDone processing variable: " + i++ +"\n");
        }
    }

    @RabbitListener(
            queues = "outflowPayment.task.verifier.queue",
            containerFactory = "rabbitListenerContainerFactory"
    )
    public void handleTaskVerifierCompletion(Map<String, Object> variables) {
//        System.out.println("This is the verifier completion listener:::" + variables);
        String taskId = (String) variables.get("taskId");
        variables.remove("taskId");
        taskService.complete(taskId, variables);
    }

    @RabbitListener(
            queues = "outflowPayment.task.authorizer.queue",
            containerFactory = "rabbitListenerContainerFactory"
    )
    public void handleTaskAuthorizerCompletion(Map<String, Object> variables){
//        System.out.println("This is the authorizer task completion listener:::" + variables);
        String taskId = (String) variables.get("taskId");
        variables.remove("taskId");
        taskService.complete(taskId, variables);
    }
}
