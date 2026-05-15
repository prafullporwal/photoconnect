package com.photoconnect.customer.mapper;

import com.photoconnect.customer.domain.Customer;
import com.photoconnect.customer.dto.CreateCustomerRequest;
import com.photoconnect.customer.dto.CustomerResponse;
import com.photoconnect.customer.dto.UpdateCustomerRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface CustomerMapper {

    @Mapping(target = "id",        ignore = true)
    @Mapping(target = "userId",    ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version",   ignore = true)
    Customer toEntity(CreateCustomerRequest request);

    CustomerResponse toResponse(Customer customer);

    @Mapping(target = "id",        ignore = true)
    @Mapping(target = "userId",    ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version",   ignore = true)
    void updateEntity(UpdateCustomerRequest request, @MappingTarget Customer customer);
}
