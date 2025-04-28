package net.techcrunch.outflowPayment.processes;

import jakarta.servlet.http.HttpServletRequest;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.runtime.ProcessInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@Controller
public class ProcessController {

    @Autowired
    RuntimeService runtimeService;
    @PostMapping("process/{processId}")
    public String submitTaskForm(HttpServletRequest request,@PathVariable String processId,
                                 @RequestBody Map<String, Object> formData,
                                 final Model model) {
        formData.put("assignee","compliance");
        //System.out.println(" Entering the home................."+formData);
        ProcessInstance processInstance=runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey(processId)
                .variables(formData)
                .start();
        request.getSession(true).setAttribute(processId,processInstance.getProcessInstanceId());
        //System.out.println(" Process instance id......."+processInstance.getProcessInstanceId());

/*        System.out.println(" Starting a process here and the " +
                "submitted data=="+formData);*/
        return "tasks/task-form";
    }
}
