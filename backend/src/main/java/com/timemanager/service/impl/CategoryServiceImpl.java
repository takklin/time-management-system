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
    public int delete(Long id, Long userId) {
        Category c = new Category();
        c.setDeleted(1);
        c.setUpdatedAt(java.time.LocalDateTime.now());
        // update with wrapper to ensure only the user's category is affected
        com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<Category> uw = new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<>();
        uw.eq("id", id).eq("user_id", userId);
        return categoryMapper.update(c, uw);
    }
}
