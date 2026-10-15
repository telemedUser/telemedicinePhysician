package com.telemedicine.common.web.page;

import java.util.List;

/**
 * A validated page request: bounded size, non-negative index and allow-listed sort terms.
 *
 * <p>Produced only by {@link PageRequestSupport}, so a repository receiving one can treat the sort
 * field names as trusted.
 *
 * @param page zero-based page index
 * @param size number of rows, already clamped to the configured maximum
 * @param sort ordered sort terms, possibly empty
 */
public record PageQuery(int page, int size, List<SortSpec> sort) {

    public PageQuery {
        sort = sort == null ? List.of() : List.copyOf(sort);
    }

    public long offset() {
        return (long) page * size;
    }

    public boolean isSorted() {
        return !sort.isEmpty();
    }
}
