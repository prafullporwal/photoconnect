package com.photoconnect.reviews.mapper;

import com.photoconnect.reviews.domain.Review;
import com.photoconnect.reviews.dto.ReviewResponse;
import org.mapstruct.Mapper;

/**
 * Entity-to-DTO mapping generated at compile time by MapStruct. We list the
 * mapper here even though the mapping is one-to-one because:
 *   1. it keeps the controller honest — there is exactly one shape going out;
 *   2. when fields diverge later (e.g. masking the customer's name) only this
 *      class changes.
 */
@Mapper(componentModel = "spring")
public interface ReviewMapper {

    ReviewResponse toResponse(Review review);
}
