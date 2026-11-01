package net.lucid.moa.employee.dto.web.response;

import java.time.LocalDate;
import io.swagger.v3.oas.annotations.media.Schema;

public record EmployeeResponse(
        @Schema(description = "사번") String employeeId,
        @Schema(description = "이름") String employeeName,
        @Schema(description = "부서코드") String departmentCode,
        @Schema(description = "입사일자") LocalDate hireDate) {
}
