package net.lucid.moa.employee.dto.web.request;

import java.time.LocalDate;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record EmployeeCreateRequest(
        @NotBlank @Size(max = 100) String employeeName,
        @NotBlank String departmentCode,
        @NotNull LocalDate hireDate) {
}
