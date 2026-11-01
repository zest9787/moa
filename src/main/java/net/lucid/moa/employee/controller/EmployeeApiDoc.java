package net.lucid.moa.employee.controller;

import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import net.lucid.moa.employee.dto.web.request.*;
import net.lucid.moa.employee.dto.web.response.*;
import net.lucid.moa.employee.mock.contract.MockApiResponse;
import net.lucid.moa.employee.mock.contract.MockPageResponse;

@Tag(name = "Employee", description = "사원 API — 현재 mock 프로파일 전용")
@ApiResponses({
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "404", description = "사원 없음")
})
public interface EmployeeApiDoc {
    @Operation(operationId = "searchEmployees", summary = "사원 목록 조회")
    @ApiResponse(responseCode = "200", description = "목록 조회 성공")
    @Parameters({
            @Parameter(name = "page", in = ParameterIn.QUERY,
                    schema = @Schema(type = "integer", defaultValue = "1", minimum = "1")),
            @Parameter(name = "size", in = ParameterIn.QUERY,
                    schema = @Schema(type = "integer", defaultValue = "20", minimum = "1", maximum = "100"))
    })
    MockApiResponse<MockPageResponse<EmployeeResponse>> searchEmployees(
            @Valid @ParameterObject EmployeeSearchRequest request);

    @Operation(operationId = "getEmployee", summary = "사원 상세 조회")
    @ApiResponse(responseCode = "200", description = "상세 조회 성공")
    MockApiResponse<EmployeeResponse> getEmployee(@Parameter(description = "사원번호", required = true) String employeeId);

    @Operation(operationId = "createEmployee", summary = "사원 등록")
    @ApiResponse(responseCode = "201", description = "등록 성공")
    MockApiResponse<EmployeeResponse> createEmployee(@Valid EmployeeCreateRequest request);

    @Operation(operationId = "updateEmployee", summary = "사원 정보 수정",
            description = "미전달 또는 null 필드는 기존 값을 유지합니다. 사전 조회한 ETag로 수정합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "수정 성공, data는 null"),
            @ApiResponse(responseCode = "412", description = "ETag 충돌")
    })
    MockApiResponse<Void> updateEmployee(@Parameter(description = "사원번호", required = true) String employeeId,
                                        @Valid EmployeeUpdateRequest request);

    @Operation(operationId = "deleteEmployee", summary = "사원 삭제")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "삭제 성공, data는 null"),
            @ApiResponse(responseCode = "412", description = "ETag 충돌")
    })
    MockApiResponse<Void> deleteEmployee(@Parameter(description = "사원번호", required = true) String employeeId);

    @Operation(operationId = "approveEmployee", summary = "사원 승인")
    @ApiResponse(responseCode = "200", description = "승인 성공")
    MockApiResponse<EmployeeApproveResponse> approveEmployee(
            @Parameter(description = "사원번호", required = true) String employeeId,
            @Valid EmployeeApproveRequest request);
}
