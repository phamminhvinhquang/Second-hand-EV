package edu.uth.listingservice.Service;

import java.util.List;

import edu.uth.listingservice.Model.Product;


public interface ProductService {
List<Product> getAllProducts();
Product getProductById(Long id);
Product createProduct(Product product);
Product updateProduct(Long id, Product product);
void deleteProduct(Long id);
}