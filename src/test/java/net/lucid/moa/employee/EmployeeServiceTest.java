package net.lucid.moa.employee;

import net.lucid.moa.employee.dto.odata.response.ODataEmployeeResponse;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.http.HttpStatus;
import net.lucid.moa.employee.dto.converter.EmployeeConverter;
import net.lucid.moa.employee.dto.sap.request.*;
import net.lucid.moa.employee.dto.sap.response.*;
import net.lucid.moa.employee.dto.web.request.*;
import net.lucid.moa.employee.mock.MockEmployeeException;
import net.lucid.moa.employee.mock.contract.MockVersionedEmployee;
import net.lucid.moa.employee.repository.*;
import net.lucid.moa.employee.service.impl.EmployeeServiceImpl;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class EmployeeServiceTest {
    private final EmployeeRepository repository = mock(EmployeeRepository.class);
    private final EmployeeConverter converter = spy(Mappers.getMapper(EmployeeConverter.class));
    private final EmployeeServiceImpl service = new EmployeeServiceImpl(repository, converter);
    private final ODataEmployeeResponse employee = new ODataEmployeeResponse("E1", "Kim", "D1", LocalDate.of(2026, 1, 1));

    @Test
    void searchesAndMapsPageAndDefaults() {
        when(repository.searchEmployees(new EmployeeQuery(null, null, 1, 20)))
                .thenReturn(new SapEmployeeCollectionResponse(List.of(employee), 1L, null));
        var result = service.searchEmployees(new EmployeeSearchRequest(null, null, null, null));
        assertThat(result.content()).extracting("employeeId").containsExactly("E1");
        assertThat(result.totalElements()).isEqualTo(1);
        assertThat(result.page()).isEqualTo(1);
        assertThat(result.size()).isEqualTo(20);
        verify(converter).toWebResponses(List.of(employee));
    }

    @Test
    void readsAndCreatesThroughConverter() {
        when(repository.getEmployee("E1")).thenReturn(new MockVersionedEmployee(employee, "\"1\""));
        assertThat(service.getEmployee("E1").hireDate()).isEqualTo(employee.hireDate());
        var request = new EmployeeCreateRequest("Kim", "D1", employee.hireDate());
        when(repository.createEmployee(any())).thenReturn(employee);
        assertThat(service.createEmployee(request).employeeId()).isEqualTo("E1");
        verify(converter).toSapCreateRequest(request);
        verify(repository).createEmployee(new SapEmployeeCreateRequest("Kim", "D1", employee.hireDate()));
    }

    @Test
    void readsEtagBeforeUpdateAndDelete() {
        when(repository.getEmployee("E1")).thenReturn(new MockVersionedEmployee(employee, "\"7\""));
        var patch = new EmployeeUpdateRequest("Changed", null);
        service.updateEmployee("E1", patch);
        var order = inOrder(repository);
        order.verify(repository).getEmployee("E1");
        order.verify(repository).updateEmployee(eq("E1"), any(), eq("\"7\""));
        verify(converter).toSapUpdateRequest(patch);
        service.deleteEmployee("E1");
        order.verify(repository).getEmployee("E1");
        order.verify(repository).deleteEmployee("E1", "\"7\"");
    }

    @Test
    void mapsBoundActionRequestAndResponse() {
        when(repository.approveEmployee("E1", new SapEmployeeApproveRequest("OK")))
                .thenReturn(new SapEmployeeApproveResponse("E1", "Kim", "D1"));
        var request = new EmployeeApproveRequest("OK");
        assertThat(service.approveEmployee("E1", request).employeeName()).isEqualTo("Kim");
        verify(converter).toSapApproveRequest(request);
        verify(converter).toWebApproveResponse(new SapEmployeeApproveResponse("E1", "Kim", "D1"));
    }

    @Test
    void propagatesNotFoundAndConcurrencyFailure() {
        var notFound = new MockEmployeeException(HttpStatus.NOT_FOUND, "missing");
        when(repository.getEmployee("missing")).thenThrow(notFound);
        assertThatThrownBy(() -> service.deleteEmployee("missing")).isSameAs(notFound);
        verify(repository, never()).deleteEmployee(any(), any());
        when(repository.getEmployee("E1")).thenReturn(new MockVersionedEmployee(employee, "\"1\""));
        var conflict = new MockEmployeeException(HttpStatus.PRECONDITION_FAILED, "conflict");
        doThrow(conflict).when(repository).updateEmployee(eq("E1"), any(), any());
        assertThatThrownBy(() -> service.updateEmployee("E1", new EmployeeUpdateRequest(null, null)))
                .isSameAs(conflict);
    }
}
