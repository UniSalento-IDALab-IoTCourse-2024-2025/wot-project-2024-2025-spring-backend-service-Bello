package bello.antonio.carrier_management_service.dto;

import bello.antonio.carrier_management_service.domain.Role;

public class AuthenticationResponseDTO {
    private String jwt;
    private Role role;
    private String userId;

    public AuthenticationResponseDTO(String jwt, Role role, String userId) {
        this.jwt = jwt;
        this.role = role;
        this.userId = userId;
    }

    public String getJwt() {
        return jwt;
    }

    public void setJwt(String jwt) {
        this.jwt = jwt;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}