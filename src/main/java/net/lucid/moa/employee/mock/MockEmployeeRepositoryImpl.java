package net.lucid.moa.employee.mock;

import net.lucid.moa.employee.dto.odata.response.ODataEmployeeResponse;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;
import net.lucid.moa.employee.dto.sap.request.*;
import net.lucid.moa.employee.dto.sap.response.*;
import net.lucid.moa.employee.mock.contract.MockVersionedEmployee;
import net.lucid.moa.employee.repository.EmployeeQuery;
import net.lucid.moa.employee.repository.EmployeeRepository;

/** In-memory sample data only. TODO replace with a real RapClient-backed repository. */
@Repository
@Profile("mock & !prod")
public class MockEmployeeRepositoryImpl implements EmployeeRepository {
    private final Map<String, MockVersionedEmployee> employees = new HashMap<>();
    private long sequence = 2;

    public MockEmployeeRepositoryImpl() {
        // Deliberately duplicated names demonstrate that search does not imply uniqueness.
        employees.put("E0001", versioned(new ODataEmployeeResponse(
                "E0001", "Sample Employee", "D001", LocalDate.of(2024, 1, 1)), 1));
        employees.put("E0002", versioned(new ODataEmployeeResponse(
                "E0002", "Sample Employee", "D002", LocalDate.of(2025, 2, 1)), 1));
    }

    @Override
    public synchronized SapEmployeeCollectionResponse searchEmployees(EmployeeQuery query) {
        var matches = employees.values().stream().map(MockVersionedEmployee::employee)
                .filter(e -> query.employeeName() == null || e.employeeName() != null
                        && e.employeeName().contains(query.employeeName()))
                .filter(e -> query.departmentCode() == null
                        || Objects.equals(e.departmentCode(), query.departmentCode()))
                .sorted(Comparator.comparing(ODataEmployeeResponse::employeeId)).toList();
        return new SapEmployeeCollectionResponse(matches.stream()
                .skip(query.offset()).limit(query.size()).toList(), (long) matches.size(), null);
    }

    @Override
    public synchronized MockVersionedEmployee getEmployee(String employeeId) {
        var result = employees.get(employeeId);
        if (result == null) {
            throw new MockEmployeeException(HttpStatus.NOT_FOUND, "Employee not found: " + employeeId);
        }
        return result;
    }

    @Override
    public synchronized ODataEmployeeResponse createEmployee(SapEmployeeCreateRequest request) {
        String id = "E%04d".formatted(++sequence);
        var employee = new ODataEmployeeResponse(id, request.employeeName(), request.departmentCode(), request.hireDate());
        employees.put(id, versioned(employee, 1));
        return employee;
    }

    @Override
    public synchronized void updateEmployee(String employeeId, SapEmployeeUpdateRequest request, String expectedEtag) {
        var current = requireVersion(employeeId, expectedEtag);
        var old = current.employee();
        var updated = new ODataEmployeeResponse(employeeId,
                request.employeeName() != null ? request.employeeName() : old.employeeName(),
                request.departmentCode() != null ? request.departmentCode() : old.departmentCode(),
                old.hireDate());
        employees.put(employeeId, versioned(updated, version(current) + 1));
    }

    @Override
    public synchronized void deleteEmployee(String employeeId, String expectedEtag) {
        requireVersion(employeeId, expectedEtag);
        employees.remove(employeeId);
    }

    @Override
    public synchronized SapEmployeeApproveResponse approveEmployee(String employeeId, SapEmployeeApproveRequest request) {
        Objects.requireNonNull(request);
        var current = getEmployee(employeeId);
        var employee = current.employee();
        // SAP approval business rules are unknown; only simulate a version change.
        employees.put(employeeId, versioned(employee, version(current) + 1));
        return new SapEmployeeApproveResponse(employeeId, employee.employeeName(), employee.departmentCode());
    }

    private MockVersionedEmployee requireVersion(String employeeId, String expectedEtag) {
        var current = getEmployee(employeeId);
        if (!Objects.equals(current.etag(), expectedEtag)) {
            throw new MockEmployeeException(HttpStatus.PRECONDITION_FAILED, "ETag conflict");
        }
        return current;
    }

    private static long version(MockVersionedEmployee employee) {
        return Long.parseLong(employee.etag().replace("\"", ""));
    }

    private static MockVersionedEmployee versioned(ODataEmployeeResponse employee, long version) {
        return new MockVersionedEmployee(employee, "\"" + version + "\"");
    }
}
