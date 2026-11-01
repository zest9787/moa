package net.lucid.moa.employee.dto.sap.response;

import net.lucid.moa.employee.dto.odata.response.ODataEmployeeResponse;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAlias;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public record SapEmployeeCollectionResponse(
        @JsonProperty("data") @JsonAlias("value") List<ODataEmployeeResponse> value,
        @JsonProperty("@odata.count") Long count,
        @JsonProperty("@odata.nextLink") String nextLink) {
}
