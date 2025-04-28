package net.techcrunch.outflowPayment.invoice;

import net.techcrunch.outflowPayment.product.ProductDTO;
import net.techcrunch.outflowPayment.product.ProductService;
import net.techcrunch.outflowPayment.util.NotFoundException;
import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;


@Service("invoiceService")
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final ProductService productService;
    private final InvoiceMapper invoiceMapper;

    public InvoiceService(final InvoiceRepository invoiceRepository,
                          ProductService productService, InvoiceMapper invoiceMapper) {
        this.invoiceRepository = invoiceRepository;
        this.productService = productService;
        this.invoiceMapper = invoiceMapper;
    }

    public List<InvoiceDTO> findAll() {
        final List<Invoice> invoices = invoiceRepository.findAll(Sort.by("id"));
        return invoices.stream()
                .map(invoice -> invoiceMapper.toDto(invoice))
                .toList();
    }

    public InvoiceDTO get(final Long id) {
        return invoiceRepository.findById(id)
                .map(invoice -> invoiceMapper.toDto(invoice))
                .orElseThrow(NotFoundException::new);
    }

    public InvoiceDTO createInvoice(DelegateExecution execution){
        System.out.println(" Invoice about to be created Here....."+execution.getVariables());

        ProductDTO product=productService.get((Long) execution.getVariable("productId"));
        InvoiceDTO invoiceDTO=InvoiceDTO.builder()
                .amount(new java.math.BigDecimal(execution.getVariable("amount").toString()))
                .customerEmail(execution.getVariable("payerEmail").toString())
                .customerName(execution.getVariable("cardHolderName").toString())
                .customerPhoneNumber(execution.getVariable("payerPhoneNumber").toString())
                .description("Payment for product "+execution.getVariable("productName").toString())
                .merchantId(execution.getVariable("merchantId").toString())
                .issueDate(java.time.LocalDate.now())
                .status(Status.NEW)
                .dueDate(java.time.LocalDate.now())
                .product(product)
                .build();
        invoiceDTO = create(invoiceDTO);
        System.out.println(" Invoice Created Here....."+execution.getVariables());
        return invoiceDTO;
    }

    public void updateStatus(DelegateExecution execution){
        System.out.println(" Updating status here....");
    }
    public InvoiceDTO create(final InvoiceDTO invoiceDTO) {
        Invoice invoice = new Invoice();
        invoice = invoiceMapper.toEntity(invoiceDTO);
        invoice= invoiceRepository.save(invoice);
        return invoiceMapper.toDto(invoice);
    }

    public void update(final Long id, final InvoiceDTO invoiceDTO) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(NotFoundException::new);
        invoice = invoiceMapper.toEntity(invoiceDTO);
        invoiceRepository.save(invoice);
    }

    public void delete(final Long id) {
        invoiceRepository.deleteById(id);
    }

/*    private InvoiceDTO mapToDTO(final Invoice invoice, final InvoiceDTO invoiceDTO) {
        invoiceDTO.setId(invoice.getId());
        invoiceDTO.setIssueDate(invoice.getIssueDate());
        invoiceDTO.setDueDate(invoice.getDueDate());
        invoiceDTO.setAmount(invoice.getAmount());
        invoiceDTO.setCustomerEmail(invoice.getCustomerEmail());
        invoiceDTO.setCustomerName(invoice.getCustomerName());
        invoiceDTO.setCustomerPhoneNumber(invoice.getCustomerPhoneNumber());
        invoiceDTO.setDescription(invoice.getDescription());
        invoiceDTO.setMerchantId(invoice.getMerchantId());
        invoiceDTO.setStatus(invoice.getStatus());
        invoiceDTO.setDateCreated(invoice.getDateCreated());
        invoiceDTO.setLastUpdated(invoice.getLastUpdated());
        invoiceDTO.setProduct(invoice.getProduct());
        return invoiceDTO;
    }*/

/*    private Invoice mapToEntity(final InvoiceDTO invoiceDTO, final Invoice invoice) {
        invoice.setCustomerEmail(invoiceDTO.getCustomerEmail());
        invoice.setCustomerName(invoiceDTO.getCustomerName());
        invoice.setCustomerPhoneNumber(invoiceDTO.getCustomerPhoneNumber());
        invoice.setDescription(invoiceDTO.getDescription());
        invoice.setMerchantId(invoiceDTO.getMerchantId());
        invoice.setIssueDate(invoiceDTO.getIssueDate());
        invoice.setDueDate(invoiceDTO.getDueDate());
        invoice.setAmount(invoiceDTO.getAmount());
        invoice.setStatus(invoiceDTO.getStatus());
        return invoice;
    }*/

}
