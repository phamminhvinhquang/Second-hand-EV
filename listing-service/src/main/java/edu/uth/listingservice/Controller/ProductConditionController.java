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

import  edu.uth.listingservice.Model.ProductCondition;
import edu.uth.listingservice.Service.ProductConditionService;

@RestController
@RequestMapping("/api/conditions")

public class ProductConditionController {

    @Autowired
    private ProductConditionService conditionService;

    @GetMapping
    public List<ProductCondition> getAll() {
        return conditionService.getAll();
    }

    @GetMapping("/{id}")
    public ProductCondition getById(@PathVariable Long id) {
        return conditionService.getById(id);
    }

    @PostMapping
    public ProductCondition create(@RequestBody ProductCondition condition) {
        return conditionService.create(condition);
    }

    @PutMapping("/{id}")
    public ProductCondition update(@PathVariable Long id, @RequestBody ProductCondition condition) {
        return conditionService.update(id, condition);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        conditionService.delete(id);
    }
}
