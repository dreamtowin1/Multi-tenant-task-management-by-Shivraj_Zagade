package com.techpulse.taskmanager.service;

import com.techpulse.taskmanager.dto.AssignTaskRequest;
import com.techpulse.taskmanager.dto.CreateTaskRequest;
import com.techpulse.taskmanager.dto.TaskResponse;
import com.techpulse.taskmanager.dto.UpdateTaskStatusRequest;
import com.techpulse.taskmanager.entity.Role;
import com.techpulse.taskmanager.entity.Task;
import com.techpulse.taskmanager.entity.TaskStatus;
import com.techpulse.taskmanager.entity.User;
import com.techpulse.taskmanager.exception.ResourceNotFoundException;
import com.techpulse.taskmanager.exception.TenantAccessDeniedException;
import com.techpulse.taskmanager.repository.TaskRepository;
import com.techpulse.taskmanager.repository.UserRepository;
import com.techpulse.taskmanager.security.TenantContext;
import com.techpulse.taskmanager.security.TenantPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    public TaskService(TaskRepository taskRepository, UserRepository userRepository) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public TaskResponse createTask(CreateTaskRequest request) {
        String tenantId = requireTenantId();
        Task task = new Task(
            request.getTitle().trim(),
            request.getDescription(),
            TaskStatus.TODO,
            null,
            tenantId
        );
        return TaskResponse.fromEntity(taskRepository.save(task));
    }

    @Transactional
    public TaskResponse assignTask(Long taskId, AssignTaskRequest request) {
        String tenantId = requireTenantId();

        Task task = taskRepository.findByIdAndTenantId(taskId, tenantId)
            .orElseThrow(() -> new ResourceNotFoundException("Task not found in current tenant"));

        User assignee = userRepository.findByIdAndTenantId(request.getUserId(), tenantId)
            .orElseThrow(() -> new ResourceNotFoundException("Assignee user not found in current tenant"));

        task.setAssignedTo(assignee.getId());
        return TaskResponse.fromEntity(taskRepository.save(task));
    }

    @Transactional
    public TaskResponse updateTaskStatus(Long taskId, UpdateTaskStatusRequest request, TenantPrincipal principal) {
        String tenantId = requireTenantId();

        Task task = taskRepository.findByIdAndTenantId(taskId, tenantId)
            .orElseThrow(() -> new ResourceNotFoundException("Task not found in current tenant"));

        if (task.getAssignedTo() == null || !task.getAssignedTo().equals(principal.getUserId())) {
            throw new TenantAccessDeniedException("You can only update the status of tasks assigned to you");
        }

        task.setStatus(request.getStatus());
        return TaskResponse.fromEntity(taskRepository.save(task));
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> getTasks(TenantPrincipal principal) {
        String tenantId = requireTenantId();

        if (principal.getRole() == Role.USER) {
            return taskRepository.findAllByTenantIdAndAssignedTo(tenantId, principal.getUserId())
                .stream()
                .map(TaskResponse::fromEntity)
                .toList();
        }

        return taskRepository.findAllByTenantId(tenantId)
            .stream()
            .map(TaskResponse::fromEntity)
            .toList();
    }

    private String requireTenantId() {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null || tenantId.isBlank()) {
            throw new TenantAccessDeniedException("Tenant context is missing");
        }
        return tenantId;
    }
}
