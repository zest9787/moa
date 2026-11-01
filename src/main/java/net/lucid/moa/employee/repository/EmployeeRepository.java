package net.lucid.moa.employee.repository;

import net.lucid.moa.employee.dto.odata.response.ODataEmployeeResponse;

import net.lucid.moa.employee.dto.sap.request.*;
import net.lucid.moa.employee.dto.sap.response.*;
import net.lucid.moa.employee.mock.contract.MockVersionedEmployee;

/** Temporary repository contract; real RapClient signatures are deliberately not assumed. */
public interface EmployeeRepository {
    SapEmployeeCollectionResponse searchEmployees(EmployeeQuery query);
    MockVersionedEmployee getEmployee(String employeeId);
    ODataEmployeeResponse createEmployee(SapEmployeeCreateRequest request);
    void updateEmployee(String employeeId, SapEmployeeUpdateRequest request, String expectedEtag);
    void deleteEmployee(String employeeId, String expectedEtag);
    SapEmployeeApproveResponse approveEmployee(String employeeId, SapEmployeeApproveRequest request);
}
