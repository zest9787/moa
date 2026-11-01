package net.lucid.moa.employee.dto.odata.response;

import java.time.LocalDate;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public record ODataEmployeeResponse(
        String employeeId, // 사번
        String employeeName, // 이름
        String departmentCode, // 부서코드
        LocalDate hireDate // 입사일자
) {
}
