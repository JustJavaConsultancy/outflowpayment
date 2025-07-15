package net.techcrunch.outflowPayment.processes;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.flowable.engine.RuntimeService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
@Component
public class FlowableMessageListener {
    @Autowired
    private RuntimeService runtimeService;

    @RabbitListener(
            queues = "${message.ouflowPayment.queue}",
            containerFactory = "rabbitListenerContainerFactory"
    )
    public void handlePaymentMessage(List<Map<String, Object>> variables) {
        System.out.println("\nReceived variables: " + variables);
        int i = 1;
        for (Map<String, Object> variable : variables) {

            ObjectMapper mapper = new ObjectMapper();
            Map transferDTO = mapper.convertValue(variable.get("TransferDTO"), Map.class);
            if (transferDTO.get("recipientName") != null)
                transferDTO.put("beneficiaryName", transferDTO.get("recipientName"));
            if (transferDTO.get("duration") != null){
                transferDTO.put("isRecurrent", true);
            }
            transferDTO.putIfAbsent("isRecurrent", false);
            runtimeService.startProcessInstanceByMessage(
                    "outflowPaymentMessage",
                    transferDTO
            );
            System.out.println("TransferDTO: " + transferDTO);
            System.out.println("\nDone processing variable: " + i++ +"\n");
        }
    }
}
