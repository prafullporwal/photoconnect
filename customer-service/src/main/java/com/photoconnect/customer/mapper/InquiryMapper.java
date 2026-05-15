package com.photoconnect.customer.mapper;

import com.photoconnect.customer.domain.Inquiry;
import com.photoconnect.customer.dto.InquiryResponse;
import org.mapstruct.Mapper;

/**
 * MapStruct mapper for {@link Inquiry} → DTO conversion.
 *
 * <p>No {@code toEntity} method here on purpose: inquiry creation needs both
 * the {@link com.photoconnect.customer.dto.CreateInquiryRequest} AND the
 * resolved photographer's {@code userId} (returned by Feign). The service
 * assembles the entity directly to keep all of that in one place.</p>
 */
@Mapper(componentModel = "spring")
public interface InquiryMapper {
    InquiryResponse toResponse(Inquiry inquiry);
}
