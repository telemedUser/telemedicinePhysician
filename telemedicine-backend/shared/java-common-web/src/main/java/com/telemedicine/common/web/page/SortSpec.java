package com.telemedicine.common.web.page;

import java.util.Locale;

/**
 * One validated sort term.
 *
 * @param field property name, already checked against the endpoint's allow-list
 * @param descending sort direction
 */
public record SortSpec(String field, boolean descending) {

    public static SortSpec asc(String field) {
        return new SortSpec(field, false);
    }

    public static SortSpec desc(String field) {
        return new SortSpec(field, true);
    }

    /** Renders as {@code field,asc} — the same form accepted on the query string. */
    public String toParameter() {
        return field + "," + (descending ? "desc" : "asc");
    }

    public String direction() {
        return descending ? "DESC" : "ASC";
    }

    static boolean isDescending(String token) {
        return "desc".equals(token.toLowerCase(Locale.ROOT));
    }
}
