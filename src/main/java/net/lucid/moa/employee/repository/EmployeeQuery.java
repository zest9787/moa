package net.lucid.moa.employee.repository;

/** Repository search criteria; pagination is one-based. */
public record EmployeeQuery(String employeeName, String departmentCode, int page, int size) {
    public EmployeeQuery {
        if (page < 1 || size < 1 || size > 100) {
            throw new IllegalArgumentException("page >= 1 and 1 <= size <= 100 required");
        }
    }

    public long offset() {
        return ((long) page - 1) * size;
    }
}
