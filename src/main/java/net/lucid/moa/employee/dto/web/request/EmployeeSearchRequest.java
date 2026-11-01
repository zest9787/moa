package net.lucid.moa.employee.dto.web.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import io.swagger.v3.oas.annotations.media.Schema;

public record EmployeeSearchRequest(
        String employeeName,
        String departmentCode,
        @Min(1) @Schema(defaultValue = "1") Integer page,
        @Min(1) @Max(100) @Schema(defaultValue = "20") Integer size) {
    public EmployeeSearchRequest {
        page = page == null ? 1 : page;
        size = size == null ? 20 : size;
    }
}
