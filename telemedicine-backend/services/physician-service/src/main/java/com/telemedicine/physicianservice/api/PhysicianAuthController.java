package com.telemedicine.physicianservice.api;

import com.telemedicine.common.core.dto.ApiResponse;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Local-development physician sign-in. Validates demo credentials and returns the physician's id
 * and display name. The id matches the patient-facing doctor id (e.g. {@code doc-khanna}) so the
 * worklist — populated from patient bookings keyed by that same id — is scoped to the signed-in
 * doctor. The physician app then sends the id back on every request via the {@code X-Physician-Id}
 * header, which the dev security chain turns into the authenticated principal.
 *
 * <p>In a real environment this is replaced by the HIS SSO / Keycloak sign-in; these demo
 * credentials exist only to make per-physician scoping reviewable locally.
 */
@RestController
@RequestMapping("/api/auth")
public class PhysicianAuthController {

    private record Account(String physicianId, String displayName, String password) {}

    /** Demo accounts. Email -> account. Password is shared for the demo set. */
    private static final Map<String, Account> ACCOUNTS = new LinkedHashMap<>();

    static {
        ACCOUNTS.put("arjun.khanna@connectedcare.in", new Account("doc-khanna", "Dr. Arjun Khanna", "doctor123"));
        ACCOUNTS.put("meera.nair@connectedcare.in", new Account("doc-nair", "Dr. Meera Nair", "doctor123"));
        ACCOUNTS.put("vikram.reddy@connectedcare.in", new Account("doc-reddy", "Dr. Vikram Reddy", "doctor123"));
        ACCOUNTS.put("lakshmi.iyer@connectedcare.in", new Account("doc-iyer", "Dr. Lakshmi Iyer", "doctor123"));
    }

    @PostMapping("/physician-sign-in")
    public ApiResponse<Map<String, Object>> signIn(@RequestBody Map<String, String> body) {
        String email = body.getOrDefault("email", "").trim().toLowerCase();
        String password = body.getOrDefault("password", "");
        Account account = ACCOUNTS.get(email);
        if (account == null || !account.password().equals(password)) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Email or password doesn't match.");
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("physicianId", account.physicianId());
        out.put("displayName", account.displayName());
        out.put("email", email);
        return ApiResponse.ok(out);
    }
}
