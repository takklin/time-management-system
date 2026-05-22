package com.timemanager.ai.service;

import com.timemanager.mapper.TaskMapper;
import com.timemanager.entity.Task;
import com.timemanager.entity.Schedule;
import com.timemanager.service.ScheduleService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

/**
 * 用户 AI 服务
 * 提供用户端的 AI 功能：
 * - 自然语言创建任务
 * - 任务智能解析
 * - 今日总结生成
 * - 通用对话
 */
@Slf4j
@Service
public class UserAiService {
    
    @Autowired
    private DynamicAiService dynamicAiService;
    
    @Autowired
    private TaskMapper taskMapper;

    @Autowired
    private ScheduleService scheduleService;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    // Toggle: if true, `promote` delegates intent/time parsing entirely to LLM.
    // Change to true to enable model-only mode; set to false to use rule-based logic (default).
    private static final boolean PROMOTE_MODEL_ONLY = true;
    
    /**
     * 基础对话 - 用户端 AI 智能助手
     * 针对普通用户的任务管理场景
     * @param userId 用户ID
     * @param message 用户消息
     * @param model 可选的模型提供商 (e.g., "chatgpt3.5", "deepseek")，为空时使用当前激活的
     */
    public String chat(Long userId, String message, String model) {
        // 智能判断用户意图
        String intent = detectUserIntent(message);
        
        String systemPrompt = """
            你是一个温暖、亲切的个人时间管理助手，像贴心朋友一样与用户交流。

            【你的职责】：
            1. 📝 帮助创建任务：当用户说"我要...", "帮我...", "创建"时，主动询问任务详情
            2. 📊 查询任务：当用户问"我的任务", "完成了多少", "今天的任务"时，描述可能的查询方式
            3. 💡 效率建议：基于任务数量、完成率给出时间管理建议
            4. 🎯 激励鼓励：当用户表达困难或疲劳时，给予积极鼓励
            5. ⏰ 时间规划：帮助用户制定合理的任务计划

            【交互风格】：
            - ✅ 语气友好、温暖，像朋友一样，适当使用表情符号（例如 😊、💪、📝、⏰、🌟）
            - ✅ 回答简洁清晰，力求可执行
            - ✅ 在确认任务创建时，content 要包含肯定且鼓励的话语（例如："💪 好的，已记下！"）
            - ✅ 如果信息不足以创建任务，要用友好语气询问澄清问题

            【检测意图】：""" + intent + """

            用户ID: """ + userId + """
            当前时间: """ + LocalDateTime.now() + """

            【示例回复】：
            用户："我今天很累"
            你的回复："☕ 累的时候要好好休息！我在这儿支持你。要不要我帮你把今天的任务整理一下？"

            用户："帮我创建一个任务"
            你的回复："📝 好的！告诉我任务是什么呢？比如：任务的名称、预计时长、什么时候想做？"
            """;
        
        log.info("[用户AI] 检测到意图: {}, 用户消息: {}, 指定模型: {}", intent, message, model);
        
        // 如果指定了模型则使用指定模型，否则使用默认激活的
        return dynamicAiService.chat(systemPrompt, message, model);
    }

