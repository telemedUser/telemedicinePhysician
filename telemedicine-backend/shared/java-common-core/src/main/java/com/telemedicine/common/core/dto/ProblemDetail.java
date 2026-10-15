package com.telemedicine.common.core.dto;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * RFC 9457 problem document.
 *
 * <p>The seven record components are frozen. Everything the RFC calls an extension member lives in
 * {@code extensions} and is <em>flattened to the top level on the wire</em> by {@link
 * JsonAnyGetter}, because the RFC places extension members alongside {@code type} and {@code
 * status} rather than nested under a container. {@link #fromJson(Map)} performs the inverse split
 * so a document still deserialises back into this shape.
 *
 * <p>Build instances with {@link ProblemDetails} rather than the constructor: it seeds the type
 * URI, title, status and category from the error catalogue and redacts PHI from {@code detail}.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProblemDetail(
        URI type,
        String title,
        int status,
        String detail,
        String instance,
        String code,
        Map<String, Object> extensions) {

    /** Known members, excluded from the extension map when parsing an inbound document. */
    private static final String[] CORE_MEMBERS = {
        "type", "title", "status", "detail", "instance", "code"
    };

    public ProblemDetail {
        extensions = extensions == null ? Map.of() : Map.copyOf(extensions);
    }

    @JsonAnyGetter
    @Override
    public Map<String, Object> extensions() {
        return extensions;
    }

    public static ProblemDetail of(int status, String title, String detail, String code) {
        return new ProblemDetail(
                URI.create("about:blank"), title, status, detail, null, code, Map.of());
    }

    /**
     * Reassembles a document whose extension members are top-level, which is how every Telemedicine
     * service emits them.
     */
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static ProblemDetail fromJson(Map<String, Object> raw) {
        Map<String, Object> members = new LinkedHashMap<>(raw);
        Object type = members.remove("type");
        Object title = members.remove("title");
        Object status = members.remove("status");
        Object detail = members.remove("detail");
        Object instance = members.remove("instance");
        Object code = members.remove("code");
        for (String member : CORE_MEMBERS) {
            members.remove(member);
        }
        return new ProblemDetail(
                type == null ? null : URI.create(type.toString()),
                asString(title),
                status instanceof Number number ? number.intValue() : 0,
                asString(detail),
                asString(instance),
                asString(code),
                members);
    }

    private static String asString(Object value) {
        return value == null ? null : value.toString();
    }
}
