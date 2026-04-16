package com.timemanager.ai.enums;

/**
 * AI 查询意图枚举
 * 区分数据查询与闲聊，让 AI 真正"智能"
 * 
 * 支持自然语言理解的各类查询：统计、列表、趋势分析等
 */
public enum QueryIntent {
    // 📊 统计类查询
    NEW_USER_COUNT("查询新增用户数"),
    LOGIN_FAIL_SUMMARY("查询登录失败统计"),
    ACTIVE_USER_COUNT("查询活跃用户数"),
    TASK_COMPLETION_RATE("查询任务完成率"),
    OPERATION_LOG_ANOMALY("查询操作日志异常"),
    
    // 👥 列表类查询（新增）
    USER_LIST("查询用户列表"),
    USER_DETAIL("查询具体用户信息"),
    TASK_LIST("查询任务列表"),
    
    // 📈 趋势/对比查询（新增）
    TREND_ANALYSIS("趋势分析"),
    COMPARISON("数据对比"),
    
    // 🔍 通用类
    GENERAL_DATA_QUERY("通用数据查询"),
    
    // 💬 闲聊（非数据查询）
    CHITCHAT("闲聊/问候"),
    
    // ❓ 无法识别
    UNKNOWN("无法识别的意图");
    
    private final String description;
    
    QueryIntent(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * 判断是否属于数据查询意图
     */
    public boolean isQueryIntent() {
        return this != CHITCHAT && this != UNKNOWN;
    }
    
    /**
     * 判断是否属于闲聊意图
     */
    public boolean isChitchat() {
        return this == CHITCHAT;
    }
    
    /**
     * 判断是否属于列表查询（需要返回多条记录）
     */
    public boolean isListQuery() {
        return this == USER_LIST || this == TASK_LIST || 
               this == USER_DETAIL || this == GENERAL_DATA_QUERY;
    }
}