    /**
     * 增强对话：接收前端传入的上下文（结构化）以及会话历史，将其格式化并插入到用户消息中，再调用 DynamicAiService
     * 要求 LLM 尽量返回纯 JSON 格式，格式示例：
     * {
     *   "type": "answer" | "create_task" | "create_schedule",
     *   "content": "可读文本回复",
     *   "data": { ... 结构化数据 ... }
     * }
     * 如果 LLM 返回无法解析为 JSON，则会回退为 { type: 'answer', content: 原始回复 }
     *
     * @param userId 用户ID
     * @param messages 会话历史（按时间顺序）
     * @param question 用户的原始问题
     * @param context 前端传入的上下文（可为 null）
     * @param model 可选的模型提供商
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public Object promote(Long userId, List<Map<String, Object>> messages, String question, Map<String, Object> context, String model) {
                // 不再使用本地意图检测，promote 完全依赖大模型判断。
                // 系统提示词中明确要求：仅当用户明确使用“创建/添加/安排”等动词时才返回 create_task/create_schedule
                                String systemPrompt = """
                                                你是一个温暖、亲切的个人时间管理助手，像贴心朋友一样与用户交流。请在 content 中使用友好语气并适当加入表情符号（例如 😊、💪、📝、⏰）。

                                                输出格式要求（严格）：
                                                - 必须返回有效的 JSON（不要包含其它多余的文字），JSON 结构如下：
                                                {
                                                        "type": "answer" | "create_task" | "create_schedule",
                                                        "content": "对用户的可读回复（友好且含 emoji）",
                                                        "data": { ... 可选的结构化字段 ... }
                                                }

                                                重要判定规则（请严格遵守）：
                                                - 仅当用户**明确**使用类似“创建”、“添加”、“安排”等明确动词时，才返回 create_task 或 create_schedule；否则返回 answer。
                                                - 当返回 create_task 时，data 应尽可能包含下列字段（若无法推断，请置为 null 并在 content 中友好询问）：
                                                    - title: 任务标题
                                                    - startTime: ISO 8601 字符串（例如 2026-05-14T08:00:00），若用户只给出相对时间（如“今天上午8点”），请基于当前日期转换为绝对时间。
                                                    - deadline: （可选）ISO 8601 字符串
                                                    - estimatedMinutes: 预估时长（整数，分钟）
                                                - 当返回 create_schedule 时，data 应包含 startTime 与 endTime（ISO 8601）。
                                                - 如果检测到与用户现有日程时间冲突，返回 update_schedule，data 中包含 original（已有日程）和 proposed（建议更改）。
                                                - 如果无法确定或信息不足，请返回 type = "answer" 并在 content 中用友好语气询问澄清问题。

                                                注意：content 必须以轻松、鼓励的语气回答，并可包含 emoji，例如“💪 已为你安排好啦～”。
                                                """ + "\n用户ID:" + userId + "\n当前时间:" + LocalDateTime.now();

        StringBuilder userMsgBuilder = new StringBuilder();

        // 附加最近的会话历史（最多 recent 12 条）
        if (messages != null && !messages.isEmpty()) {
            int from = Math.max(0, messages.size() - 12);
            userMsgBuilder.append("=== 最近会话历史（最近优先） ===\n");
            for (int i = from; i < messages.size(); i++) {
                Map msg = messages.get(i);
                Object role = msg.get("role");
                Object content = msg.get("content");
                userMsgBuilder.append(role == null ? "?" : role.toString()).append(": ");
                userMsgBuilder.append(content == null ? "" : content.toString()).append("\n");
            }
            userMsgBuilder.append("=== 会话历史结束 ===\n\n");
        }

        userMsgBuilder.append("用户问题: ").append(question).append("\n\n");

        if (context != null && !context.isEmpty()) {
            userMsgBuilder.append("=== 前端传入的上下文开始 ===\n");

            Object highObj = context.get("high_priority_tasks");
            if (highObj instanceof List) {
                userMsgBuilder.append("高优任务:\n");
                for (Object item : (List) highObj) {
                    if (item instanceof Map) {
                        Map map = (Map) item;
                        Object title = map.get("title");
                        Object deadline = map.get("deadline");
                        Object est = map.get("estimatedMinutes");
                        userMsgBuilder.append("- ").append(title == null ? "(无标题)" : title.toString());
                        if (deadline != null) userMsgBuilder.append(" 截止:").append(deadline.toString());
                        if (est != null) userMsgBuilder.append(" 预估:").append(est.toString()).append("分");
                        userMsgBuilder.append("\n");
                    }
                }
            }

            Object mediumObj = context.get("medium_priority_tasks");
            if (mediumObj instanceof List) {
                userMsgBuilder.append("中优任务:\n");
                for (Object item : (List) mediumObj) {
                    if (item instanceof Map) {
                        Map map = (Map) item;
                        Object title = map.get("title");
                        Object deadline = map.get("deadline");
                        Object est = map.get("estimatedMinutes");
                        userMsgBuilder.append("- ").append(title == null ? "(无标题)" : title.toString());
                        if (deadline != null) userMsgBuilder.append(" 截止:").append(deadline.toString());
                        if (est != null) userMsgBuilder.append(" 预估:").append(est.toString()).append("分");
                        userMsgBuilder.append("\n");
                    }
                }
            }

            Object procObj = context.get("procrastinate_tasks");
            if (procObj instanceof List) {
                userMsgBuilder.append("可拖延鱼塘（低优）:\n");
                for (Object item : (List) procObj) {
                    if (item instanceof Map) {
                        Map map = (Map) item;
                        Object title = map.get("title");
                        Object deadline = map.get("deadline");
                        userMsgBuilder.append("- ").append(title == null ? "(无标题)" : title.toString());
                        if (deadline != null) userMsgBuilder.append(" 截止:").append(deadline.toString());
                        userMsgBuilder.append("\n");
                    }
                }
            }

            Object completedObj = context.get("completed_tasks");
            if (completedObj instanceof List) {
                List completedList = (List) completedObj;
                if (!completedList.isEmpty()) {
                    userMsgBuilder.append("已完成任务（共 ").append(completedList.size()).append(" 项）:\n");
                    for (Object item : completedList) {
                        if (item instanceof Map) {
                            Map map = (Map) item;
                            Object title = map.get("title");
                            Object compAt = map.get("completedAt");
                            userMsgBuilder.append("- ").append(title == null ? "(无标题)" : title.toString());
                            if (compAt != null) userMsgBuilder.append(" 完成时间:").append(compAt.toString());
                            userMsgBuilder.append("\n");
                        }
                    }
                }
            }

            Object countsObj = context.get("counts");
            if (countsObj instanceof Map) {
                Map counts = (Map) countsObj;
                userMsgBuilder.append(String.format("任务计数 - 高:%s 中:%s 低:%s 今日:%s\n",
                    counts.getOrDefault("high", 0), counts.getOrDefault("medium", 0), counts.getOrDefault("low", 0), counts.getOrDefault("today", 0)
                ));
            }

            Object overload = context.get("overload");
            if (overload != null) {
                userMsgBuilder.append("是否过载: ").append(overload.toString()).append("\n");
            }

            Object weekly = context.get("weekly_core_done");
            if (weekly != null) {
                userMsgBuilder.append("本周核心任务完成数: ").append(weekly.toString()).append("\n");
            }

            userMsgBuilder.append("=== 上下文结束 ===\n\n");
        }

        // 最终用户消息文本
        String userMessage = userMsgBuilder.toString();

        // 如果启用模型优先模式，则将所有判断交给大模型（移除本地规则），直接返回模型输出（首个 JSON）
        if (PROMOTE_MODEL_ONLY) {
                                String systemPromptModelOnly = """
                                        你是一个温暖、亲切的时间管理助手，像朋友一样与用户对话。请使用自然友好的语气，并适度加入表情符号（例如 😊、💪、📝、⏰）。

                                        输出格式要求（严格）：
                                        - 必须只返回有效的 JSON，不要输出任何额外文字。可以返回单个 JSON 对象或 JSON 数组。每个元素结构如下：
                                        {
                                            "type": "answer" | "create_task" | "create_schedule" | "update_schedule",
                                            "content": "对用户的可读回复（友好且含 emoji）",
                                            "data": { ... }
                                        }

                                        对于 create_task，data 必须尽可能包含：
                                        - "title": 任务标题（字符串）
                                        - "startTime": ISO 8601（例如 2026-05-14T08:00:00），若用户给出相对时间（如“今天上午8点”），请基于当前日期转换为绝对时间。
                                        - "deadline": 可选 ISO 8601
                                        - "estimatedMinutes": 整数（分钟）
                                        如果无法推断某字段，请将其设为 null，并在 content 中用友好语气询问用户以补全信息。

                                        对于 create_schedule，data 必须包含 "startTime" 与 "endTime"（ISO 8601）。
                                        当检测到与用户现有日程冲突时，返回 update_schedule，data 中包含 original（已有日程）和 proposed（建议更改）。
                                        如果用户请求一次创建多个任务，返回 JSON 数组，每项为 create_task。
                                        """ + "\n当前日期: " + LocalDate.now() + " 当前时间: " + LocalDateTime.now();

                    // 构建结构化的消息历史（将前端传来的 messages 列表按 role/content 传给模型）
                    List<Map<String, String>> chatHistory = new java.util.ArrayList<>();
                    if (messages != null && !messages.isEmpty()) {
                        int from = Math.max(0, messages.size() - 12);
                        for (int i = from; i < messages.size(); i++) {
                            Map msg = messages.get(i);
                            Object role = msg.get("role");
                            Object content = msg.get("content");
                            chatHistory.add(Map.of(
                                    "role", role == null ? "user" : role.toString(),
                                    "content", content == null ? "" : content.toString()
                            ));
                        }
                    }

                    // 构建 context 文本（与原先拼接到 userMessage 的内容一致）
                    StringBuilder contextBuilder = new StringBuilder();
                    if (context != null && !context.isEmpty()) {
                        contextBuilder.append("=== 前端传入的上下文开始 ===\n");

                        Object highObj = context.get("high_priority_tasks");
                        if (highObj instanceof List) {
                            contextBuilder.append("高优任务:\n");
                            for (Object item : (List) highObj) {
                                if (item instanceof Map) {
                                    Map map = (Map) item;
                                    Object title = map.get("title");
                                    Object deadline = map.get("deadline");
                                    Object est = map.get("estimatedMinutes");
                                    contextBuilder.append("- ").append(title == null ? "(无标题)" : title.toString());
                                    if (deadline != null) contextBuilder.append(" 截止:").append(deadline.toString());
                                    if (est != null) contextBuilder.append(" 预估:").append(est.toString()).append("分");
                                    contextBuilder.append("\n");
                                }
                            }
                        }

                        Object mediumObj = context.get("medium_priority_tasks");
                        if (mediumObj instanceof List) {
                            contextBuilder.append("中优任务:\n");
                            for (Object item : (List) mediumObj) {
                                if (item instanceof Map) {
                                    Map map = (Map) item;
                                    Object title = map.get("title");
                                    Object deadline = map.get("deadline");
                                    Object est = map.get("estimatedMinutes");
                                    contextBuilder.append("- ").append(title == null ? "(无标题)" : title.toString());
                                    if (deadline != null) contextBuilder.append(" 截止:").append(deadline.toString());
                                    if (est != null) contextBuilder.append(" 预估:").append(est.toString()).append("分");
                                    contextBuilder.append("\n");
                                }
                            }
                        }

                        Object procObj = context.get("procrastinate_tasks");
                        if (procObj instanceof List) {
                            contextBuilder.append("可拖延鱼塘（低优）:\n");
                            for (Object item : (List) procObj) {
                                if (item instanceof Map) {
                                    Map map = (Map) item;
                                    Object title = map.get("title");
                                    Object deadline = map.get("deadline");
                                    contextBuilder.append("- ").append(title == null ? "(无标题)" : title.toString());
                                    if (deadline != null) contextBuilder.append(" 截止:").append(deadline.toString());
                                    contextBuilder.append("\n");
                                }
                            }
                        }

                        Object completedObj = context.get("completed_tasks");
                        if (completedObj instanceof List) {
                            List completedList = (List) completedObj;
                            if (!completedList.isEmpty()) {
                                contextBuilder.append("已完成任务（共 ").append(completedList.size()).append(" 项）:\n");
                                for (Object item : completedList) {
                                    if (item instanceof Map) {
                                        Map map = (Map) item;
                                        Object title = map.get("title");
                                        Object compAt = map.get("completedAt");
                                        contextBuilder.append("- ").append(title == null ? "(无标题)" : title.toString());
                                        if (compAt != null) contextBuilder.append(" 完成时间:").append(compAt.toString());
                                        contextBuilder.append("\n");
                                    }
                                }
                            }
                        }

                        Object countsObj = context.get("counts");
                        if (countsObj instanceof Map) {
                            Map counts = (Map) countsObj;
                            contextBuilder.append(String.format("任务计数 - 高:%s 中:%s 低:%s 今日:%s\n",
                                    counts.getOrDefault("high", 0), counts.getOrDefault("medium", 0), counts.getOrDefault("low", 0), counts.getOrDefault("today", 0)
                            ));
                        }

                        Object overload = context.get("overload");
                        if (overload != null) {
                            contextBuilder.append("是否过载: ").append(overload.toString()).append("\n");
                        }

                        Object weekly = context.get("weekly_core_done");
                        if (weekly != null) {
                            contextBuilder.append("本周核心任务完成数: ").append(weekly.toString()).append("\n");
                        }

                        contextBuilder.append("=== 上下文结束 ===\n\n");
                    }

                    String finalUserContent = "用户问题: " + question + "\n\n" + contextBuilder.toString();
                    chatHistory.add(Map.of("role", "user", "content", finalUserContent));

                    String aiRespModelOnly = dynamicAiService.chat(systemPromptModelOnly, chatHistory, model);
            try {
                String jsonStr = extractJson(aiRespModelOnly);
                if (jsonStr == null) {
                    java.util.Map<String, Object> fallback = new java.util.HashMap<>();
                    fallback.put("type", "answer");
                    fallback.put("content", aiRespModelOnly);
                    fallback.put("data", null);
                    return fallback;
                }

                // 支持数组或对象
                String trimmed = jsonStr.trim();
                if (trimmed.startsWith("[")) {
                    List parsedList = objectMapper.readValue(jsonStr, List.class);
                    // 对于其中的 create_schedule 项，做一次冲突检测并替换为 update_schedule（如有冲突）
                    List<Object> out = new java.util.ArrayList<>();
                    for (Object it : parsedList) {
                        if (!(it instanceof Map)) { out.add(it); continue; }
                        Map item = (Map) it;
                        String typeStr = item.get("type") == null ? null : String.valueOf(item.get("type"));
                        if ("create_schedule".equals(typeStr)) {
                            Map dataMap = (Map) item.get("data");
                            if (dataMap != null) {
                                Object s0 = dataMap.get("startTime");
                                Object e0 = dataMap.get("endTime");
                                LocalDateTime pst = tryParseDateTime(String.valueOf(s0));
                                LocalDateTime pet = tryParseDateTime(String.valueOf(e0));
                                if (pst != null && pet != null) {
                                    // 查询附近日程
                                    String startDate = pst.toLocalDate().toString();
                                    String endDate = pet.toLocalDate().toString();
                                    try {
                                        List<Schedule> existing = scheduleService.list(userId, startDate, endDate);
                                        boolean conflict = false;
                                        Schedule conflictSch = null;
                                        for (Schedule sch : existing) {
                                            if (sch == null || sch.getStartTime() == null || sch.getEndTime() == null) continue;
                                            if (!(sch.getEndTime().isBefore(pst) || sch.getStartTime().isAfter(pet))) {
                                                conflict = true; conflictSch = sch; break;
                                            }
                                        }
                                        if (conflict && conflictSch != null) {
                                            Map<String,Object> update = new java.util.HashMap<>();
                                            update.put("type", "update_schedule");
                                            update.put("content", "检测到与已有日程冲突，请确认修改或保留两者");
                                            Map<String,Object> d = new java.util.HashMap<>();
                                            d.put("original", scheduleToMap(conflictSch));
                                            d.put("proposed", dataMap);
                                            update.put("data", d);
                                            out.add(update);
                                            continue;
                                        }
                                    } catch (Exception e) {
                                        log.warn("[用户AI] 冲突检测失败：{}", e.getMessage());
                                    }
                                }
                            }
                        }
                        out.add(item);
                    }
                    return out;
                } else {
                    Map parsed = objectMapper.readValue(jsonStr, Map.class);
                    // 对单个 create_schedule 做冲突检测
                    try {
                        Object typeObj = parsed.get("type");
                        String typeStr = typeObj == null ? null : String.valueOf(typeObj);
                        if ("create_schedule".equals(typeStr)) {
                            Map dataMap = (Map) parsed.get("data");
                            if (dataMap != null) {
                                Object s0 = dataMap.get("startTime");
                                Object e0 = dataMap.get("endTime");
                                LocalDateTime pst = tryParseDateTime(String.valueOf(s0));
                                LocalDateTime pet = tryParseDateTime(String.valueOf(e0));
                                if (pst != null && pet != null) {
                                    List<Schedule> existing = scheduleService.list(userId, pst.toLocalDate().toString(), pet.toLocalDate().toString());
                                    for (Schedule sch : existing) {
                                        if (sch == null || sch.getStartTime() == null || sch.getEndTime() == null) continue;
                                        if (!(sch.getEndTime().isBefore(pst) || sch.getStartTime().isAfter(pet))) {
                                            Map<String,Object> update = new java.util.HashMap<>();
                                            update.put("type", "update_schedule");
                                            update.put("content", "检测到与已有日程冲突，请确认修改或保留两者");
                                            Map<String,Object> d = new java.util.HashMap<>();
                                            d.put("original", scheduleToMap(sch));
                                            d.put("proposed", dataMap);
                                            update.put("data", d);
                                            return update;
                                        }
                                    }
                                }
                            }
                        }
                    } catch (Exception ignore) {}
                    return parsed;
                }
            } catch (Exception e) {
                log.warn("[用户AI] 模型优先模式：无法解析为 JSON，回退为原始文本。错误: {}", e.getMessage());
                java.util.Map<String, Object> fallback = new java.util.HashMap<>();
                fallback.put("type", "answer");
                fallback.put("content", aiRespModelOnly);
                fallback.put("data", null);
                return fallback;
            }
        }
        // 本版本已移除本地意图/时间强制判定，promote 仅依赖模型的解析与返回（本地解析若需保留仅作为辅助不覆盖模型结果）

        // 调用 DynamicAiService（支持指定 provider/model），使用结构化历史消息以确保模型能读取对话上下文
        List<Map<String, String>> chatHistoryAll = new java.util.ArrayList<>();
        if (messages != null && !messages.isEmpty()) {
            int fromAll = Math.max(0, messages.size() - 12);
            for (int i = fromAll; i < messages.size(); i++) {
                Map msg = messages.get(i);
                Object role = msg.get("role");
                Object content = msg.get("content");
                chatHistoryAll.add(Map.of(
                        "role", role == null ? "user" : role.toString(),
                        "content", content == null ? "" : content.toString()
                ));
            }
        }

        // 将上下文拼到最后一个 user 消息中（与原先 userMessage 中的顺序保持一致）
        StringBuilder ctxBuilderAll = new StringBuilder();
        if (context != null && !context.isEmpty()) {
            ctxBuilderAll.append("=== 前端传入的上下文开始 ===\n");
            Object highObj = context.get("high_priority_tasks");
            if (highObj instanceof List) {
                ctxBuilderAll.append("高优任务:\n");
                for (Object item : (List) highObj) {
                    if (item instanceof Map) {
                        Map map = (Map) item;
                        Object title = map.get("title");
                        Object deadline = map.get("deadline");
                        Object est = map.get("estimatedMinutes");
                        ctxBuilderAll.append("- ").append(title == null ? "(无标题)" : title.toString());
                        if (deadline != null) ctxBuilderAll.append(" 截止:").append(deadline.toString());
                        if (est != null) ctxBuilderAll.append(" 预估:").append(est.toString()).append("分");
                        ctxBuilderAll.append("\n");
                    }
                }
            }
            Object mediumObj = context.get("medium_priority_tasks");
            if (mediumObj instanceof List) {
                ctxBuilderAll.append("中优任务:\n");
                for (Object item : (List) mediumObj) {
                    if (item instanceof Map) {
                        Map map = (Map) item;
                        Object title = map.get("title");
                        Object deadline = map.get("deadline");
                        Object est = map.get("estimatedMinutes");
                        ctxBuilderAll.append("- ").append(title == null ? "(无标题)" : title.toString());
                        if (deadline != null) ctxBuilderAll.append(" 截止:").append(deadline.toString());
                        if (est != null) ctxBuilderAll.append(" 预估:").append(est.toString()).append("分");
                        ctxBuilderAll.append("\n");
                    }
                }
            }
            Object procObj = context.get("procrastinate_tasks");
            if (procObj instanceof List) {
                ctxBuilderAll.append("可拖延鱼塘（低优）:\n");
                for (Object item : (List) procObj) {
                    if (item instanceof Map) {
                        Map map = (Map) item;
                        Object title = map.get("title");
                        Object deadline = map.get("deadline");
                        ctxBuilderAll.append("- ").append(title == null ? "(无标题)" : title.toString());
                        if (deadline != null) ctxBuilderAll.append(" 截止:").append(deadline.toString());
                        ctxBuilderAll.append("\n");
                    }
                }
            }
            Object completedObj = context.get("completed_tasks");
            if (completedObj instanceof List) {
                List completedList = (List) completedObj;
                if (!completedList.isEmpty()) {
                    ctxBuilderAll.append("已完成任务（共 ").append(completedList.size()).append(" 项）:\n");
                    for (Object item : completedList) {
                        if (item instanceof Map) {
                            Map map = (Map) item;
                            Object title = map.get("title");
                            Object compAt = map.get("completedAt");
                            ctxBuilderAll.append("- ").append(title == null ? "(无标题)" : title.toString());
                            if (compAt != null) ctxBuilderAll.append(" 完成时间:").append(compAt.toString());
                            ctxBuilderAll.append("\n");
                        }
                    }
                }
            }
            Object countsObj = context.get("counts");
            if (countsObj instanceof Map) {
                Map counts = (Map) countsObj;
                ctxBuilderAll.append(String.format("任务计数 - 高:%s 中:%s 低:%s 今日:%s\n",
                        counts.getOrDefault("high", 0), counts.getOrDefault("medium", 0), counts.getOrDefault("low", 0), counts.getOrDefault("today", 0)
                ));
            }
            Object overload = context.get("overload");
            if (overload != null) {
                ctxBuilderAll.append("是否过载: ").append(overload.toString()).append("\n");
            }
            Object weekly = context.get("weekly_core_done");
            if (weekly != null) {
                ctxBuilderAll.append("本周核心任务完成数: ").append(weekly.toString()).append("\n");
            }
            ctxBuilderAll.append("=== 上下文结束 ===\n\n");
        }

        String finalUser = "用户问题: " + question + "\n\n" + ctxBuilderAll.toString();
        chatHistoryAll.add(Map.of("role", "user", "content", finalUser));

        String aiResp = dynamicAiService.chat(systemPrompt, chatHistoryAll, model);

        // 尝试解析为 JSON（取第一个完整 JSON）
        try {
            String jsonStr = extractJson(aiResp);
            if (jsonStr == null) {
                // 如果没有找到有效 JSON，回退为原始文本
                java.util.Map<String, Object> fallback = new java.util.HashMap<>();
                fallback.put("type", "answer");
                fallback.put("content", aiResp);
                fallback.put("data", null);
                return fallback;
            }

            String trimmed = jsonStr.trim();
            if (trimmed.startsWith("[")) {
                List parsedList = objectMapper.readValue(jsonStr, List.class);
                List<Object> out = new java.util.ArrayList<>();
                for (Object it : parsedList) {
                    if (!(it instanceof Map)) { out.add(it); continue; }
                    Map item = (Map) it;
                    String typeStr = item.get("type") == null ? null : String.valueOf(item.get("type"));
                    if ("create_schedule".equals(typeStr)) {
                        Map dataMap = (Map) item.get("data");
                        if (dataMap != null) {
                            Object s0 = dataMap.get("startTime");
                            Object e0 = dataMap.get("endTime");
                            LocalDateTime pst = tryParseDateTime(String.valueOf(s0));
                            LocalDateTime pet = tryParseDateTime(String.valueOf(e0));
                            if (pst != null && pet != null) {
                                try {
                                    List<Schedule> existing = scheduleService.list(userId, pst.toLocalDate().toString(), pet.toLocalDate().toString());
                                    boolean conflict = false;
                                    Schedule conflictSch = null;
                                    for (Schedule sch : existing) {
                                        if (sch == null || sch.getStartTime() == null || sch.getEndTime() == null) continue;
                                        if (!(sch.getEndTime().isBefore(pst) || sch.getStartTime().isAfter(pet))) {
                                            conflict = true; conflictSch = sch; break;
                                        }
                                    }
                                    if (conflict && conflictSch != null) {
                                        Map<String,Object> update = new java.util.HashMap<>();
                                        update.put("type", "update_schedule");
                                        update.put("content", "检测到与已有日程冲突，请确认修改或保留两者");
                                        Map<String,Object> d = new java.util.HashMap<>();
                                        d.put("original", scheduleToMap(conflictSch));
                                        d.put("proposed", dataMap);
                                        update.put("data", d);
                                        out.add(update);
                                        continue;
                                    }
                                } catch (Exception ex) { log.warn("[用户AI] 冲突检查失败", ex.getMessage()); }
                            }
                        }
                    }
                    out.add(item);
                }
                return out;
            } else {
                Map parsed = objectMapper.readValue(jsonStr, Map.class);
                try {
                    Object typeObj = parsed.get("type");
                    String typeStr = typeObj == null ? null : String.valueOf(typeObj);
                    if ("create_schedule".equals(typeStr)) {
                        Map dataMap = (Map) parsed.get("data");
                        if (dataMap != null) {
                            Object s0 = dataMap.get("startTime");
                            Object e0 = dataMap.get("endTime");
                            LocalDateTime pst = tryParseDateTime(String.valueOf(s0));
                            LocalDateTime pet = tryParseDateTime(String.valueOf(e0));
                            if (pst != null && pet != null) {
                                List<Schedule> existing = scheduleService.list(userId, pst.toLocalDate().toString(), pet.toLocalDate().toString());
                                for (Schedule sch : existing) {
                                    if (sch == null || sch.getStartTime() == null || sch.getEndTime() == null) continue;
                                    if (!(sch.getEndTime().isBefore(pst) || sch.getStartTime().isAfter(pet))) {
                                        Map<String,Object> update = new java.util.HashMap<>();
                                        update.put("type", "update_schedule");
                                        update.put("content", "检测到与已有日程冲突，请确认修改或保留两者");
                                        Map<String,Object> d = new java.util.HashMap<>();
                                        d.put("original", scheduleToMap(sch));
                                        d.put("proposed", dataMap);
                                        update.put("data", d);
                                        return update;
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception ignore) {}
                return parsed;
            }
        } catch (Exception e) {
            log.warn("[用户AI] 无法解析为 JSON，返回原始文本。错误: {}", e.getMessage());
            java.util.Map<String, Object> fallback = new java.util.HashMap<>();
            fallback.put("type", "answer");
            fallback.put("content", aiResp);
            fallback.put("data", null);
            return fallback;
        }
    }
    
    /**
     * 基础对话 - 用户端 AI 智能助手（使用默认模型）
     * 针对普通用户的任务管理场景
     */
    public String chat(Long userId, String message) {
        return chat(userId, message, null);
    }
    
