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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private TaskService taskService;

    @AfterEach
    void clearContext() {
        TenantContext.clear();
    }

    @Test
    void createTask_shouldUseTenantIdFromContext() {
        TenantContext.setTenantId("tenant-a");
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> {
            Task t = inv.getArgument(0);
            t.setId(100L);
            return t;
        });

        TaskResponse response = taskService.createTask(new CreateTaskRequest("Fix Bug", "Details"));

        assertEquals(100L, response.getId());
        assertEquals("tenant-a", response.getTenantId());
        assertEquals(TaskStatus.TODO, response.getStatus());
    }

    @Test
    void assignTask_crossTenantUser_shouldThrowNotFound() {
        TenantContext.setTenantId("tenant-a");
        Task task = new Task("Fix Bug", "Details", TaskStatus.TODO, null, "tenant-a");
        task.setId(1L);

        when(taskRepository.findByIdAndTenantId(1L, "tenant-a")).thenReturn(Optional.of(task));
        when(userRepository.findByIdAndTenantId(99L, "tenant-a")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
            () -> taskService.assignTask(1L, new AssignTaskRequest(99L)));
    }

    @Test
    void updateTaskStatus_byDifferentUser_shouldThrowForbidden() {
        TenantContext.setTenantId("tenant-a");
        Task task = new Task("Fix Bug", "Details", TaskStatus.TODO, 5L, "tenant-a");
        task.setId(1L);
        TenantPrincipal otherUser = new TenantPrincipal(6L, "other@acme.com", "", Role.USER, "tenant-a");

        when(taskRepository.findByIdAndTenantId(1L, "tenant-a")).thenReturn(Optional.of(task));

        assertThrows(TenantAccessDeniedException.class,
            () -> taskService.updateTaskStatus(1L, new UpdateTaskStatusRequest(TaskStatus.DONE), otherUser));
    }

    @Test
    void getTasks_userRole_shouldQueryAssignedTasksOnly() {
        TenantContext.setTenantId("tenant-a");
        TenantPrincipal user = new TenantPrincipal(5L, "dev@acme.com", "", Role.USER, "tenant-a");
        Task task = new Task("My Task", "Desc", TaskStatus.TODO, 5L, "tenant-a");
        task.setId(1L);

        when(taskRepository.findAllByTenantIdAndAssignedTo("tenant-a", 5L)).thenReturn(List.of(task));

        List<TaskResponse> tasks = taskService.getTasks(user);
        assertEquals(1, tasks.size());
        assertEquals(5L, tasks.get(0).getAssignedTo());
    }

    @Test
    void getTasks_adminRole_shouldQueryAllTenantTasks() {
        TenantContext.setTenantId("tenant-a");
        TenantPrincipal admin = new TenantPrincipal(1L, "admin@acme.com", "", Role.ADMIN, "tenant-a");
        Task task = new Task("Any Task", "Desc", TaskStatus.TODO, 5L, "tenant-a");
        task.setId(1L);

        when(taskRepository.findAllByTenantId("tenant-a")).thenReturn(List.of(task));

        List<TaskResponse> tasks = taskService.getTasks(admin);
        assertEquals(1, tasks.size());
    }

    @Test
    void createTask_withoutTenantContext_shouldThrowAccessDenied() {
        assertThrows(TenantAccessDeniedException.class,
            () -> taskService.createTask(new CreateTaskRequest("Title", "Desc")));
    }
}
