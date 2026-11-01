package net.lucid.moa.employee.dto.web.request;

import jakarta.validation.constraints.Size;

public record EmployeeApproveRequest(@Size(max = 500) String approvalComment) {
}
