package edu.uth.listingservice.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import edu.uth.listingservice.Model.Product;


@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
}