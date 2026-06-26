package edu.uth.userservice.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "role")
public class Role {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, length = 50)
    private String name;

    private String description;

    @Column(name = "created_at")
    private Instant createdAt = Instant.now();

    public Role() {}

    public Role(String name) { this.name = name; }

    // ✅ THÊM HÀM KHỞI TẠO MỚI NÀY:
    public Role(String name, String description) {
        this.name = name;
        this.description = description;
    }

    // getters / setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
