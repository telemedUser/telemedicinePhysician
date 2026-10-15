package com.telemedicine.common.web.openapi;

import static org.assertj.core.api.Assertions.assertThat;

import com.telemedicine.common.core.constants.Headers;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.parameters.HeaderParameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TelemedicineOpenApiCustomizerTest {

    private static final String JWKS =
            "http://keycloak:8080/realms/telemedicine/protocol/openid-connect/certs";

    private final TelemedicineOpenApiCustomizer customizer =
            new TelemedicineOpenApiCustomizer(JWKS);

    @Test
    @DisplayName("the shared problem schema is published once, with its violation type")
    void registersTheSharedErrorSchema() {
        OpenAPI openApi = new OpenAPI();

        customizer.customise(openApi);

        assertThat(openApi.getComponents().getSchemas()).containsKeys("ProblemDetail", "Violation");
        assertThat(openApi.getComponents().getSchemas().get("ProblemDetail").getProperties())
                .containsKeys(
                        "type", "title", "status", "detail", "instance", "code", "extensions");
        assertThat(openApi.getComponents().getSchemas().get("ProblemDetail").getRequired())
                .containsExactly("type", "title", "status");
    }

    @Test
    void registersTheKeycloakSecurityScheme() {
        OpenAPI openApi = new OpenAPI();

        customizer.customise(openApi);

        SecurityScheme scheme = openApi.getComponents().getSecuritySchemes().get("keycloak");
        assertThat(scheme.getType()).isEqualTo(SecurityScheme.Type.HTTP);
        assertThat(scheme.getScheme()).isEqualTo("bearer");
        assertThat(scheme.getBearerFormat()).isEqualTo("JWT");
        assertThat(scheme.getDescription()).contains(JWKS);
        assertThat(openApi.getSecurity()).hasSize(1);
    }

    @Test
    @DisplayName(
            "every operation gains the standard error responses without per-service boilerplate")
    void decoratesEveryOperation() {
        OpenAPI openApi = openApiWithOneOperation(new Operation());

        customizer.customise(openApi);

        ApiResponses responses = firstOperation(openApi).getResponses();
        assertThat(responses.keySet())
                .contains("400", "401", "403", "404", "409", "422", "429", "500");
        assertThat(responses.get("409").getContent().keySet())
                .containsExactly("application/problem+json");
        assertThat(
                        responses
                                .get("409")
                                .getContent()
                                .get("application/problem+json")
                                .getSchema()
                                .get$ref())
                .isEqualTo("#/components/schemas/ProblemDetail");
    }

    @Test
    @DisplayName("an operation that documents a status keeps its own richer description")
    void doesNotOverwriteExplicitResponses() {
        Operation operation =
                new Operation()
                        .responses(
                                new ApiResponses()
                                        .addApiResponse(
                                                "409",
                                                new ApiResponse()
                                                        .description(
                                                                "Appointment already checked in")));

        customizer.customise(openApiWithOneOperation(operation));

        assertThat(operation.getResponses().get("409").getDescription())
                .isEqualTo("Appointment already checked in");
    }

    @Test
    void documentsTheCorrelationHeaderExactlyOnce() {
        OpenAPI openApi = openApiWithOneOperation(new Operation());

        customizer.customise(openApi);
        customizer.customise(openApi);

        assertThat(firstOperation(openApi).getParameters())
                .filteredOn(parameter -> Headers.CORRELATION_ID.equals(parameter.getName()))
                .hasSize(1);
    }

    @Test
    void leavesAnAlreadyDeclaredCorrelationHeaderAlone() {
        Operation operation =
                new Operation()
                        .addParametersItem(
                                new HeaderParameter()
                                        .name(Headers.CORRELATION_ID)
                                        .description("service specific"));

        customizer.customise(openApiWithOneOperation(operation));

        assertThat(operation.getParameters())
                .singleElement()
                .satisfies(p -> assertThat(p.getDescription()).isEqualTo("service specific"));
    }

    @Test
    void isIdempotentAndToleratesADocumentWithNoPaths() {
        OpenAPI openApi = new OpenAPI();

        customizer.customise(openApi);
        customizer.customise(openApi);

        assertThat(openApi.getComponents().getSchemas()).hasSize(2);
        assertThat(openApi.getComponents().getSecuritySchemes()).hasSize(1);
    }

    private static OpenAPI openApiWithOneOperation(Operation operation) {
        return new OpenAPI()
                .paths(new Paths().addPathItem("/v1/appointments", new PathItem().get(operation)));
    }

    private static Operation firstOperation(OpenAPI openApi) {
        return openApi.getPaths().get("/v1/appointments").getGet();
    }
}
