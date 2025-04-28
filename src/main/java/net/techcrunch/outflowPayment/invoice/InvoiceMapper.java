package net.techcrunch.outflowPayment.invoice;

import org.mapstruct.*;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public interface InvoiceMapper {
    Invoice toEntity(InvoiceDTO invoiceDTO);

    InvoiceDTO toDto(Invoice invoice);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    Invoice partialUpdate(InvoiceDTO invoiceDTO, @MappingTarget Invoice invoice);
}