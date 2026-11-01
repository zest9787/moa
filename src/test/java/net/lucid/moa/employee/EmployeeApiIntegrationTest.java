package net.lucid.moa.employee;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("mock")
class EmployeeApiIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired JsonMapper json;

    @Test
    void servesAllSixEndpointsAndIgnoresNullPatchFields() throws Exception {
        var created = mvc.perform(post("/api/v1/employees").contentType(MediaType.APPLICATION_JSON).content("""
                {"employeeName":"Integration Employee","departmentCode":"DNEW","hireDate":"2026-10-30"}
                """)).andExpect(status().isCreated()).andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.hireDate").value("2026-10-30")).andReturn();
        var id = json.readTree(created.getResponse().getContentAsString()).path("data").path("employeeId").asString();
        String path = "/api/v1/employees/" + id;
        mvc.perform(get("/api/v1/employees").param("departmentCode", "DNEW"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.page").value(1)).andExpect(jsonPath("$.data.size").value(20));
        mvc.perform(patch(path).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true));
        mvc.perform(get(path)).andExpect(jsonPath("$.data.employeeName").value("Integration Employee"));
        mvc.perform(patch(path).contentType(MediaType.APPLICATION_JSON)
                .content("{\"employeeName\":null,\"departmentCode\":\"DCHANGED\"}"))
                .andExpect(status().isOk());
        mvc.perform(get(path)).andExpect(status().isOk()).andExpect(jsonPath("$.data.employeeName").value("Integration Employee"))
                .andExpect(jsonPath("$.data.departmentCode").value("DCHANGED"))
                .andExpect(jsonPath("$.data.hireDate").value("2026-10-30"));
        mvc.perform(post(path + "/approve").contentType(MediaType.APPLICATION_JSON).content("{\"approvalComment\":\"OK\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.employeeId").value(id))
                .andExpect(jsonPath("$.data.hireDate").doesNotExist());
        mvc.perform(delete(path)).andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true));
        mvc.perform(get(path)).andExpect(status().isNotFound()).andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void validatesQueryBoundsAndTypes() throws Exception {
        for (String query : new String[]{"page=0", "page=-1", "page=abc", "size=0", "size=101", "size=abc"}) {
            mvc.perform(get("/api/v1/employees?" + query)).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }
        mvc.perform(get("/api/v1/employees").param("employeeName", "nonexistent"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.content").isEmpty())
                .andExpect(jsonPath("$.data.totalElements").value(0));
    }

    @Test
    void validatesBodiesAndMissingEmployees() throws Exception {
        for (String body : new String[]{"{}", "{\"employeeName\":\" \",\"departmentCode\":\"D1\",\"hireDate\":\"2026-01-01\"}",
                "{\"employeeName\":\"Kim\",\"departmentCode\":\"D1\",\"hireDate\":\"invalid\"}",
                "{\"employeeName\":\"" + "a".repeat(101) + "\",\"departmentCode\":\"D1\",\"hireDate\":\"2026-01-01\"}"}) {
            mvc.perform(post("/api/v1/employees").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.success").value(false));
        }
        mvc.perform(post("/api/v1/employees/E0001/approve").contentType(MediaType.APPLICATION_JSON)
                .content("{\"approvalComment\":\"" + "a".repeat(501) + "\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(patch("/api/v1/employees/E0001").contentType(MediaType.APPLICATION_JSON).content("null"))
                .andExpect(status().isBadRequest());
        mvc.perform(delete("/api/v1/employees/missing")).andExpect(status().isNotFound());
        mvc.perform(post("/api/v1/employees/missing/approve").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void generatesSwaggerFromApiDocInterfaceWithoutSapModels() throws Exception {
        var result = mvc.perform(get("/v3/api-docs")).andExpect(status().isOk()).andReturn();
        var document = json.readTree(result.getResponse().getContentAsString());
        var paths = document.path("paths");
        assertThat(paths.path("/api/v1/employees").path("get").path("summary").asString()).isEqualTo("사원 목록 조회");
        assertThat(paths.path("/api/v1/employees").path("post").path("responses").has("201")).isTrue();
        assertThat(paths.path("/api/v1/employees/{employeeId}").has("get")).isTrue();
        assertThat(paths.path("/api/v1/employees/{employeeId}").has("patch")).isTrue();
        assertThat(paths.path("/api/v1/employees/{employeeId}").has("delete")).isTrue();
        assertThat(paths.path("/api/v1/employees/{employeeId}/approve").path("post").path("operationId").asString())
                .isEqualTo("approveEmployee");
        assertThat(document.path("components").path("schemas").toString()).doesNotContain("SapEmployee", "PatchField");
    }
}
