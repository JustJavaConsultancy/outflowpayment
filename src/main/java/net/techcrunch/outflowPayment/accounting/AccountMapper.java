package net.techcrunch.outflowPayment.accounting;

import org.mapstruct.*;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE,
        componentModel = MappingConstants.ComponentModel.SPRING)
public interface AccountMapper {
    Account toEntity(AccountDTO accountDTO);

    AccountDTO toDto(Account account);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    Account partialUpdate(AccountDTO accountDTO, @MappingTarget Account account);
}