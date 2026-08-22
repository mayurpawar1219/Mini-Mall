package com.minidmart.service;

import com.minidmart.dto.CategoryRequest;
import com.minidmart.dto.CategoryResponse;
import com.minidmart.entity.Category;
import com.minidmart.exception.DuplicateResourceException;
import com.minidmart.exception.ResourceNotFoundException;
import com.minidmart.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final com.minidmart.repository.ProductRepository productRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategories() {
        return categoryRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CategoryResponse getCategoryById(Long id) {
        return categoryRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + id));
    }

    @Transactional
    public CategoryResponse createCategory(CategoryRequest request, String adminId) {
        if (categoryRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("Category", "name", request.getName());
        }

        Category category = Category.builder()
                .name(request.getName())
                .description(request.getDescription())
                .build();

        category = categoryRepository.save(category);

        if (adminId != null) {
            auditService.log(null, "CATEGORY_CREATED", "Category", category.getId().toString(), "Created by: " + adminId);
        }

        return toResponse(category);
    }

    @Transactional
    public CategoryResponse updateCategory(Long id, CategoryRequest request, String adminId) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + id));

        if (!category.getName().equals(request.getName()) && categoryRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("Category", "name", request.getName());
        }

        category.setName(request.getName());
        category.setDescription(request.getDescription());

        category = categoryRepository.save(category);

        if (adminId != null) {
            auditService.log(null, "CATEGORY_UPDATED", "Category", category.getId().toString(), "Updated by: " + adminId);
        }

        return toResponse(category);
    }

    @Transactional
    public void deleteCategory(Long id, String adminId) {
        if (!categoryRepository.existsById(id)) {
            throw new ResourceNotFoundException("Category not found with ID: " + id);
        }
        
        if (productRepository.existsByCategoryId(id)) {
            throw new com.minidmart.exception.InvalidOperationException("Cannot delete category because it has associated products.");
        }
        
        categoryRepository.deleteById(id);

        if (adminId != null) {
            auditService.log(null, "CATEGORY_DELETED", "Category", id.toString(), "Deleted by: " + adminId);
        }
    }

    private CategoryResponse toResponse(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .build();
    }
}
