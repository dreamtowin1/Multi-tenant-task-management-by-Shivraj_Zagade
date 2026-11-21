package com.techpulse.taskmanager.repository;

import com.techpulse.taskmanager.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findAllByTenantId(String tenantId);

    List<Task> findAllByTenantIdAndAssignedTo(String tenantId, Long assignedTo);

    Optional<Task> findByIdAndTenantId(Long id, String tenantId);
}
