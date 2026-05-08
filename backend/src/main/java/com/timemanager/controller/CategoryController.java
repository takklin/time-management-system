package com.timemanager.controller;

import com.timemanager.common.result.Result;
import com.timemanager.entity.Category;
import com.timemanager.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
public class CategoryController {

    private static final Logger logger = LoggerFactory.getLogger(CategoryController.class);

    @Autowired
    private CategoryService categoryService;

    private Long currentUserId() {
        return com.timemanager.util.UserUtil.getCurrentUserId();
    }

    @GetMapping
    public Result<List<Category>> list() {
        Long uid = currentUserId();
        logger.debug("GET /categories currentUserId={}", uid);
        if (uid == null) return Result.error(401, "Unauthorized");
        return Result.success(categoryService.listByUser(uid));
    }

    @PostMapping
    public Result<?> create(@RequestBody Category category) {
        Long uid = currentUserId();
        logger.debug("POST /categories body={}, currentUserId={}", category, uid);
        if (uid == null) return Result.error(401, "Unauthorized");
        try {
            category.setUserId(uid);
            if (category.getCreatedAt() == null) category.setCreatedAt(LocalDateTime.now());
            category.setUpdatedAt(LocalDateTime.now());
            if (category.getDeleted() == null) category.setDeleted(0);
            categoryService.create(category);
            return Result.success();
        } catch (Exception ex) {
            logger.error("Failed to create category for user {}: {}", uid, ex.getMessage(), ex);
            return Result.error(500, "创建分类失败");
        }
    }

    @PutMapping("/{id}")
    public Result<?> update(@PathVariable Long id, @RequestBody Category category) {
        category.setId(id);
        categoryService.update(category);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        Long uid = currentUserId();
        logger.debug("DELETE /categories/{} requested by user={}", id, uid);
        if (uid == null) return Result.error(401, "Unauthorized");
        try {
            int rows = categoryService.delete(id, uid);
            if (rows <= 0) {
                logger.warn("Delete category {} affected 0 rows for user {}", id, uid);
                return Result.error(404, "分类未找到或无权限删除");
            }
            logger.info("Category {} marked deleted by user {} (rows={})", id, uid, rows);
            return Result.success();
        } catch (Exception ex) {
            logger.error("Failed to delete category {} for user {}: {}", id, uid, ex.getMessage(), ex);
            return Result.error(500, "删除分类失败");
        }
    }
}
