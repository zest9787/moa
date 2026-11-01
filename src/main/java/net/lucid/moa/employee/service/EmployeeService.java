package net.lucid.moa.employee.service;

import net.lucid.moa.employee.dto.web.request.*;
import net.lucid.moa.employee.dto.web.response.*;
import net.lucid.moa.employee.mock.contract.MockPageResponse;

public interface EmployeeService {
    MockPageResponse<EmployeeResponse> searchEmployees(EmployeeSearchRequest request);
    EmployeeResponse getEmployee(String employeeId);
    EmployeeResponse createEmployee(EmployeeCreateRequest request);
    void updateEmployee(String employeeId, EmployeeUpdateRequest request);
    void deleteEmployee(String employeeId);
    EmployeeApproveResponse approveEmployee(String employeeId, EmployeeApproveRequest request);
}
