package net.techcrunch.outflowPayment.transaction;

import org.mapstruct.*;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public interface TransactionMapper {
    Transaction toEntity(TransactionDTO transactionDTO);

    TransactionDTO toDto(Transaction transaction);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    Transaction partialUpdate(TransactionDTO transactionDTO, @MappingTarget Transaction transaction);
}