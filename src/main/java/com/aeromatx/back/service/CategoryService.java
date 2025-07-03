package com.aeromatx.back.service;

import com.aeromatx.back.entity.Category;
import com.aeromatx.back.repository.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CategoryService {

    @Autowired
    private CategoryRepository categoryRepository;

    public Category createCategory(Category category) {
        // You might want to add validation here, e.g., check for unique name/slug
        return categoryRepository.save(category);
    }

    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    // ✨ NEW METHOD: Get only active categories ✨
    public List<Category> getActiveCategories() {
        // Assuming "Active" is the exact string used for active categories in your DB.
        // Make sure it matches the status value you set when creating categories.
        return categoryRepository.findByStatus("Active"); // Calls the method from CategoryRepository
    }

    public Optional<Category> getCategoryById(Long id) {
        return categoryRepository.findById(id);
    }

    public Category updateCategory(Long id, Category updatedCategory) {
        return categoryRepository.findById(id).map(category -> {
            category.setName(updatedCategory.getName());
            category.setSlug(updatedCategory.getSlug());
            category.setDescription(updatedCategory.getDescription());
            category.setStatus(updatedCategory.getStatus());
            return categoryRepository.save(category);
        }).orElseThrow(() -> new RuntimeException("Category not found with id " + id)); // Handle not found appropriately
    }

    public void deleteCategory(Long id) {
        categoryRepository.deleteById(id);
    }

    // Optional: Method to check if a category name already exists (for creation/update validation)
    public boolean existsByName(String name) {
        return categoryRepository.findByName(name).isPresent();
    }
}