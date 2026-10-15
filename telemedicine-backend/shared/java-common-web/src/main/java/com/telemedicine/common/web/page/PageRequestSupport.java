package com.telemedicine.common.web.page;

import com.telemedicine.common.core.dto.Violation;
import com.telemedicine.common.core.error.ValidationException;
import com.telemedicine.common.web.TelemedicineWebProperties;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Validates raw page parameters into a {@link PageQuery}.
 *
 * <p>The sort allow-list is a security control, not ergonomics. A sort field usually reaches the
 * database as an identifier that cannot be parameterised, so accepting an arbitrary string is a
 * classic injection route; and even when it is safe to interpolate, letting callers order by an
 * unindexed or unrelated column turns a cheap query into a table scan or leaks the existence of
 * columns the API never documents.
 *
 * <p>An over-large page is rejected rather than silently clamped, so a client asking for 5000 rows
 * finds out at development time instead of quietly receiving 100 and paginating incorrectly.
 */
public class PageRequestSupport {

    private final int defaultSize;
    private final int maxSize;

    public PageRequestSupport(TelemedicineWebProperties properties) {
        this(properties.getPagination().getDefaultSize(), properties.getPagination().getMaxSize());
    }

    public PageRequestSupport(int defaultSize, int maxSize) {
        if (defaultSize <= 0 || maxSize <= 0 || defaultSize > maxSize) {
            throw new IllegalArgumentException(
                    "Invalid pagination bounds: defaultSize="
                            + defaultSize
                            + ", maxSize="
                            + maxSize);
        }
        this.defaultSize = defaultSize;
        this.maxSize = maxSize;
    }

    public int defaultSize() {
        return defaultSize;
    }

    public int maxSize() {
        return maxSize;
    }

    /** Resolves page and size with no sorting. */
    public PageQuery resolve(Integer page, Integer size) {
        return resolve(page, size, (List<String>) null, Set.of());
    }

    /**
     * Resolves a page request whose sort terms arrive as one string, separated by semicolons —
     * {@code "startsAt,desc;createdAt,asc"}. Comma is already the field/direction separator, so it
     * cannot also separate terms.
     */
    public PageQuery resolve(
            Integer page, Integer size, String sort, Collection<String> allowedSortFields) {
        List<String> terms = sort == null || sort.isBlank() ? List.of() : List.of(sort.split(";"));
        return resolve(page, size, terms, allowedSortFields);
    }

    /**
     * Resolves and validates a page request.
     *
     * @param sort terms in the repeated-parameter form Spring Data uses, each {@code field} or
     *     {@code field,asc|desc}
     * @param allowedSortFields the only field names this endpoint permits
     * @throws ValidationException if any parameter is out of bounds or not allow-listed
     */
    public PageQuery resolve(
            Integer page, Integer size, List<String> sort, Collection<String> allowedSortFields) {
        List<Violation> violations = new ArrayList<>();

        int resolvedPage = page == null ? 0 : page;
        if (resolvedPage < 0) {
            violations.add(Violation.of("page", "must be zero or greater"));
            resolvedPage = 0;
        }

        int resolvedSize = size == null ? defaultSize : size;
        if (resolvedSize < 1) {
            violations.add(Violation.of("size", "must be at least 1"));
            resolvedSize = defaultSize;
        } else if (resolvedSize > maxSize) {
            violations.add(Violation.of("size", "must not exceed " + maxSize));
            resolvedSize = maxSize;
        }

        List<SortSpec> sortSpecs = parseSort(sort, allowedSortFields, violations);

        if (!violations.isEmpty()) {
            throw new ValidationException("Invalid page request", violations);
        }
        return new PageQuery(resolvedPage, resolvedSize, sortSpecs);
    }

    private List<SortSpec> parseSort(
            List<String> sort, Collection<String> allowedSortFields, List<Violation> violations) {
        if (sort == null || sort.isEmpty()) {
            return List.of();
        }
        Set<String> allowed = new LinkedHashSet<>(allowedSortFields);
        List<SortSpec> specs = new ArrayList<>();

        for (String term : sort) {
            String trimmed = term == null ? "" : term.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            String[] parts = trimmed.split(",");
            String field = parts[0].trim();
            String direction = parts.length > 1 ? parts[1].trim() : "asc";

            if (!allowed.contains(field)) {
                violations.add(
                        new Violation(
                                "sort",
                                "'"
                                        + field
                                        + "' is not sortable; allowed: "
                                        + String.join(", ", allowed),
                                null));
                continue;
            }
            if (!"asc".equalsIgnoreCase(direction) && !"desc".equalsIgnoreCase(direction)) {
                violations.add(Violation.of("sort", "direction must be asc or desc"));
                continue;
            }
            specs.add(new SortSpec(field, SortSpec.isDescending(direction)));
        }
        return specs;
    }
}
