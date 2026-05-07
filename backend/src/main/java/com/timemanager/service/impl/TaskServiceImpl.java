package com.timemanager.service.impl;

import com.timemanager.entity.Task;
import com.timemanager.mapper.TaskMapper;
import com.timemanager.util.UserUtil;
import com.timemanager.service.TaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.time.LocalDateTime;

@Service
public class TaskServiceImpl implements TaskService {

    private static final Logger logger = LoggerFactory.getLogger(TaskServiceImpl.class);

    @Autowired
    private TaskMapper taskMapper;

    @Override
    public List<Task> list(Long userId) {
        return taskMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Task>()
                        .eq("user_id", userId)
                        .eq("deleted", 0));
    }

    @Override
    public List<Task> listWithCategory(Long userId, Long categoryId) {
        if (categoryId == null) {
            return taskMapper.selectWithCategoryName(userId);
        } else {
            return taskMapper.selectWithCategoryNameByCategory(userId, categoryId);
        }
    }

    @Override
    public Task getById(Long id) {
        return taskMapper.selectById(id);
    }

    @Override
    public void create(Task task) {
        if (task.getCreatedAt() == null) task.setCreatedAt(LocalDateTime.now());
        if (task.getUpdatedAt() == null) task.setUpdatedAt(LocalDateTime.now());
        taskMapper.insert(task);
    }

    @Override
    public Task update(Task task) {
        if (task == null) return null;

        if (task.getId() == null) {
            // No id provided: cannot update
            return null;
        }

        // Use conditional update by id and (optionally) userId to avoid relying on a prior select
        try {
            Long id = task.getId();
            Long userId = task.getUserId();
            logger.info("[UPDATE] updating task id={}, userId={}", id, userId);
            // Fetch existing record to validate ownership and deleted flag
            Task existing = taskMapper.selectById(id);
            if (existing == null) {
                logger.info("[UPDATE] existing task id={} not found", id);
                return null;
            }
            if (existing.getDeleted() != null && existing.getDeleted() == 1) {
                logger.info("[UPDATE] existing task id={} is deleted", id);
                return null;
            }
            Long currentUser = UserUtil.getCurrentUserId();
            if (currentUser == null) {
                logger.warn("[UPDATE] current user is null, rejecting update for id={}", id);
                return null;
            }
            if (!currentUser.equals(existing.getUserId())) {
                logger.warn("[UPDATE] ownership mismatch: currentUser={} vs task.userId={} for id={}", currentUser, existing.getUserId(), id);
                return null;
            }

            task.setUpdatedAt(LocalDateTime.now());
            com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<Task> wrapper = new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<>();
            wrapper.eq("id", id);
            // only update active (not deleted) record
            wrapper.eq("deleted", 0);
            int rows = taskMapper.update(task, wrapper);
            logger.info("[UPDATE] update affected rows={} for id={}", rows, id);
            if (rows <= 0) {
                return null;
            }
            return taskMapper.selectById(id);
        } catch (Exception ex) {
            logger.error("[UPDATE] failed for id={}: {}", task.getId(), ex.getMessage(), ex);
            return null;
        }
    }

    @Override
    @Transactional
    public boolean delete(Long id) {
        try {
            Task existing = taskMapper.selectById(id);
            if (existing == null) {
                logger.info("[DELETE] Task id={} not found", id);
                return false;
            }
            Long currentUser = UserUtil.getCurrentUserId();
            if (currentUser == null || !currentUser.equals(existing.getUserId())) {
                logger.warn("[DELETE] ownership mismatch or unauthenticated delete attempt for id={}, currentUser={}", id, currentUser);
                return false;
            }
            Task t = new Task();
            t.setId(id);
            t.setDeleted(1);
            t.setUpdatedAt(LocalDateTime.now());
            com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<Task> wrapper = new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<>();
            wrapper.eq("id", id).eq("user_id", currentUser).eq("deleted", 0);
            int rows = taskMapper.update(t, wrapper);
            logger.info("[DELETE] conditional update affected rows={} for id={}", rows, id);
            return rows > 0;
        } catch (Exception ex) {
            logger.error("[DELETE] failed for id={}: {}", id, ex.getMessage(), ex);
            return false;
        }
    }

    @Override
    public void complete(Long id, Integer actualMinutes) {
        Task t = new Task();
        t.setId(id);
        t.setStatus(1);
        t.setActualMinutes(actualMinutes);
        t.setCompletedAt(java.time.LocalDateTime.now());
        taskMapper.updateById(t);
    }

    @Override
    public List<Task> listWithFilters(Long userId, Long categoryId, String priority, java.time.LocalDateTime start, java.time.LocalDateTime end, Boolean completed) {
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Task> wrapper = new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        wrapper.eq("user_id", userId).eq("deleted", 0);
        if (categoryId != null) wrapper.eq("category_id", categoryId);
        if (priority != null && !priority.isEmpty()) wrapper.eq("priority", priority);
        if (completed != null) wrapper.eq("status", completed ? 1 : 0);
        if (start != null && end != null) {
            // filter by start_time (historically used as deadline/start) within provided window
            wrapper.ge("start_time", start).le("start_time", end);
        }
        wrapper.orderByDesc("created_at");
        return taskMapper.selectList(wrapper);
    }
}
