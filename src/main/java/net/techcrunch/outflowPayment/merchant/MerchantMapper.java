package net.techcrunch.outflowPayment.merchant;

import org.mapstruct.*;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public interface MerchantMapper {
    Merchant toEntity(MerchantDto merchantDto);

    MerchantDto toDto(Merchant merchant);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    Merchant partialUpdate(MerchantDto merchantDto, @MappingTarget Merchant merchant);
}