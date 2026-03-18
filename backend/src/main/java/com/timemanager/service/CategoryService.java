package com.timemanager.service;

import com.timemanager.entity.Category;
import java.util.List;

public interface CategoryService {
    List<Category> listByUser(Long userId);
    void create(Category category);
    void update(Category category);
    void delete(Long id);
}
