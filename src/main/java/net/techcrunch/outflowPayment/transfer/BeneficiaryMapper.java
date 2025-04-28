package net.techcrunch.outflowPayment.transfer;

import org.mapstruct.*;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public interface BeneficiaryMapper {
    Beneficiary toEntity(BeneficiaryDTO beneficiaryDTO);

    BeneficiaryDTO toDto(Beneficiary beneficiary);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    Beneficiary partialUpdate(BeneficiaryDTO beneficiaryDTO, @MappingTarget Beneficiary beneficiary);
}