package com.techpulse.taskmanager.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TenantContextTest {

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void setGetAndClear_shouldManageTenantIdInCurrentThread() {
        assertNull(TenantContext.getTenantId());
        TenantContext.setTenantId("tenant-abc");
        assertEquals("tenant-abc", TenantContext.getTenantId());
        TenantContext.clear();
        assertNull(TenantContext.getTenantId());
    }

    @Test
    void tenantContext_shouldBeIsolatedAcrossThreads() throws InterruptedException {
        TenantContext.setTenantId("tenant-main");
        AtomicReference<String> otherThreadValue = new AtomicReference<>("initial");

        Thread worker = new Thread(() -> otherThreadValue.set(TenantContext.getTenantId()));
        worker.start();
        worker.join();

        assertNull(otherThreadValue.get());
        assertEquals("tenant-main", TenantContext.getTenantId());
    }
}
