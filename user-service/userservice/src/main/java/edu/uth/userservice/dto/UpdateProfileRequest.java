package edu.uth.userservice.dto;

public class UpdateProfileRequest {
    private String name;
    private String email;
    private String phone;
    private String address;
    private String cityName;

    // getters / setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getCityName() { return cityName; }
    public void setCityName(String cityName) { this.cityName = cityName; }
}
