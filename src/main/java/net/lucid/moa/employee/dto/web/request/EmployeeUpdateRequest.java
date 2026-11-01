package net.lucid.moa.employee.dto.web.request;

/** Null and omitted fields both leave the existing value unchanged. */
public record EmployeeUpdateRequest(String employeeName, String departmentCode) {
}
