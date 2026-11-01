package net.lucid.moa.employee;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import net.lucid.moa.employee.controller.EmployeeController;
import net.lucid.moa.employee.mock.MockEmployeeException;
import net.lucid.moa.employee.mock.MockEmployeeExceptionHandler;
import net.lucid.moa.employee.service.EmployeeService;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EmployeeController.class)
@Import(MockEmployeeExceptionHandler.class)
@ActiveProfiles("mock")
class EmployeeControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean EmployeeService service;

    @Test
    void mapsUpdateAndDeleteEtagConflictsTo412() throws Exception {
        doThrow(new MockEmployeeException(HttpStatus.PRECONDITION_FAILED, "ETag conflict"))
                .when(service).updateEmployee(eq("E1"), any());
        doThrow(new MockEmployeeException(HttpStatus.PRECONDITION_FAILED, "ETag conflict"))
                .when(service).deleteEmployee("E1");
        mvc.perform(patch("/api/v1/employees/E1").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isPreconditionFailed()).andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("ETag conflict"));
        mvc.perform(delete("/api/v1/employees/E1"))
                .andExpect(status().isPreconditionFailed()).andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void rejectsInvalidInputBeforeCallingService() throws Exception {
        mvc.perform(post("/api/v1/employees").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/employees").param("size", "101"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
}
