package com.telemedicine.common.web;

import java.util.List;

/**
 * Paged collection envelope.
 *
 * <p>Frozen shape, for the same reason as {@code ApiResponse}: the four members are mirrored in the
 * OpenAPI contracts, the Python DTOs and the front-end types.
 *
 * @param items the page contents
 * @param page zero-based page index
 * @param size requested page size
 * @param total total matching rows across all pages
 */
public record PageResponse<T>(List<T> items, int page, int size, long total) {

    public static <T> PageResponse<T> of(List<T> items, int page, int size, long total) {
        return new PageResponse<>(items, page, size, total);
    }

    /** Convenience for a query that matched nothing, preserving the requested page coordinates. */
    public static <T> PageResponse<T> empty(int page, int size) {
        return new PageResponse<>(List.of(), page, size, 0L);
    }

    /** Total number of pages, derived rather than stored so it cannot drift from {@code total}. */
    public int totalPages() {
        return size <= 0 ? 0 : (int) Math.ceil((double) total / size);
    }

    public boolean hasNext() {
        return (long) (page + 1) * size < total;
    }
}
