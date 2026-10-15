package com.telemedicine.common.testing.contract;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.model.SimpleResponse;
import com.atlassian.oai.validator.report.ValidationReport;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Base helpers for validating controller responses against the shared OpenAPI contracts shipped in
 * {@code telemedicine-backend/shared/contracts}.
 */
public abstract class OpenApiContractTest {

    protected OpenApiInteractionValidator validatorFor(String contractRelativePath)
            throws IOException {
        Path contract =
                Path.of(System.getProperty("telemedicine.contracts.dir", ""))
                        .resolve(contractRelativePath);
        assert contract.toFile().exists() : "Missing contract at " + contract;
        return OpenApiInteractionValidator.createForSpecificationUrl(contract.toUri().toString())
                .build();
    }

    protected ValidationReport validateResponse(
            OpenApiInteractionValidator validator,
            HttpMethod method,
            String path,
            MvcResult result)
            throws Exception {
        MockHttpServletResponse response = result.getResponse();
        SimpleResponse simpleResponse =
                new SimpleResponse.Builder(response.getStatus())
                        .withBody(response.getContentAsString())
                        .build();
        return validator.validateResponse(
                path, Request.Method.valueOf(method.name()), simpleResponse);
    }

    protected String readContract(String contractRelativePath) throws IOException {
        Path contract =
                Path.of(System.getProperty("telemedicine.contracts.dir", ""))
                        .resolve(contractRelativePath);
        return Files.readString(contract);
    }
}
