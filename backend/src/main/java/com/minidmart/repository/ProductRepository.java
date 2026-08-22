package com.minidmart.repository;

import com.minidmart.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    Optional<Product> findBySku(String sku);
    boolean existsBySku(String sku);
    List<Product> findByCategoryId(Long categoryId);
    List<Product> findByActiveTrue();
    List<Product> findByNameContainingIgnoreCase(String keyword);
    boolean existsByCategoryId(Long categoryId);
}
