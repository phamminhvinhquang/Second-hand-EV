package edu.uth.listingservice.Controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.uth.listingservice.Model.ProductSpecification;
import edu.uth.listingservice.Service.ProductSpecificationService;

@RestController
@RequestMapping("/api/specifications")

public class ProductSpecificationController {

    @Autowired
    private ProductSpecificationService specificationService;

    @GetMapping
    public List<ProductSpecification> getAll() {
        return specificationService.getAll();
    }

    @GetMapping("/{id}")
    public ProductSpecification getById(@PathVariable Long id) {
        return specificationService.getById(id);
    }

    @GetMapping("/product/{productId}")
    public ProductSpecification getByProductId(@PathVariable Long productId) {
        return specificationService.getByProductId(productId);
    }

    @PostMapping
    public ProductSpecification create(@RequestBody ProductSpecification specification) {
        return specificationService.create(specification);
    }

    @PutMapping("/{id}")
    public ProductSpecification update(@PathVariable Long id, @RequestBody ProductSpecification specification) {
        return specificationService.update(id, specification);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        specificationService.delete(id);
    }
}
