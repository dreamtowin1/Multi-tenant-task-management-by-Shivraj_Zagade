package com.techpulse.taskmanager.entity;

import jakarta.persistence.*;

@Entity
@Table(
    name = "tasks",
    indexes = {
        @Index(name = "idx_tasks_tenant_id", columnList = "tenant_id"),
        @Index(name = "idx_tasks_tenant_assigned", columnList = "tenant_id, assigned_to")
    }
)
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskStatus status = TaskStatus.TODO;

    @Column(name = "assigned_to")
    private Long assignedTo;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private String tenantId;

    public Task() {
    }

    public Task(String title, String description, TaskStatus status, Long assignedTo, String tenantId) {
        this.title = title;
        this.description = description;
        this.status = status != null ? status : TaskStatus.TODO;
        this.assignedTo = assignedTo;
        this.tenantId = tenantId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    public Long getAssignedTo() {
        return assignedTo;
    }

    public void setAssignedTo(Long assignedTo) {
        this.assignedTo = assignedTo;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }
}
