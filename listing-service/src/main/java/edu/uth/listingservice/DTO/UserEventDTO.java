
package edu.uth.listingservice.DTO;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;


@JsonIgnoreProperties(ignoreUnknown = true)
public class UserEventDTO {

    /**
     * @JsonProperty("userId") Map trường "userId" (từ JSON của User-Service)
     * vào trường "id" của DTO này.
     */
    @JsonProperty("userId")
    private Integer id;

    private String name;
    private String email;
    private String phone;
    private String address;
    
    // Giữ lại eventType nếu User-Service có gửi, nếu không nó sẽ là null
    private String eventType; 

    public UserEventDTO() {}

    // getters / setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }
}