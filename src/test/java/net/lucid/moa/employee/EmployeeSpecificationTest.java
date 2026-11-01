package net.lucid.moa.employee;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;
import static org.assertj.core.api.Assertions.assertThat;

class EmployeeSpecificationTest {
    @Test
    void allSixSpecificationsAreValidYamlWithDistinctEndpoints() throws Exception {
        var endpoints = new HashSet<String>();
        try (var paths = Files.list(Path.of("api-specs/employee"))) {
            var specifications = paths.filter(p -> p.toString().endsWith(".yaml")).toList();
            assertThat(specifications).hasSize(6);
            for (var path : specifications) {
                try (var reader = Files.newBufferedReader(path)) {
                    Map<String, Object> specification = new Yaml().load(reader);
                    assertThat(specification).containsKeys("specVersion", "feature", "api", "sap", "webRequest");
                    var api = (Map<?, ?>) specification.get("api");
                    assertThat(endpoints.add(api.get("method") + " " + api.get("path"))).as(path.toString()).isTrue();
                    var sap = (Map<?, ?>) specification.get("sap");
                    assertThat(sap.get("odataVersion")).isEqualTo("V4");
                    assertThat(sap.get("entitySet")).isEqualTo("Employees");
                }
            }
        }
    }
}
