package com.photoconnect.customer.service;

import com.photoconnect.customer.domain.Customer;
import com.photoconnect.customer.dto.CreateCustomerRequest;
import com.photoconnect.customer.dto.CustomerResponse;
import com.photoconnect.customer.dto.UpdateCustomerRequest;
import com.photoconnect.customer.exception.CustomerAlreadyExistsException;
import com.photoconnect.customer.exception.CustomerNotFoundException;
import com.photoconnect.customer.mapper.CustomerMapper;
import com.photoconnect.customer.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** CRUD for customer profiles. Same pattern as PhotographerService. */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository repository;
    private final CustomerMapper mapper;

    public CustomerResponse createProfile(UUID userId, CreateCustomerRequest request) {
        if (repository.existsByUserId(userId)) {
            throw new CustomerAlreadyExistsException(userId);
        }
        Customer customer = mapper.toEntity(request);
        customer.setUserId(userId);
        Customer saved = repository.save(customer);
        log.info("Created customer profile {} for user {}", saved.getId(), userId);
        return mapper.toResponse(saved);
    }

    public CustomerResponse updateProfile(UUID userId, UpdateCustomerRequest request) {
        Customer customer = findByUserIdOrThrow(userId);
        mapper.updateEntity(request, customer);
        return mapper.toResponse(repository.save(customer));
    }

    @Transactional(readOnly = true)
    public CustomerResponse getOwnProfile(UUID userId) {
        return mapper.toResponse(findByUserIdOrThrow(userId));
    }

    public void deleteProfile(UUID userId) {
        Customer customer = findByUserIdOrThrow(userId);
        repository.delete(customer);
    }

    private Customer findByUserIdOrThrow(UUID userId) {
        return repository.findByUserId(userId)
                .orElseThrow(() -> new CustomerNotFoundException(userId));
    }
}
