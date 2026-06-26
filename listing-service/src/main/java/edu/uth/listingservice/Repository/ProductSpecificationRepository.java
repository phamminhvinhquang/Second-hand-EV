package edu.uth.listingservice.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import edu.uth.listingservice.Model.ProductSpecification;


@Repository
public interface ProductSpecificationRepository extends JpaRepository<ProductSpecification, Long> {
    ProductSpecification findByProduct_ProductId(Long productId);
}