package net.lucid.moa.employee.dto.sap.request;

import java.time.LocalDate;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

// Field casing follows YAML; actual SAP metadata remains unverified.
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public record SapEmployeeCreateRequest(
        String employeeName,
        String departmentCode,
        LocalDate hireDate) {
}
