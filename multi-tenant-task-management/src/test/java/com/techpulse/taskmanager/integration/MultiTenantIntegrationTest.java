package com.techpulse.taskmanager.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class MultiTenantIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void fullMultiTenantLifecycleAndCrossTenantIsolation() throws Exception {
        // 1. Register Tenant A and Tenant B
        JsonNode tenantA = registerTenant("Alpha Corp", "admin@alpha.com", "Password123!");
        JsonNode tenantB = registerTenant("Beta Corp", "admin@beta.com", "Password123!");

        String tenantAId = tenantA.get("tenantId").asText();
        String tenantBId = tenantB.get("tenantId").asText();

        // 2. Unauthenticated access must fail with 401
        mockMvc.perform(get("/tasks"))
            .andExpect(status().isUnauthorized());

        // 3. Invalid login must fail with 401
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"admin@alpha.com\",\"password\":\"WrongPassword\"}"))
            .andExpect(status().isUnauthorized());

        // 4. Login as Admin A & Admin B
        String adminAToken = login("admin@alpha.com", "Password123!");
        String adminBToken = login("admin@beta.com", "Password123!");

        // 5. Admin A creates Manager A, User A1, User A2
        long managerAId = createUser(adminAToken, "manager@alpha.com", "Password123!", "MANAGER");
        long userA1Id = createUser(adminAToken, "user1@alpha.com", "Password123!", "USER");
        createUser(adminAToken, "user2@alpha.com", "Password123!", "USER");

        // Admin B creates Manager B and User B1
        createUser(adminBToken, "manager@beta.com", "Password123!", "MANAGER");
        long userB1Id = createUser(adminBToken, "user1@beta.com", "Password123!", "USER");

        // Verify GET /users only returns Tenant A users for Admin A
        mockMvc.perform(get("/users")
                .header("Authorization", "Bearer " + adminAToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(4)))
            .andExpect(jsonPath("$[*].tenantId", everyItem(is(tenantAId))));

        // 6. Login as Manager A, User A1, User A2, Manager B, User B1
        String managerAToken = login("manager@alpha.com", "Password123!");
        String userA1Token = login("user1@alpha.com", "Password123!");
        String userA2Token = login("user2@alpha.com", "Password123!");
        String managerBToken = login("manager@beta.com", "Password123!");
        String userB1Token = login("user1@beta.com", "Password123!");

        // Non-ADMIN cannot create users (403)
        mockMvc.perform(post("/users")
                .header("Authorization", "Bearer " + managerAToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"x@alpha.com\",\"password\":\"Password123!\",\"role\":\"USER\"}"))
            .andExpect(status().isForbidden());

        // 7. Manager A creates a task (even if client sneaks tenantId in JSON, it must be ignored)
        MvcResult createTaskResult = mockMvc.perform(post("/tasks")
                .header("Authorization", "Bearer " + managerAToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Alpha Task\",\"description\":\"Build Auth\",\"tenantId\":\"" + tenantBId + "\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.tenantId", is(tenantAId)))
            .andExpect(jsonPath("$.status", is("TODO")))
            .andReturn();

        long taskA1Id = objectMapper.readTree(createTaskResult.getResponse().getContentAsString()).get("id").asLong();

        // Non-MANAGER cannot create tasks (403)
        mockMvc.perform(post("/tasks")
                .header("Authorization", "Bearer " + userA1Token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Unauthorized Task\",\"description\":\"Should fail\"}"))
            .andExpect(status().isForbidden());

        // 8. Cross-tenant assignment prevention:
        // Manager A cannot assign Tenant A's task to Tenant B's user
        mockMvc.perform(put("/tasks/" + taskA1Id + "/assign")
                .header("Authorization", "Bearer " + managerAToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"userId\":" + userB1Id + "}"))
            .andExpect(status().isNotFound());

        // Manager B cannot assign Tenant A's task
        mockMvc.perform(put("/tasks/" + taskA1Id + "/assign")
                .header("Authorization", "Bearer " + managerBToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"userId\":" + userB1Id + "}"))
            .andExpect(status().isNotFound());

        // Valid assignment within Tenant A
        mockMvc.perform(put("/tasks/" + taskA1Id + "/assign")
                .header("Authorization", "Bearer " + managerAToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"userId\":" + userA1Id + "}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.assignedTo", is((int) userA1Id)));

        // 9. Status update authorization:
        // User A2 (not assigned) gets 403
        mockMvc.perform(put("/tasks/" + taskA1Id + "/status")
                .header("Authorization", "Bearer " + userA2Token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"IN_PROGRESS\"}"))
            .andExpect(status().isForbidden());

        // User B1 (different tenant) gets 404
        mockMvc.perform(put("/tasks/" + taskA1Id + "/status")
                .header("Authorization", "Bearer " + userB1Token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"IN_PROGRESS\"}"))
            .andExpect(status().isNotFound());

        // User A1 (assigned) succeeds
        mockMvc.perform(put("/tasks/" + taskA1Id + "/status")
                .header("Authorization", "Bearer " + userA1Token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"IN_PROGRESS\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status", is("IN_PROGRESS")));

        // 10. GET /tasks visibility checks
        mockMvc.perform(get("/tasks").header("Authorization", "Bearer " + userA1Token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)));

        mockMvc.perform(get("/tasks").header("Authorization", "Bearer " + userA2Token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(0)));

        mockMvc.perform(get("/tasks").header("Authorization", "Bearer " + adminAToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)));

        mockMvc.perform(get("/tasks").header("Authorization", "Bearer " + adminBToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(0)));
    }

    private JsonNode registerTenant(String name, String email, String password) throws Exception {
        String body = String.format(
            "{\"tenantName\":\"%s\",\"adminEmail\":\"%s\",\"adminPassword\":\"%s\"}",
            name, email, password
        );
        MvcResult result = mockMvc.perform(post("/tenant/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isCreated())
            .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private String login(String email, String password) throws Exception {
        String body = String.format("{\"email\":\"%s\",\"password\":\"%s\"}", email, password);
        MvcResult result = mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
    }

    private long createUser(String adminToken, String email, String password, String role) throws Exception {
        String body = String.format(
            "{\"email\":\"%s\",\"password\":\"%s\",\"role\":\"%s\"}",
            email, password, role
        );
        MvcResult result = mockMvc.perform(post("/users")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isCreated())
            .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }
}
