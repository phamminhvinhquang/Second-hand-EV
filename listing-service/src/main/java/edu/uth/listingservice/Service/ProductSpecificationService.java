package edu.uth.listingservice.Service;

import edu.uth.listingservice.Model.ProductSpecification;

import java.util.List;

public interface ProductSpecificationService {
    List<ProductSpecification> getAll();
    ProductSpecification getById(Long id);
    ProductSpecification getByProductId(Long productId);
    ProductSpecification create(ProductSpecification specification);
    ProductSpecification update(Long id, ProductSpecification specification);
    void delete(Long id);
}
