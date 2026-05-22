package com.timemanager.service;

import com.timemanager.entity.UserMessage;
import com.timemanager.mapper.UserMessageMapper;
import com.timemanager.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class UserMessageService {

    @Autowired
    private UserMessageMapper userMessageMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired(required = false)
    private AlertPushService alertPushService;

    public UserMessage sendFromAdmin(Long fromAdminId, Long userId, String title, String content) {
        try {
            UserMessage m = UserMessage.builder()
                    .userId(userId)
                    .fromAdminId(fromAdminId)
                    .title(title)
                    .content(content)
                    .isRead(false)
                    .isDeleted(false)
                    .createdAt(LocalDateTime.now())
                    .build();
            userMessageMapper.insert(m);

            // push to user via websocket (separate channel /queue/messages)
            if (alertPushService != null) {
                Map<String, Object> payload = new HashMap<>();
                payload.put("id", m.getId());
                payload.put("title", m.getTitle());
                payload.put("content", m.getContent());
                    payload.put("userId", m.getUserId());
                    // 将关键 id 字段序列化为字符串以避免前端在 JSON.parse 时丢失大整数精度
                    payload.put("id", m.getId() != null ? String.valueOf(m.getId()) : null);
                    payload.put("fromAdminId", m.getFromAdminId() != null ? String.valueOf(m.getFromAdminId()) : null);
                    payload.put("createdAt", m.getCreatedAt() != null ? m.getCreatedAt().toString() : null);
                    payload.put("userId", m.getUserId() != null ? String.valueOf(m.getUserId()) : null);
                // Try to resolve username for websocket push; fall back to userId string
                try {
                    String username = null;
                    if (userMapper != null) {
                        com.timemanager.entity.User u = userMapper.selectById(userId);
                        if (u != null && u.getUsername() != null) username = u.getUsername();
                    }
                    if (username != null) {
                        try { alertPushService.sendUserMessageToUser(username, payload); } catch (Exception ex) { /* best-effort */ }
                    } else {
                        try { alertPushService.sendToUser(String.valueOf(userId), payload); } catch (Exception ex) { /* best-effort */ }
                    }
                } catch (Exception ex) {
                    try { alertPushService.sendToUser(String.valueOf(userId), payload); } catch (Exception ignored) {}
                }
            }

            return m;
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public List<UserMessage> listForUser(Long userId, int offset, int limit) {
        return userMessageMapper.selectByUserId(userId, offset, limit);
    }

    public int countForUser(Long userId) {
        return userMessageMapper.countByUserId(userId);
    }

    public boolean markRead(Long userId, Long id) {
        try {
            int updated = userMessageMapper.markRead(id, userId);
            if (updated > 0) return true;
            // 如果没有更新，可能是因为已被标记为已读，检查记录归属并返回已读状态
            try {
                com.timemanager.entity.UserMessage m = userMessageMapper.selectById(id);
                if (m != null && m.getUserId() != null && m.getUserId().equals(userId)) {
                    if (Boolean.TRUE.equals(m.getIsRead())) return true;
                }
            } catch (Exception ex) {
                // ignore
            }
            return false;
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }

    public boolean softDelete(Long userId, Long id) {
        try {
            int updated = userMessageMapper.softDelete(id, userId);
            if (updated > 0) return true;
            // 若未更新，可能是已删除，检查记录归属并返回
            try {
                com.timemanager.entity.UserMessage m = userMessageMapper.selectById(id);
                if (m != null && m.getUserId() != null && m.getUserId().equals(userId)) {
                    if (Boolean.TRUE.equals(m.getIsDeleted())) return true;
                }
            } catch (Exception ex) {
                // ignore
            }
            return false;
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }
}
