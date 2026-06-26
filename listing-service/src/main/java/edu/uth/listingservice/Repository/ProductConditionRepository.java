package edu.uth.listingservice.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import edu.uth.listingservice.Model.ProductCondition;


@Repository
public interface ProductConditionRepository extends JpaRepository<ProductCondition, Long> {
}