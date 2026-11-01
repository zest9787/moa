package net.lucid.moa.employee.mock.contract;

import net.lucid.moa.employee.dto.odata.response.ODataEmployeeResponse;

/** TODO use the real RapClient ETag response contract when available. */
public record MockVersionedEmployee(ODataEmployeeResponse employee, String etag) {
}
