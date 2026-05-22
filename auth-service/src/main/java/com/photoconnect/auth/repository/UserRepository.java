package com.photoconnect.auth.repository;

import com.photoconnect.auth.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Active-only lookup. A soft-deleted user (deletedAt != null) must not
     * be allowed to log in even if the email matches.
     */
    Optional<User> findByEmailIgnoreCaseAndDeletedAtIsNull(String email);

    boolean existsByEmailIgnoreCaseAndDeletedAtIsNull(String email);

    Optional<User> findByPhoneAndDeletedAtIsNull(String phone);
}
