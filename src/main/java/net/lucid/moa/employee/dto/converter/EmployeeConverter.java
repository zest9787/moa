package net.lucid.moa.employee.dto.converter;

import net.lucid.moa.employee.dto.odata.response.ODataEmployeeResponse;

import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import net.lucid.moa.employee.dto.web.request.*;
import net.lucid.moa.employee.dto.web.response.*;
import net.lucid.moa.employee.dto.sap.request.*;
import net.lucid.moa.employee.dto.sap.response.*;

@Mapper(componentModel = "spring", unmappedSourcePolicy = ReportingPolicy.IGNORE,
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface EmployeeConverter {
    SapEmployeeCreateRequest toSapCreateRequest(EmployeeCreateRequest source);
    SapEmployeeUpdateRequest toSapUpdateRequest(EmployeeUpdateRequest source);
    SapEmployeeApproveRequest toSapApproveRequest(EmployeeApproveRequest source);
    EmployeeResponse toWebResponse(ODataEmployeeResponse source);
    List<EmployeeResponse> toWebResponses(List<ODataEmployeeResponse> source);
    EmployeeApproveResponse toWebApproveResponse(SapEmployeeApproveResponse source);
}
