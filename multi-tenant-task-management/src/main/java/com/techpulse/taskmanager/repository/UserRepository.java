package com.techpulse.taskmanager.repository;

import com.techpulse.taskmanager.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Used exclusively during unauthenticated login to resolve the user's credentials and tenant_id.
     */
    Optional<User> findByEmail(String email);

    boolean existsByEmailIgnoreCase(String email);

    List<User> findAllByTenantId(String tenantId);

    Optional<User> findByIdAndTenantId(Long id, String tenantId);

    Optional<User> findByEmailAndTenantId(String email, String tenantId);
}
