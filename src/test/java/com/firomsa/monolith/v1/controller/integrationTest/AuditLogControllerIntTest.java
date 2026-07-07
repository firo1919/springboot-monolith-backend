package com.firomsa.monolith.v1.controller.integrationTest;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.firomsa.monolith.dto.AuditLogFilterDTO;

class AuditLogControllerIntTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @WithMockUser(authorities = "SCOPE_ADMIN")
    void testGetAuditLogs() throws Exception {
        mockMvc.perform(get("/api/v1/admin/audit-logs")
                .param("page", "0")
                .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @WithMockUser(authorities = "SCOPE_ADMIN")
    void testGetAuditLogsWithFilters() throws Exception {
        mockMvc.perform(get("/api/v1/admin/audit-logs")
                .param("action", "CREATE")
                .param("status", "SUCCESS")
                .param("page", "0")
                .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @WithMockUser(authorities = "SCOPE_ADMIN")
    void testGetAuditLogStatistics() throws Exception {
        mockMvc.perform(get("/api/v1/admin/audit-logs/statistics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalLogs").exists())
                .andExpect(jsonPath("$.successCount").exists())
                .andExpect(jsonPath("$.failureCount").exists());
    }

    @Test
    @WithMockUser(authorities = "SCOPE_ADMIN")
    void testExportToCsv() throws Exception {
        AuditLogFilterDTO filter = new AuditLogFilterDTO(null, null, null, null, null, null,
                null, null, 0, 20, "timestamp,desc");

        mockMvc.perform(post("/api/v1/admin/audit-logs/export/csv")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(filter)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "SCOPE_ADMIN")
    void testExportToJson() throws Exception {
        AuditLogFilterDTO filter = new AuditLogFilterDTO(null, null, null, null, null, null,
                null, null, 0, 20, "timestamp,desc");

        mockMvc.perform(post("/api/v1/admin/audit-logs/export/json")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(filter)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "SCOPE_ADMIN")
    void testDeleteAuditLogsByDateRange() throws Exception {
        LocalDateTime startDate = LocalDateTime.now().minusDays(30);
        LocalDateTime endDate = LocalDateTime.now();

        mockMvc.perform(delete("/api/v1/admin/audit-logs")
                .with(csrf())
                .param("startDate", startDate.toString())
                .param("endDate", endDate.toString()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "SCOPE_EMPLOYEE")
    void testAccessDeniedForNonAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/admin/audit-logs"))
                .andExpect(status().isForbidden());
    }
}
