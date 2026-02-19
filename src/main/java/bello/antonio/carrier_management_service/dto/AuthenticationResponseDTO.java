package bello.antonio.carrier_management_service.dto;

import bello.antonio.carrier_management_service.domain.Role;

public class AuthenticationResponseDTO {
    private String jwt;
    private Role role;

    public String getJwt() {
        return jwt;
    }

    public void setJwt(String jwt) {
        this.jwt = jwt;
    }

    public AuthenticationResponseDTO(String jwt) {
        this.jwt = jwt;
    }

    public AuthenticationResponseDTO(String jwt, Role role) {
        this.jwt = jwt;
        this.role = role;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }
}