    /**
     * 检测用户意图（本地快速判断）
     */
    private String detectUserIntent(String message) {
        if (message == null) return "GENERAL_CHAT - 一般对话";
        if (message.contains("累") || message.contains("困") || message.contains("难")) {
            return "USER_FEELING_TIRED - 用户表达困难/疲劳";
        } else if (message.contains("创建") || message.contains("新建") || message.contains("帮我") || message.contains("安排")) {
            return "CREATE_TASK - 用户想创建新任务";
        } else if (message.contains("完成") || message.contains("多少") || message.contains("几个") || message.contains("查询") || message.contains("统计")) {
            return "QUERY_TASKS - 用户想查询任务信息";
        } else if (message.contains("建议") || message.contains("如何") || message.contains("怎样") || message.contains("方法")) {
            return "REQUEST_ADVICE - 用户请求建议";
        } else if (message.equalsIgnoreCase("你好") || message.equalsIgnoreCase("hi") || message.equalsIgnoreCase("hello")) {
            return "GREETING - 问候";
        } else {
            return "GENERAL_CHAT - 一般对话";
        }
    }
    
    /**
     * 自然语言解析为任务结构化数据
     * 例如: "明天下午3点开会" -> JSON 包含标题、时间、时长等
     */
    public TaskParseResult parseTaskFromNaturalLanguage(String input) {
        String systemPrompt = """
            你是一个任务解析专家。从用户的自然语言输入中提取任务信息。

            必须返回纯 JSON 格式（不要有其他文字）：
            {
                "title": "任务标题",
                "startTime": "YYYY-MM-DDTHH:mm:ss" 或 null,
                "deadline": "YYYY-MM-DDTHH:mm:ss" 或 null,
                "estimatedMinutes": 预估时长（分钟）或 null,
                "categoryName": "工作/学习/个人/其他" 或 null
            }

            规则：
            - 时间请使用 ISO 8601（例如 2026-05-14T08:00:00）。
            - 如果用户只给出相对时间（如“今天上午8点”），请基于当前日期转换为绝对时间。
            - 如果没有指定某些信息，则该字段置为 null。
            - 标题必须清晰简洁（不超过50字）。

            只返回 JSON，不要有其他解释。
            """;
        
        try {
            String userPrompt = "用户输入：" + input;
            String aiResp = dynamicAiService.chat(systemPrompt, userPrompt);
            
            // 提取 JSON 部分
            String jsonStr = extractJson(aiResp);
            TaskParseResult result = objectMapper.readValue(jsonStr, TaskParseResult.class);
            
            log.info("[用户AI] 任务解析成功: {}", result.getTitle());
            return result;
            
        } catch (Exception e) {
            log.error("[用户AI] 任务解析失败", e);
            return new TaskParseResult();
        }
    }
    
