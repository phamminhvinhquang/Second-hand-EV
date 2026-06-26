package edu.uth.userservice.dto;

public class LoginRequest {
    private String identifier; // optional
    private String email;      // optional - để client gửi email field trực tiếp
    private String password;

    // getters / setters
    public String getIdentifier() { return identifier; }
    public void setIdentifier(String identifier) { this.identifier = identifier; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
