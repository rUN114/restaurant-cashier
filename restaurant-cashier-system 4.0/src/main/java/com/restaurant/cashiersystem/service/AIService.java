package com.restaurant.cashiersystem.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.LocalDate;
import java.util.*;

@Service
public class AIService {
    private static final Logger log = LoggerFactory.getLogger(AIService.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    @Value("${deepseek.api.key:}")
    private String apiKey;

    @Value("${deepseek.api.url:https://api.deepseek.com/chat/completions}")
    private String apiUrl;

    private final JdbcTemplate jdbcTemplate;
    private final RestTemplate restTemplate;

    public AIService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.restTemplate = new RestTemplate();
    }

    /** 生成明日预制建议 */
    public List<Map<String, Object>> suggest(Long storeId) {
        if (apiKey == null || apiKey.isEmpty() || apiKey.contains("your-key")) {
            log.warn("DeepSeek API key not configured, using fallback");
            return fallbackSuggestion(storeId);
        }

        try {
            Map<String, Object> data = gatherSalesData(storeId);
            String prompt = buildPrompt(data);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", "deepseek-chat");
            body.put("temperature", 0.3);
            body.put("max_tokens", 1000);
            body.put("messages", List.of(
                Map.of("role", "system", "content", "你是餐厅数据分析师，根据历史销售数据给出明日预制建议。返回JSON格式，不要额外文字。"),
                Map.of("role", "user", "content", prompt)
            ));

            ResponseEntity<String> resp = restTemplate.postForEntity(
                apiUrl, new HttpEntity<>(body, headers), String.class);

            if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                return parseResponse(resp.getBody());
            }
        } catch (Exception e) {
            log.error("AI 建议请求失败", e);
        }
        return fallbackSuggestion(storeId);
    }

    /** 收集30天销售数据 */
    private Map<String, Object> gatherSalesData(Long storeId) {
        String sf = storeId != null ? " AND o.store_id = " + storeId : "";

        // 明天是周几
        String tomorrow = LocalDate.now().plusDays(1).getDayOfWeek().toString();

        // 近30天每日销量TOP10菜品
        String sql = "SELECT d.name, DATE(o.settled_at) AS day, SUM(oi.quantity) AS qty " +
            "FROM order_items oi JOIN orders o ON oi.order_id = o.id " +
            "JOIN dishes d ON oi.dish_id = d.id " +
            "WHERE o.status = 'settled' AND o.settled_at >= DATE_SUB(CURDATE(), INTERVAL 30 DAY)" + sf +
            " GROUP BY d.name, DATE(o.settled_at) ORDER BY day DESC, qty DESC";

        List<Map<String, Object>> sales = jdbcTemplate.queryForList(sql);

        // 近7天每日总营业额
        String revSql = "SELECT DATE(settled_at) AS day, SUM(final_amount) AS revenue, COUNT(*) AS orders " +
            "FROM orders WHERE status = 'settled' AND settled_at >= DATE_SUB(CURDATE(), INTERVAL 7 DAY)" + sf +
            " GROUP BY DATE(settled_at) ORDER BY day";

        List<Map<String, Object>> revenue = jdbcTemplate.queryForList(revSql);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("tomorrow", tomorrow);
        data.put("sales30d", sales);
        data.put("revenue7d", revenue);
        return data;
    }

    /** 构造提示词 */
    private String buildPrompt(Map<String, Object> data) {
        String tomorrow = (String) data.get("tomorrow");
        return String.format(
            "明天是%s。以下是餐厅近30天销售数据和近7天营业额。请分析并给出明日建议：\n" +
            "1. 建议预制哪些菜品，多少份\n" +
            "2. 预计明日总营业额\n" +
            "3. 备货提醒（哪些原料可能不够）\n\n" +
            "数据：%s\n\n" +
            "只返回JSON：{\"suggestions\":[{\"dish\":\"菜名\",\"quantity\":份数,\"reason\":\"原因\"}],\"forecast\":\"预计营业额\",\"alerts\":[\"提醒1\"]}",
            tomorrow, safeJson(data));
    }

    /** 解析AI返回：DeepSeek包装 → 提取 content → 解析JSON */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> parseResponse(String body) {
        try {
            // 第1层：DeepSeek 响应 {"choices":[{"message":{"content":"..."}}]}
            Map<String, Object> outer = mapper.readValue(body, Map.class);
            List<Map<String, Object>> choices = (List<Map<String, Object>>) outer.get("choices");
            if (choices == null || choices.isEmpty()) {
                return List.of(Map.of("raw", body));
            }
            Object msgObj = choices.get(0).get("message");
            String content = msgObj instanceof Map ? (String) ((Map<?,?>) msgObj).get("content") : null;
            if (content == null) {
                return List.of(Map.of("raw", body));
            }

            // 第2层：AI返回的JSON（可能包裹在markdown中）
            int start = content.indexOf('{');
            int end = content.lastIndexOf('}');
            if (start >= 0 && end > start) content = content.substring(start, end + 1);

            Map<String, Object> parsed = mapper.readValue(content, Map.class);
            List<Map<String, Object>> result = new ArrayList<>();
            result.add(parsed);
            return result;
        } catch (Exception e) {
            log.warn("解析AI响应失败: {}", body.substring(0, Math.min(200, body.length())));
            return List.of(Map.of("raw", body));
        }
    }

    /** 未配置AI时的兜底建议（基于近7天平均） */
    private List<Map<String, Object>> fallbackSuggestion(Long storeId) {
        String sf = storeId != null ? " AND o.store_id = " + storeId : "";
        String sql = "SELECT d.name, ROUND(AVG(daily.qty), 0) AS suggest FROM (" +
            " SELECT oi.dish_id, DATE(o.settled_at) AS day, SUM(oi.quantity) AS qty " +
            " FROM order_items oi JOIN orders o ON oi.order_id = o.id " +
            " WHERE o.status = 'settled' AND o.settled_at >= DATE_SUB(CURDATE(), INTERVAL 7 DAY)" + sf +
            " GROUP BY oi.dish_id, DATE(o.settled_at)" +
            ") daily JOIN dishes d ON d.id = daily.dish_id " +
            " GROUP BY d.name ORDER BY suggest DESC LIMIT 10";

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
        Map<String, Object> fallback = new LinkedHashMap<>();
        fallback.put("source", "近7天日均销量");
        List<Map<String, Object>> suggestions = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> s = new LinkedHashMap<>();
            s.put("dish", row.get("name"));
            s.put("quantity", ((Number) row.get("suggest")).intValue());
            s.put("reason", "近7天日均");
            suggestions.add(s);
        }
        fallback.put("suggestions", suggestions);
        return List.of(fallback);
    }

    private String safeJson(Object obj) {
        try { return mapper.writeValueAsString(obj); }
        catch (Exception e) { return "{}"; }
    }
}