    /**
     * 生成今日总结
     * 查询用户今日完成情况，让 AI 生成鼓励性总结
     */
    public String generateDailySummary(Long userId) {
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.plusDays(1).atStartOfDay();
        
        // TODO: 实现数据库查询方法
        // 这里使用示例数据
        int completedCount = 5;
        long totalMinutes = 480;
        int createdCount = 8;
        
        String stats = String.format(
            "今日完成%d个任务，总耗时%d分钟，新建%d个任务。",
            completedCount, totalMinutes, createdCount
        );
        
        String systemPrompt = """
            你是一个鼓励型的效率助手。根据用户今日的任务完成情况，生成一段简短有趣的总结和鼓励。
            
            要求：
            - 不超过 80 字
            - 语气友善、充满正能量
            - 如果完成数量少，要给予鼓励
            - 如果完成数量多，要表示庆祝和认可
            
            只返回总结内容，不要前缀。
            """;
        
        return dynamicAiService.chat(systemPrompt, "数据：" + stats);
    }
    
    /**
     * 提示生成任务列表
     * 输入如："复习期末" -> 生成子任务建议列表
     */
    public String generateTaskSuggestions(String mainTask) {
        String systemPrompt = """
            你是一个任务分解专家。用户给出一个主任务，你需要分解成具体的子任务。
            
            返回格式：
            - 子任务1
            - 子任务2
            - 子任务3
            （通常 3-5 个为佳）
            
            每个子任务应该：
            - 具体、可执行
            - 相对独立
            - 有明确的完成标准
            """;
        
        return dynamicAiService.chat(systemPrompt, "主任务：" + mainTask);
    }
    
