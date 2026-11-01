package net.lucid.moa.employee.service.impl;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import net.lucid.moa.employee.dto.converter.EmployeeConverter;
import net.lucid.moa.employee.dto.web.request.*;
import net.lucid.moa.employee.dto.web.response.*;
import net.lucid.moa.employee.mock.contract.MockPageResponse;
import net.lucid.moa.employee.repository.EmployeeQuery;
import net.lucid.moa.employee.repository.EmployeeRepository;
import net.lucid.moa.employee.service.EmployeeService;

@Service
@Profile("mock & !prod") // TODO remove when the real repository and common contracts are connected.
public class EmployeeServiceImpl implements EmployeeService {
    private final EmployeeRepository repository;
    private final EmployeeConverter converter;

    public EmployeeServiceImpl(EmployeeRepository repository, EmployeeConverter converter) {
        this.repository = repository;
        this.converter = converter;
    }

    @Override
    public MockPageResponse<EmployeeResponse> searchEmployees(EmployeeSearchRequest request) {
        var result = repository.searchEmployees(new EmployeeQuery(
                request.employeeName(), request.departmentCode(), request.page(), request.size()));
        // Mock always supplies an exact count and never emits a nextLink.
        return new MockPageResponse<>(converter.toWebResponses(result.value()),
                result.count(), request.page(), request.size());
    }

    @Override
    public EmployeeResponse getEmployee(String employeeId) {
        return converter.toWebResponse(repository.getEmployee(employeeId).employee());
    }

    @Override
    public EmployeeResponse createEmployee(EmployeeCreateRequest request) {
        return converter.toWebResponse(repository.createEmployee(converter.toSapCreateRequest(request)));
    }

    @Override
    public void updateEmployee(String employeeId, EmployeeUpdateRequest request) {
        var snapshot = repository.getEmployee(employeeId);
        repository.updateEmployee(employeeId, converter.toSapUpdateRequest(request), snapshot.etag());
    }

    @Override
    public void deleteEmployee(String employeeId) {
        var snapshot = repository.getEmployee(employeeId);
        repository.deleteEmployee(employeeId, snapshot.etag());
    }

    @Override
    public EmployeeApproveResponse approveEmployee(String employeeId, EmployeeApproveRequest request) {
        return converter.toWebApproveResponse(repository.approveEmployee(employeeId, converter.toSapApproveRequest(request)));
    }
}
