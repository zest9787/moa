package net.lucid.moa.employee;

import net.lucid.moa.employee.dto.odata.response.ODataEmployeeResponse;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import tools.jackson.databind.json.JsonMapper;
import net.lucid.moa.employee.dto.converter.EmployeeConverter;
import net.lucid.moa.employee.dto.sap.request.*;
import net.lucid.moa.employee.dto.sap.response.*;
import net.lucid.moa.employee.dto.web.request.*;
import net.lucid.moa.employee.dto.web.response.*;
import static org.assertj.core.api.Assertions.assertThat;

class EmployeeConverterTest {
    private final EmployeeConverter converter = Mappers.getMapper(EmployeeConverter.class);
    private final JsonMapper json = JsonMapper.builder().build();

    @Test
    void mapsEveryCreateAndResponseField() {
        var date = LocalDate.of(2026, 10, 30);
        assertThat(converter.toSapCreateRequest(new EmployeeCreateRequest("Kim", "D1", date)))
                .isEqualTo(new SapEmployeeCreateRequest("Kim", "D1", date));
        var sap = new ODataEmployeeResponse("E1", "Kim", "D1", date);
        var expected = new EmployeeResponse("E1", "Kim", "D1", date);
        assertThat(converter.toWebResponse(sap)).isEqualTo(expected);
        assertThat(converter.toWebResponses(List.of(sap))).containsExactly(expected);
        assertThat(converter.toWebResponses(List.of())).isEmpty();
    }

    @Test
    void mapsApprovalWithoutInventingHireDate() {
        assertThat(converter.toSapApproveRequest(new EmployeeApproveRequest("Approved")))
                .isEqualTo(new SapEmployeeApproveRequest("Approved"));
        assertThat(converter.toWebApproveResponse(new SapEmployeeApproveResponse("E1", "Kim", "D1")))
                .isEqualTo(new EmployeeApproveResponse("E1", "Kim", "D1"));
    }

    @Test
    void omitsNullAndMissingFieldsFromODataPatch() {
        var missing = converter.toSapUpdateRequest(json.readValue("{}", EmployeeUpdateRequest.class));
        assertThat(missing.employeeName()).isNull();
        assertThat(json.writeValueAsString(missing)).isEqualTo("{}");

        var patch = converter.toSapUpdateRequest(json.readValue(
                "{\"employeeName\":null,\"departmentCode\":\"D2\"}", EmployeeUpdateRequest.class));
        assertThat(patch.employeeName()).isNull();
        assertThat(patch.departmentCode()).isEqualTo("D2");
        assertThat(json.readTree(json.writeValueAsString(patch)))
                .isEqualTo(json.readTree("{\"DepartmentCode\":\"D2\"}"));
    }

    @Test
    void usesYamlSapNamesAndReadsV4CollectionMetadata() {
        var sap = json.readValue("""
                {"value":[{"EmployeeId":"E1","EmployeeName":"Kim","DepartmentCode":"D1",
                "HireDate":"2026-10-30","Unknown":"ignored"}],"@odata.count":7,
                "@odata.nextLink":"Employees?$skiptoken=next","@odata.context":"ignored"}
                """, SapEmployeeCollectionResponse.class);
        assertThat(sap.count()).isEqualTo(7);
        assertThat(sap.nextLink()).isEqualTo("Employees?$skiptoken=next");
        assertThat(sap.value()).containsExactly(new ODataEmployeeResponse(
                "E1", "Kim", "D1", LocalDate.of(2026, 10, 30)));
        var request = new SapEmployeeCreateRequest("Kim", "D1", LocalDate.of(2026, 10, 30));
        assertThat(json.readTree(json.writeValueAsString(request))).isEqualTo(json.readTree(
                "{\"EmployeeName\":\"Kim\",\"DepartmentCode\":\"D1\",\"HireDate\":\"2026-10-30\"}"));
    }

    @Test
    void readsProvidedDataWrappedSample() throws Exception {
        var sample = java.nio.file.Files.readString(java.nio.file.Path.of(
                "api-specs/employee/samples/employee-search-response.json"));
        var response = json.readValue(sample, SapEmployeeCollectionResponse.class);
        assertThat(response.value()).containsExactly(new ODataEmployeeResponse(
                "222221c", "홍길동", "X001", LocalDate.of(2023, 1, 1)));
        assertThat(json.readTree(json.writeValueAsString(response)).has("data")).isTrue();
    }

    @Test
    void handlesNullSources() {
        assertThat(converter.toSapCreateRequest(null)).isNull();
        assertThat(converter.toSapUpdateRequest(null)).isNull();
        assertThat(converter.toSapApproveRequest(null)).isNull();
        assertThat(converter.toWebResponse(null)).isNull();
        assertThat(converter.toWebResponses(null)).isNull();
        assertThat(converter.toWebApproveResponse(null)).isNull();
    }
}
