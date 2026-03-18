package com.timemanager.service.impl;

import com.timemanager.entity.Category;
import com.timemanager.mapper.CategoryMapper;
import com.timemanager.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryServiceImpl implements CategoryService {

    @Autowired
    private CategoryMapper categoryMapper;

    @Override
    public List<Category> listByUser(Long userId) {
        return categoryMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Category>()
                        .eq("user_id", userId)
                        .eq("deleted", 0));
    }

    @Override
    public void create(Category category) {
        categoryMapper.insert(category);
    }

    @Override
    public void update(Category category) {
        categoryMapper.updateById(category);
    }

    @Override
    public void delete(Long id) {
        Category c = new Category();
        c.setId(id);
        c.setDeleted(1);
        categoryMapper.updateById(c);
    }
}
