package com.timemanager.ai.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 简易的中文时间解析器，满足常见表达式（明天/后天、22-24号、5月14日、3点到5点等），
 * 返回一个起止时间用于判定是否跨天（从而决定是日程还是任务）。
 */
public class TimeParser {

    public static class Result {
        public LocalDateTime start;
        public LocalDateTime end;
        public boolean success;
        public boolean allDay;

        public Result() {
            this.success = false;
            this.allDay = false;
        }
    }

    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static Result parse(String text) {
        Result r = new Result();
        if (text == null || text.trim().isEmpty()) return r;
        String s = text.replaceAll("\\s+", "");
        LocalDate today = LocalDate.now();

        // 常见的相对范围：明天和后天 / 明后天
        if (s.contains("明天和后天") || s.contains("明后天") || s.contains("明天及后天")) {
            r.start = today.plusDays(1).atStartOfDay();
            r.end = today.plusDays(2).atTime(23, 59, 59);
            r.success = true;
            r.allDay = true;
            return r;
        }
        if (s.contains("今天和明天") || s.contains("今天明天")) {
            r.start = today.atStartOfDay();
            r.end = today.plusDays(1).atTime(23, 59, 59);
            r.success = true;
            r.allDay = true;
            return r;
        }

        // 22-24号 类型
        Pattern dayRange = Pattern.compile("(\\d{1,2})\\s*[-~到至]\\s*(\\d{1,2})号");
        Matcher m = dayRange.matcher(s);
        if (m.find()) {
            int sd = Integer.parseInt(m.group(1));
            int ed = Integer.parseInt(m.group(2));
            java.time.LocalDate sdate = withDayOfMonthAssumeMonth(today, sd);
            java.time.LocalDate edate = withDayOfMonthAssumeMonth(today, ed);
            if (edate.isBefore(sdate)) edate = edate.plusMonths(1);
            r.start = sdate.atStartOfDay();
            r.end = edate.atTime(23, 59, 59);
            r.success = true;
            r.allDay = true;
            return r;
        }

        // 5月14日-5月15日 或 5月14日到15日
        Pattern monthDayRange = Pattern.compile("(\\d{1,2})月(\\d{1,2})[日号]?\\s*[-~到至]\\s*(?:(\\d{1,2})月)?(\\d{1,2})[日号]?");
        m = monthDayRange.matcher(s);
        if (m.find()) {
            int sm = Integer.parseInt(m.group(1));
            int sd = Integer.parseInt(m.group(2));
            String emStr = m.group(3);
            int em = emStr != null ? Integer.parseInt(emStr) : sm;
            int ed = Integer.parseInt(m.group(4));
            java.time.LocalDate sdate = java.time.LocalDate.of(today.getYear(), sm, sd);
            java.time.LocalDate edate = java.time.LocalDate.of(today.getYear(), em, ed);
            if (edate.isBefore(sdate)) edate = edate.plusYears(1);
            r.start = sdate.atStartOfDay();
            r.end = edate.atTime(23, 59, 59);
            r.success = true;
            r.allDay = true;
            return r;
        }

        // 单日相对词（今天/明天/后天） + 尝试解析时间范围或单个时间
        if (s.contains("今天") || s.contains("今日")) {
            java.time.LocalDate td = today;
            if (tryParseTimeRangeForDate(s, td, r)) return r;
            if (tryParseSingleTimeForDate(s, td, r)) return r;
            r.start = td.atStartOfDay();
            r.end = td.atTime(23, 59, 59);
            r.success = true;
            r.allDay = true;
            return r;
        }
        if (s.contains("明天") || s.contains("明日")) {
            java.time.LocalDate td = today.plusDays(1);
            if (tryParseTimeRangeForDate(s, td, r)) return r;
            if (tryParseSingleTimeForDate(s, td, r)) return r;
            r.start = td.atStartOfDay();
            r.end = td.atTime(23, 59, 59);
            r.success = true;
            r.allDay = true;
            return r;
        }
        if (s.contains("后天")) {
            java.time.LocalDate td = today.plusDays(2);
            if (tryParseTimeRangeForDate(s, td, r)) return r;
            if (tryParseSingleTimeForDate(s, td, r)) return r;
            r.start = td.atStartOfDay();
            r.end = td.atTime(23, 59, 59);
            r.success = true;
            r.allDay = true;
            return r;
        }

        // 单个具体日期 5月14日
        Pattern monthDay = Pattern.compile("(\\d{1,2})月(\\d{1,2})[日号]?");
        m = monthDay.matcher(s);
        if (m.find()) {
            int sm = Integer.parseInt(m.group(1));
            int sd = Integer.parseInt(m.group(2));
            java.time.LocalDate date;
            try {
                date = java.time.LocalDate.of(today.getYear(), sm, sd);
                if (date.isBefore(today)) date = date.plusYears(1);
            } catch (Exception ex) {
                return r;
            }
            if (tryParseTimeRangeForDate(s, date, r)) return r;
            if (tryParseSingleTimeForDate(s, date, r)) return r;
            r.start = date.atStartOfDay();
            r.end = date.atTime(23, 59, 59);
            r.success = true;
            r.allDay = true;
            return r;
        }

        // 最后尝试单纯的时间范围（无日期），假定今天
        if (tryParseTimeRangeForDate(s, today, r)) return r;
        if (tryParseSingleTimeForDate(s, today, r)) return r;

        return r;
    }

