package net.lucid.moa.employee;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import net.lucid.moa.employee.mock.MockEmployeeException;
import net.lucid.moa.employee.mock.MockEmployeeRepositoryImpl;
import net.lucid.moa.employee.repository.EmployeeQuery;
import net.lucid.moa.employee.dto.sap.request.*;
import static org.assertj.core.api.Assertions.*;

class MockEmployeeRepositoryTest {
    private final MockEmployeeRepositoryImpl repository = new MockEmployeeRepositoryImpl();

    @Test
    void filtersOrdersCountsAndPagesIncludingDuplicateNamesAndEmptyResults() {
        var first = repository.searchEmployees(new EmployeeQuery("Sample", null, 1, 1));
        assertThat(first.count()).isEqualTo(2);
        assertThat(first.value()).extracting("employeeId").containsExactly("E0001");
        var second = repository.searchEmployees(new EmployeeQuery("Sample", null, 2, 1));
        assertThat(second.value()).extracting("employeeId").containsExactly("E0002");
        assertThat(repository.searchEmployees(new EmployeeQuery(null, "D002", 1, 20)).count()).isEqualTo(1);
        assertThat(repository.searchEmployees(new EmployeeQuery("missing", null, 1, 20)).value()).isEmpty();
        assertThat(repository.searchEmployees(new EmployeeQuery(null, null, Integer.MAX_VALUE, 100)).value()).isEmpty();
        assertThatThrownBy(() -> new EmployeeQuery(null, null, 0, 20)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void userInputIsLiteralDataInMockSearch() {
        var created = repository.createEmployee(new SapEmployeeCreateRequest("O'Brien & Kim", "D'1", LocalDate.now()));
        var result = repository.searchEmployees(new EmployeeQuery("O'Brien", "D'1", 1, 20));
        assertThat(result.value()).containsExactly(created);
    }

    @Test
    void createsPatchesApprovesAndDeletesWithVersionChecks() {
        var created = repository.createEmployee(new SapEmployeeCreateRequest("New", "D3", LocalDate.now()));
        var id = created.employeeId();
        var initial = repository.getEmployee(id);
        repository.updateEmployee(id, new SapEmployeeUpdateRequest(null, null), initial.etag());
        var patched = repository.getEmployee(id);
        assertThat(patched.employee().employeeName()).isEqualTo("New");
        assertThat(patched.employee().departmentCode()).isEqualTo("D3");
        assertThat(patched.etag()).isNotEqualTo(initial.etag());
        assertThat(repository.approveEmployee(id, new SapEmployeeApproveRequest("OK")).employeeId()).isEqualTo(id);
        assertThatThrownBy(() -> repository.deleteEmployee(id, initial.etag()))
                .isInstanceOfSatisfying(MockEmployeeException.class, e -> assertThat(e.status().value()).isEqualTo(412));
        repository.deleteEmployee(id, repository.getEmployee(id).etag());
        assertThatThrownBy(() -> repository.getEmployee(id)).isInstanceOfSatisfying(
                MockEmployeeException.class, e -> assertThat(e.status().value()).isEqualTo(404));
    }

    @Test
    void rejectsStaleUpdateAndMissingEmployee() {
        var before = repository.getEmployee("E0001");
        var request = new SapEmployeeUpdateRequest("Changed", null);
        repository.updateEmployee("E0001", request, before.etag());
        assertThatThrownBy(() -> repository.updateEmployee("E0001", request, before.etag()))
                .isInstanceOfSatisfying(MockEmployeeException.class, e -> assertThat(e.status().value()).isEqualTo(412));
        assertThatThrownBy(() -> repository.deleteEmployee("missing", "\"1\""))
                .isInstanceOfSatisfying(MockEmployeeException.class, e -> assertThat(e.status().value()).isEqualTo(404));
        assertThatThrownBy(() -> repository.approveEmployee("missing", new SapEmployeeApproveRequest(null)))
                .isInstanceOf(MockEmployeeException.class);
    }
}
