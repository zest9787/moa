package net.lucid.moa.employee.mock.contract;

/** Temporary mock-only contract. TODO replace with the actual CommonApiResponse. */
public record MockApiResponse<T>(boolean success, T data, String error) {
    public static <T> MockApiResponse<T> ok(T data) {
        return new MockApiResponse<>(true, data, null);
    }

    public static MockApiResponse<Void> failure(String error) {
        return new MockApiResponse<>(false, null, error);
    }
}