    private static int dayOffset(String word) {
        switch (word) {
            case "今天":
            case "今日": return 0;
            case "明天":
            case "明日": return 1;
            case "后天": return 2;
            case "大后天": return 3;
            default: return 0;
        }
    }

    private static java.time.LocalDate withDayOfMonthAssumeMonth(java.time.LocalDate base, int day) {
        int year = base.getYear();
        int month = base.getMonthValue();
        try {
            return java.time.LocalDate.of(year, month, day);
        } catch (Exception e) {
            java.time.LocalDate next = base.plusMonths(1);
            try {
                return java.time.LocalDate.of(next.getYear(), next.getMonthValue(), day);
            } catch (Exception ex) {
                return base;
            }
        }
    }

    private static boolean tryParseTimeRangeForDate(String s, java.time.LocalDate date, Result r) {
        Pattern pat = Pattern.compile("(上午|下午|中午|晚上|凌晨)?(\\d{1,2})(点|:|：)(\\d{0,2})?\\s*[到\\-~至]\\s*(上午|下午|中午|晚上|凌晨)?(\\d{1,2})(点|:|：)(\\d{0,2})?");
        Matcher m = pat.matcher(s);
        if (m.find()) {
            String period1 = m.group(1);
            int h1 = Integer.parseInt(m.group(2));
            String min1Str = m.group(4);
            int min1 = (min1Str == null || min1Str.isEmpty()) ? 0 : Integer.parseInt(min1Str);
            String period2 = m.group(5);
            int h2 = Integer.parseInt(m.group(6));
            String min2Str = m.group(8);
            int min2 = (min2Str == null || min2Str.isEmpty()) ? 0 : Integer.parseInt(min2Str);
            h1 = adjustHourForPeriod(h1, period1);
            h2 = adjustHourForPeriod(h2, period2);
            r.start = date.atTime(h1, min1, 0);
            r.end = date.atTime(h2, min2, 0);
            if (r.end.isBefore(r.start)) r.end = r.end.plusDays(1);
            r.success = true;
            r.allDay = false;
            return true;
        }
        return false;
    }

    private static boolean tryParseSingleTimeForDate(String s, java.time.LocalDate date, Result r) {
        Pattern pat = Pattern.compile("(上午|下午|中午|晚上|凌晨)?(\\d{1,2})(点|:|：)(\\d{0,2})?");
        Matcher m = pat.matcher(s);
        if (m.find()) {
            String period = m.group(1);
            int h = Integer.parseInt(m.group(2));
            String minStr = m.group(4);
            int min = (minStr == null || minStr.isEmpty()) ? 0 : Integer.parseInt(minStr);
            h = adjustHourForPeriod(h, period);
            r.start = date.atTime(h, min, 0);
            r.end = r.start.plusHours(1);
            r.success = true;
            r.allDay = false;
            return true;
        }
        return false;
    }

    private static int adjustHourForPeriod(int hour, String period) {
        if (period == null) return hour;
        switch (period) {
            case "下午":
            case "晚上":
                if (hour >= 1 && hour <= 11) hour = hour + 12;
                break;
            case "中午":
                if (hour >= 1 && hour <= 11) hour = hour + 12;
                break;
            case "凌晨":
                // 保持不变
                break;
            case "上午":
                // 保持不变
                break;
        }
        return hour;
    }

}