    /**
     * 从 AI 响应中提取 JSON 字符串
     */
    private String extractJson(String text) {
        if (text == null) return null;
        String s = text.trim();
        // 优先寻找数组
        int arrStart = s.indexOf('[');
        int objStart = s.indexOf('{');
        if (arrStart != -1 && (objStart == -1 || arrStart < objStart)) {
            int depth = 0;
            for (int i = arrStart; i < s.length(); i++) {
                char c = s.charAt(i);
                if (c == '[') depth++;
                else if (c == ']') {
                    depth--;
                    if (depth == 0) return s.substring(arrStart, i + 1);
                }
            }
        }
        // 否则寻找对象
        if (objStart != -1) {
            int braceCount = 0;
            for (int i = objStart; i < s.length(); i++) {
                char c = s.charAt(i);
                if (c == '{') braceCount++;
                else if (c == '}') {
                    braceCount--;
                    if (braceCount == 0) return s.substring(objStart, i + 1);
                }
            }
        }
        return null;
    }

    // 尝试解析多种常见格式为 LocalDateTime，返回 null 表示无法解析
    private LocalDateTime tryParseDateTime(String s) {
        if (s == null) return null;
        String t = s.trim();
        if (t.isEmpty() || "null".equalsIgnoreCase(t)) return null;
        try {
            // ISO 格式优先
            return LocalDateTime.parse(t, DateTimeFormatter.ISO_DATE_TIME);
        } catch (Exception ignored) {}
        try {
            // 常见的空格分隔格式：yyyy-MM-dd HH:mm:ss 或 yyyy-MM-dd HH:mm
            DateTimeFormatter f1 = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            return LocalDateTime.parse(t, f1);
        } catch (Exception ignored) {}
        try {
            DateTimeFormatter f2 = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            return LocalDateTime.parse(t, f2);
        } catch (Exception ignored) {}
        try {
            // 仅日期
            DateTimeFormatter f3 = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            return LocalDate.parse(t, f3).atStartOfDay();
        } catch (Exception ignored) {}
        return null;
    }

    private Map<String,Object> scheduleToMap(Schedule sch) {
        Map<String,Object> m = new java.util.HashMap<>();
        if (sch == null) return m;
        m.put("id", sch.getId());
        m.put("title", sch.getTitle());
        try { m.put("startTime", sch.getStartTime() == null ? null : sch.getStartTime().toString()); } catch (Exception ignored) { m.put("startTime", null); }
        try { m.put("endTime", sch.getEndTime() == null ? null : sch.getEndTime().toString()); } catch (Exception ignored) { m.put("endTime", null); }
        m.put("taskId", sch.getTaskId());
        m.put("remindBefore", sch.getRemindBefore());
        return m;
    }
    
    /**
     * 任务解析结果
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TaskParseResult {
        private String title;                    // 任务标题
        private String deadline;                 // 截止时间 (YYYY-MM-DD HH:mm)
        private Integer estimatedMinutes;        // 预估时长（分钟）
        private String categoryName;             // 分类名称
    }
}
