package net.lucid.moa.employee.dto.web.response;

/** Approval specifies three fields, unlike the four-field detail response. */
public record EmployeeApproveResponse(String employeeId, String employeeName, String departmentCode) {
}
