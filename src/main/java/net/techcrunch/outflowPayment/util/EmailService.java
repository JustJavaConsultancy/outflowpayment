package net.techcrunch.outflowPayment.util;

import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.stereotype.Component;

@Component("emailService")
public class EmailService {

    public void sendMail(DelegateExecution execution){

        System.out.println("\n\n" + " Sending mail..execution variable=="
                +execution.getVariables() +" task "+execution.getCurrentActivityName());
    }
}
