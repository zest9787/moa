package net.lucid.moa.employee.controller;

import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import net.lucid.moa.employee.dto.web.request.*;
import net.lucid.moa.employee.dto.web.response.*;
import net.lucid.moa.employee.mock.contract.MockApiResponse;
import net.lucid.moa.employee.mock.contract.MockPageResponse;
import net.lucid.moa.employee.service.EmployeeService;

@RestController
@RequestMapping("/api/v1/employees")
@Profile("mock & !prod") // TODO replace temporary response contract before enabling outside mock.
public class EmployeeController implements EmployeeApiDoc {
    private final EmployeeService service;

    public EmployeeController(EmployeeService service) {
        this.service = service;
    }

    @Override
    @GetMapping
    public MockApiResponse<MockPageResponse<EmployeeResponse>> searchEmployees(
            @Valid @ModelAttribute EmployeeSearchRequest request) {
        return MockApiResponse.ok(service.searchEmployees(request));
    }

    @Override
    @GetMapping("/{employeeId}")
    public MockApiResponse<EmployeeResponse> getEmployee(@PathVariable("employeeId") String employeeId) {
        return MockApiResponse.ok(service.getEmployee(employeeId));
    }

    @Override
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MockApiResponse<EmployeeResponse> createEmployee(@Valid @RequestBody EmployeeCreateRequest request) {
        return MockApiResponse.ok(service.createEmployee(request));
    }

    @Override
    @PatchMapping("/{employeeId}")
    public MockApiResponse<Void> updateEmployee(@PathVariable("employeeId") String employeeId,
                                               @Valid @RequestBody EmployeeUpdateRequest request) {
        service.updateEmployee(employeeId, request);
        return MockApiResponse.ok(null);
    }

    @Override
    @DeleteMapping("/{employeeId}")
    public MockApiResponse<Void> deleteEmployee(@PathVariable("employeeId") String employeeId) {
        service.deleteEmployee(employeeId);
        return MockApiResponse.ok(null);
    }

    @Override
    @PostMapping("/{employeeId}/approve")
    public MockApiResponse<EmployeeApproveResponse> approveEmployee(@PathVariable("employeeId") String employeeId,
            @Valid @RequestBody EmployeeApproveRequest request) {
        return MockApiResponse.ok(service.approveEmployee(employeeId, request));
    }
}
