package edu.uth.listingservice.Service;



import java.util.List;

import edu.uth.listingservice.Model.ProductCondition;

public interface ProductConditionService {
    List<ProductCondition> getAll();
    ProductCondition getById(Long id);
    ProductCondition create(ProductCondition condition);
    ProductCondition update(Long id, ProductCondition condition);
    void delete(Long id);
}
