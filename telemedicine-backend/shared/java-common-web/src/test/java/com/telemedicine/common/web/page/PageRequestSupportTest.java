package com.telemedicine.common.web.page;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.telemedicine.common.core.error.ValidationException;
import com.telemedicine.common.web.PageResponse;
import com.telemedicine.common.web.TelemedicineWebProperties;
import java.util.List;
import java.util.Set;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PageRequestSupportTest {

    private static final Set<String> ALLOWED = Set.of("startsAt", "createdAt");

    private final PageRequestSupport support = new PageRequestSupport(20, 100);

    @Test
    void appliesDefaults() {
        PageQuery query = support.resolve(null, null);

        assertThat(query.page()).isZero();
        assertThat(query.size()).isEqualTo(20);
        assertThat(query.sort()).isEmpty();
        assertThat(query.isSorted()).isFalse();
        assertThat(query.offset()).isZero();
    }

    @Test
    void readsBoundsFromProperties() {
        TelemedicineWebProperties properties = new TelemedicineWebProperties();
        properties.getPagination().setDefaultSize(15);
        properties.getPagination().setMaxSize(50);

        PageRequestSupport configured = new PageRequestSupport(properties);

        assertThat(configured.defaultSize()).isEqualTo(15);
        assertThat(configured.maxSize()).isEqualTo(50);
    }

    @Test
    void rejectsIncoherentBounds() {
        assertThatThrownBy(() -> new PageRequestSupport(50, 10))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PageRequestSupport(0, 10))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PageRequestSupport(10, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void computesTheOffset() {
        assertThat(support.resolve(3, 25).offset()).isEqualTo(75L);
    }

    @Test
    @DisplayName("an over-large page is rejected, not silently clamped")
    void rejectsOversizedPages() {
        assertThatThrownBy(() -> support.resolve(0, 5000))
                .isInstanceOf(ValidationException.class)
                .asInstanceOf(InstanceOfAssertFactories.type(ValidationException.class))
                .satisfies(
                        exception ->
                                assertThat(exception.getViolations())
                                        .singleElement()
                                        .satisfies(
                                                violation -> {
                                                    assertThat(violation.field()).isEqualTo("size");
                                                    assertThat(violation.message())
                                                            .isEqualTo("must not exceed 100");
                                                }));
    }

    @Test
    void rejectsNegativePagesAndZeroSizes() {
        assertThatThrownBy(() -> support.resolve(-1, 10)).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> support.resolve(0, 0)).isInstanceOf(ValidationException.class);
    }

    @Test
    void reportsEveryProblemAtOnce() {
        assertThatThrownBy(() -> support.resolve(-1, 5000, List.of("secretColumn"), ALLOWED))
                .asInstanceOf(InstanceOfAssertFactories.type(ValidationException.class))
                .satisfies(exception -> assertThat(exception.getViolations()).hasSize(3));
    }

    @Test
    void parsesAllowedSortTerms() {
        PageQuery query = support.resolve(0, 10, List.of("startsAt,desc", "createdAt"), ALLOWED);

        assertThat(query.sort())
                .containsExactly(SortSpec.desc("startsAt"), SortSpec.asc("createdAt"));
        assertThat(query.isSorted()).isTrue();
    }

    @Test
    void parsesSemicolonSeparatedSortStrings() {
        PageQuery query = support.resolve(0, 10, "startsAt,desc;createdAt,asc", ALLOWED);

        assertThat(query.sort())
                .containsExactly(SortSpec.desc("startsAt"), SortSpec.asc("createdAt"));
    }

    @Test
    void ignoresBlankSortInput() {
        assertThat(support.resolve(0, 10, "  ", ALLOWED).sort()).isEmpty();
        assertThat(support.resolve(0, 10, (String) null, ALLOWED).sort()).isEmpty();
        assertThat(support.resolve(0, 10, List.of("", "  "), ALLOWED).sort()).isEmpty();
        assertThat(support.resolve(0, 10, (List<String>) null, ALLOWED).sort()).isEmpty();
    }

    @Test
    @DisplayName("a field outside the allow-list is refused rather than passed to the database")
    void refusesUnknownSortFields() {
        assertThatThrownBy(() -> support.resolve(0, 10, List.of("password"), ALLOWED))
                .asInstanceOf(InstanceOfAssertFactories.type(ValidationException.class))
                .satisfies(
                        exception ->
                                assertThat(exception.getViolations())
                                        .singleElement()
                                        .satisfies(
                                                violation -> {
                                                    assertThat(violation.field()).isEqualTo("sort");
                                                    assertThat(violation.message())
                                                            .contains("'password' is not sortable");
                                                }));
    }

    @Test
    void refusesUnknownSortDirections() {
        assertThatThrownBy(() -> support.resolve(0, 10, List.of("startsAt,sideways"), ALLOWED))
                .asInstanceOf(InstanceOfAssertFactories.type(ValidationException.class))
                .satisfies(
                        exception ->
                                assertThat(exception.getViolations())
                                        .singleElement()
                                        .satisfies(
                                                violation ->
                                                        assertThat(violation.message())
                                                                .isEqualTo(
                                                                        "direction must be asc or"
                                                                                + " desc")));
    }

    @Test
    void sortSpecRendersBackToItsParameterForm() {
        assertThat(SortSpec.desc("startsAt").toParameter()).isEqualTo("startsAt,desc");
        assertThat(SortSpec.asc("startsAt").toParameter()).isEqualTo("startsAt,asc");
        assertThat(SortSpec.desc("startsAt").direction()).isEqualTo("DESC");
        assertThat(SortSpec.asc("startsAt").direction()).isEqualTo("ASC");
    }

    @Test
    void pageResponseDerivesItsNavigationState() {
        PageResponse<String> page = PageResponse.of(List.of("a", "b"), 0, 2, 5);

        assertThat(page.totalPages()).isEqualTo(3);
        assertThat(page.hasNext()).isTrue();
        assertThat(PageResponse.of(List.of("e"), 2, 2, 5).hasNext()).isFalse();
        assertThat(PageResponse.empty(1, 20).items()).isEmpty();
        assertThat(PageResponse.empty(1, 20).totalPages()).isZero();
        assertThat(new PageResponse<>(List.of(), 0, 0, 0).totalPages()).isZero();
    }

    @Test
    void pageQueryDefensivelyCopiesItsSort() {
        assertThat(new PageQuery(0, 10, null).sort()).isEmpty();
    }
}
