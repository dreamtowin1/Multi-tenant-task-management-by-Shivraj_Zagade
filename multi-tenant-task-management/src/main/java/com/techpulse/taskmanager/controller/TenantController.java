package com.techpulse.taskmanager.controller;

import com.techpulse.taskmanager.dto.TenantRegisterRequest;
import com.techpulse.taskmanager.dto.TenantRegisterResponse;
import com.techpulse.taskmanager.service.TenantService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/tenant")
public class TenantController {

    private final TenantService tenantService;

    public TenantController(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    @PostMapping("/register")
    public ResponseEntity<TenantRegisterResponse> registerTenant(@Valid @RequestBody TenantRegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tenantService.registerTenant(request));
    }
}
