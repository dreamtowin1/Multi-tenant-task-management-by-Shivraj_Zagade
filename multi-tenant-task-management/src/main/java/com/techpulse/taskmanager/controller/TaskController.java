package com.techpulse.taskmanager.controller;

import com.techpulse.taskmanager.dto.AssignTaskRequest;
import com.techpulse.taskmanager.dto.CreateTaskRequest;
import com.techpulse.taskmanager.dto.TaskResponse;
import com.techpulse.taskmanager.dto.UpdateTaskStatusRequest;
import com.techpulse.taskmanager.security.TenantPrincipal;
import com.techpulse.taskmanager.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<TaskResponse> createTask(@Valid @RequestBody CreateTaskRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(taskService.createTask(request));
    }

    @PutMapping("/{id}/assign")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<TaskResponse> assignTask(
        @PathVariable Long id,
        @Valid @RequestBody AssignTaskRequest request
    ) {
        return ResponseEntity.ok(taskService.assignTask(id, request));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<TaskResponse> updateTaskStatus(
        @PathVariable Long id,
        @Valid @RequestBody UpdateTaskStatusRequest request,
        @AuthenticationPrincipal TenantPrincipal principal
    ) {
        return ResponseEntity.ok(taskService.updateTaskStatus(id, request, principal));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<List<TaskResponse>> getTasks(@AuthenticationPrincipal TenantPrincipal principal) {
        return ResponseEntity.ok(taskService.getTasks(principal));
    }
}
