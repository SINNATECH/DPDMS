package zw.gov.dpdms.auth.dto;

import lombok.Builder;
import lombok.Getter;
import zw.gov.dpdms.auth.model.Role;

import java.time.Instant;

@Getter
@Builder
public class LoginResponse {

    private String token;
    private String tokenType; // always "Bearer" — included so clients don't hardcode it
    private Instant expiresAt;
    private String username;
    private Role role;
    private String hazard; // null for NATIONAL/ADMIN
    private String ward;   // null for anything except RECORDER
}
