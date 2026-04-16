package com.timemanager.ai.service;

import com.timemanager.ai.dto.ChatMessageDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 会话历史服务
 * 用于维护用户会话的对话历史，避免重复查询
 * 基于内存实现，可扩展为 Redis 存储
 */
@Slf4j
@Component
public class SessionHistoryService {
    
    // 每个 sessionId 对应一个聊天历史列表
    // 结构: sessionId -> LinkedList<ChatMessageDTO>
    private final Map<String, LinkedList<ChatMessageDTO>> sessionStore = new ConcurrentHashMap<>();
    
    // 最大保留消息数（每个会话）
    private static final int MAX_MESSAGES_PER_SESSION = 20;
    
    /**
     * 获取会话的最近 N 条消息
     */
    public List<ChatMessageDTO> getRecentMessages(String sessionId, int count) {
        LinkedList<ChatMessageDTO> list = sessionStore.getOrDefault(sessionId, new LinkedList<>());
        if (list.isEmpty()) {
            return new ArrayList<>();
        }
        // 返回最近 count 条消息
        int startIdx = Math.max(0, list.size() - count);
        return new ArrayList<>(list.subList(startIdx, list.size()));
    }
    
    /**
     * 获取会话的所有消息
     */
    public List<ChatMessageDTO> getAllMessages(String sessionId) {
        return new ArrayList<>(sessionStore.getOrDefault(sessionId, new LinkedList<>()));
    }
    
    /**
     * 保存消息到会话
     */
    public void saveMessage(String sessionId, ChatMessageDTO message) {
        LinkedList<ChatMessageDTO> list = sessionStore.computeIfAbsent(sessionId, k -> new LinkedList<>());
        list.add(message);
        
        // 如果消息数超过限制，删除最早的消息
        if (list.size() > MAX_MESSAGES_PER_SESSION) {
            list.removeFirst();
        }
        
        log.debug("[会话历史] sessionId={}, 消息数={}", sessionId, list.size());
    }
    
    /**
     * 保存用户消息和助手回复（一对一保存）
     */
    public void saveExchange(String sessionId, String userMsg, String assistantMsg, String intent) {
        LinkedList<ChatMessageDTO> list = sessionStore.computeIfAbsent(sessionId, k -> new LinkedList<>());
        
        // 保存用户消息
        list.add(new ChatMessageDTO("user", userMsg));
        
        // 保存助手回复
        ChatMessageDTO assistantMessage = new ChatMessageDTO("assistant", assistantMsg);
        assistantMessage.setIntent(intent);
        list.add(assistantMessage);
        
        // 清理超出限制的消息
        while (list.size() > MAX_MESSAGES_PER_SESSION) {
            list.removeFirst();
        }
        
        log.debug("[会话历史] sessionId={}, 新增对话对, 当前消息数={}", sessionId, list.size());
    }
    
    /**
     * 清空会话历史
     */
    public void clearSession(String sessionId) {
        sessionStore.remove(sessionId);
        log.info("[会话历史] sessionId={} 已清空", sessionId);
    }
    
    /**
     * 获取会话摘要（用于 AI Prompt 中的上下文）
     * 将最近几条消息拼接成字符串
     */
    public String getSessionSummary(String sessionId, int count) {
        List<ChatMessageDTO> messages = getRecentMessages(sessionId, count);
        if (messages.isEmpty()) {
            return "";
        }
        
        StringBuilder sb = new StringBuilder("---对话历史---\n");
        for (ChatMessageDTO msg : messages) {
            String roleLabel = "user".equals(msg.getRole()) ? "用户" : "助手";
            sb.append(roleLabel).append(": ").append(msg.getContent()).append("\n");
        }
        return sb.toString();
    }
    
    /**
     * 检查是否为重复问题
     * 简单实现：比对最后一条用户消息
     */
    public boolean isRepeatQuestion(String sessionId, String currentQuestion) {
        LinkedList<ChatMessageDTO> list = sessionStore.getOrDefault(sessionId, new LinkedList<>());
        if (list.isEmpty()) {
            return false;
        }
        
        // 找到最后一条用户消息
        for (int i = list.size() - 1; i >= 0; i--) {
            ChatMessageDTO msg = list.get(i);
            if ("user".equals(msg.getRole())) {
                // 计算相似度（简单方案：计算编辑距离或关键词重叠）
                return isSimilarQuestion(msg.getContent(), currentQuestion);
            }
        }
        
        return false;
    }
    
    /**
     * 判断两个问题是否相似（简单方案）
     */
    private boolean isSimilarQuestion(String q1, String q2) {
        // 移除空格，转小写，计算相似度
        String s1 = q1.replaceAll("\\s+", "").toLowerCase();
        String s2 = q2.replaceAll("\\s+", "").toLowerCase();
        
        // 完全相同
        if (s1.equals(s2)) {
            return true;
        }
        
        // 包含关键字重叠（简单判断）
        String[] words1 = s1.split("[\\p{P}]+");
        Set<String> set1 = new HashSet<>(Arrays.asList(words1));
        
        String[] words2 = s2.split("[\\p{P}]+");
        Set<String> set2 = new HashSet<>(Arrays.asList(words2));
        
        set1.retainAll(set2);
        // 如果重叠词超过 50%，判定为相似
        return set1.size() >= Math.max(words1.length, words2.length) * 0.5;
    }
    
    /**
     * 获取会话统计信息
     */
    public Map<String, Object> getSessionStats(String sessionId) {
        LinkedList<ChatMessageDTO> list = sessionStore.getOrDefault(sessionId, new LinkedList<>());
        Map<String, Object> stats = new HashMap<>();
        stats.put("sessionId", sessionId);
        stats.put("totalMessages", list.size());
        stats.put("rounds", list.size() / 2);
        stats.put("lastActivity", !list.isEmpty() ? list.getLast().getTimestamp() : null);
        return stats;
    }
}
