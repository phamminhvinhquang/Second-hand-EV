package edu.uth.userservice.dto;

import java.util.List;

public class RolesUpdateRequest {
    private List<String> roles;

    public RolesUpdateRequest() {}
    public RolesUpdateRequest(List<String> roles) { this.roles = roles; }

    public List<String> getRoles() { return roles; }
    public void setRoles(List<String> roles) { this.roles = roles; }
}
