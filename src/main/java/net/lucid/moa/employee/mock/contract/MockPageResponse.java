package net.lucid.moa.employee.mock.contract;

import java.util.List;

/** TODO replace with the real common paging contract before SAP integration. */
public record MockPageResponse<T>(List<T> content, long totalElements, int page, int size) {
    public MockPageResponse {
        content = List.copyOf(content);
    }
}
