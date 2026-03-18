package com.timemanager.controller;

import com.timemanager.common.result.Result;
import com.timemanager.entity.Category;
import com.timemanager.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    private Long currentUserId() {
        return com.timemanager.util.UserUtil.getCurrentUserId();
    }

    @GetMapping
    public Result<List<Category>> list() {
        return Result.success(categoryService.listByUser(currentUserId()));
    }

    @PostMapping
    public Result<?> create(@RequestBody Category category) {
        category.setUserId(currentUserId());
        categoryService.create(category);
        return Result.success();
    }

    @PutMapping("/{id}")
    public Result<?> update(@PathVariable Long id, @RequestBody Category category) {
        category.setId(id);
        categoryService.update(category);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return Result.success();
    }
}
