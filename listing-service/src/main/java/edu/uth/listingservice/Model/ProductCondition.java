package edu.uth.listingservice.Model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "Product_Conditions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductCondition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "condition_id")
    private Long conditionId;

    @Column(name = "condition_name", nullable = false)
    private String conditionName;
}

