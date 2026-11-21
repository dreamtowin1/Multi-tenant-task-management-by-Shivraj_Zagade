package com.techpulse.taskmanager.dto;

import com.techpulse.taskmanager.entity.Task;
import com.techpulse.taskmanager.entity.TaskStatus;

public class TaskResponse {

    private Long id;
    private String title;
    private String description;
    private TaskStatus status;
    private Long assignedTo;
    private String tenantId;

    public TaskResponse() {
    }

    public TaskResponse(Long id, String title, String description, TaskStatus status, Long assignedTo, String tenantId) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.status = status;
        this.assignedTo = assignedTo;
        this.tenantId = tenantId;
    }

    public static TaskResponse fromEntity(Task task) {
        return new TaskResponse(
            task.getId(),
            task.getTitle(),
            task.getDescription(),
            task.getStatus(),
            task.getAssignedTo(),
            task.getTenantId()
        );
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public Long getAssignedTo() {
        return assignedTo;
    }

    public String getTenantId() {
        return tenantId;
    }
}
