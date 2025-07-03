package com.aeromatx.back.controller;

import com.aeromatx.back.entity.Category; // Correct package based on your files
import com.aeromatx.back.service.CategoryService; // Correct package based on your files
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@CrossOrigin(origins = "http://localhost:8080,http://127.0.0.1:5500") // IMPORTANT: Be specific in production!
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    // Endpoint for adding a new category (typically admin-only)
    @PostMapping("/admin")
    public ResponseEntity<Category> createCategory(@RequestBody Category category) {
        Category createdCategory = categoryService.createCategory(category);
        return new ResponseEntity<>(createdCategory, HttpStatus.CREATED);
    }

    // Endpoint to get ALL categories (might include inactive ones, for admin views)
    @GetMapping("/admin/all")
    public ResponseEntity<List<Category>> getAllCategoriesForAdmin() {
        List<Category> categories = categoryService.getAllCategories();
        return new ResponseEntity<>(categories, HttpStatus.OK);
    }

    // ✨ NEW ENDPOINT: Get only active categories for frontend display ✨
    @GetMapping // This will be accessed by your frontend header
    public ResponseEntity<List<Category>> getActiveCategoriesForFrontend() {
        List<Category> activeCategories = categoryService.getActiveCategories();
        return new ResponseEntity<>(activeCategories, HttpStatus.OK);
    }

    @GetMapping("/admin/{id}") // Assuming admin needs to fetch by ID
    public ResponseEntity<Category> getCategoryById(@PathVariable Long id) {
        return categoryService.getCategoryById(id)
                .map(category -> new ResponseEntity<>(category, HttpStatus.OK))
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @PutMapping("/admin/{id}") // For updating categories (admin-only)
    public ResponseEntity<Category> updateCategory(@PathVariable Long id, @RequestBody Category category) {
        Category updatedCategory = categoryService.updateCategory(id, category);
        return new ResponseEntity<>(updatedCategory, HttpStatus.OK);
    }

    @DeleteMapping("/admin/{id}") // For deleting categories (admin-only)
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}