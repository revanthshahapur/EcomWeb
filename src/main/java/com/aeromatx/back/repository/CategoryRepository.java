package com.aeromatx.back.repository;

import com.aeromatx.back.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    Optional<Category> findByName(String name);

    // ✅ Add this line to fetch only active categories
    List<Category> findByStatus(String status);
}
