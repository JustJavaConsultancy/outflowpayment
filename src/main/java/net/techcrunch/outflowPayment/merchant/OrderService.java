package net.techcrunch.outflowPayment.merchant;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.techcrunch.outflowPayment.invoice.InvoiceDTO;
import net.techcrunch.outflowPayment.invoice.InvoiceService;
import net.techcrunch.outflowPayment.invoice.Status;
import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service("orderService")
public class OrderService {
    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final InvoiceService invoiceService;
    @Autowired
    ObjectMapper objectMapper;

    public OrderService(OrderRepository orderRepository, OrderMapper orderMapper, InvoiceService invoiceService) {
        this.orderRepository = orderRepository;
        this.orderMapper = orderMapper;
        this.invoiceService = invoiceService;
    }
    public OrderDTO get(Long id) {
        return orderMapper.toDto(orderRepository.findById(id).orElseThrow());
    }

    public OrderDTO createOrder(DelegateExecution execution) {
        Map<String,Object> variables = execution.getVariables();
        System.out.println(" The execution in createOrder==="+variables);
        InvoiceDTO invoiceDTO=objectMapper.convertValue(variables.get("invoice"),InvoiceDTO.class);
        OrderDTO orderDTO=OrderDTO.builder()
                .merchantId((String) variables.get("merchantId"))
                .invoice(invoiceDTO)
                .build();
        orderDTO=create(orderDTO);
        invoiceDTO.setStatus(Status.PAID);
        invoiceService.update(invoiceDTO.getId(),invoiceDTO);
        System.out.println(" orderDTO after saving=="+orderDTO);
        return orderDTO;
    }
    public OrderDTO create(OrderDTO orderDTO) {
        return orderMapper.toDto(orderRepository.save(orderMapper.toEntity(orderDTO)));
    }

    public OrderDTO update(Long id, OrderDTO orderDTO) {
        Order order = orderRepository.findById(id).orElseThrow();
        orderMapper.partialUpdate(orderDTO, order);
        return orderMapper.toDto(orderRepository.save(order));
    }

    public void delete(Long id) {
        orderRepository.deleteById(id);
    }

    public OrderDTO getByInvoiceId(Long invoiceId) {
        return orderMapper.toDto(
                orderRepository
                .findByInvoiceId(invoiceId)
                .orElseThrow());
    }
}
