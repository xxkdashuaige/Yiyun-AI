package com.yiyun.service;

import com.yiyun.dto.AiChatRequest;
import com.yiyun.dto.AiChatResponse;
import com.yiyun.dto.SourceItem;
import com.yiyun.util.SecurityUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * AI 服务 HTTP 客户端，封装对 ai-service 的调用。
 * <p>
 * 所有调用都会自动携带当前用户的 JWT Token，供 ai-service 做身份认证。
 */
@Service
public class AiServiceClient {

    private static final Logger log = LoggerFactory.getLogger(AiServiceClient.class);

    private final RestTemplate restTemplate;

    @Value("${ai-service.base-url}")
    private String baseUrl;

    public AiServiceClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        restTemplate.getMessageConverters().add(0, new StringHttpMessageConverter(StandardCharsets.UTF_8));
    }

    /**
     * 构建带 JWT 的请求头
     */
    private HttpHeaders buildAuthHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String token = SecurityUtil.getCurrentToken();
        if (token != null) {
            headers.setBearerAuth(token);
        }
        return headers;
    }

    /**
     * 调用 ai-service 的 chat 接口进行文本生成。
     */
    @SuppressWarnings("unchecked")
    public AiChatResponse chat(AiChatRequest request) {
        String url = baseUrl + "/api/ai/chat";
        Map<String, Object> body = buildChatBody(request);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, buildAuthHeaders());
        log.debug("调用 ai-service chat: url={}", url);
        long start = System.currentTimeMillis();
        Map<String, Object> response = restTemplate.postForObject(url, entity, Map.class);
        log.debug("ai-service chat 响应耗时: {}ms", System.currentTimeMillis() - start);
        if (response == null) {
            throw new RuntimeException("ai-service 响应为空");
        }

        AiChatResponse result = new AiChatResponse();
        result.setReply((String) response.get("reply"));
        result.setSessionId(toLong(response.get("session_id")));

        List<SourceItem> sources = parseSources(response.get("sources"));
        result.setSources(sources);
        return result;
    }

    /**
     * 流式调用 ai-service 的 chat/stream 接口，将 SSE 事件转发给前端 SseEmitter。
     *
     * @param token 当前用户的 JWT Token（由调用方在请求线程中提取，避免异步线程丢失上下文）
     */
    public void chatStream(AiChatRequest request, SseEmitter emitter, String token,
                           Consumer<String> onReplyReady,
                           Consumer<List<SourceItem>> onSourcesReady) {
        String url = baseUrl + "/api/ai/chat/stream";
        Map<String, Object> body = buildChatBody(request);

        StringBuilder replyBuilder = new StringBuilder();
        List<SourceItem> sourcesHolder = new ArrayList<>();

        try {
            restTemplate.execute(url, HttpMethod.POST,
                    req -> {
                        req.getHeaders().setContentType(MediaType.APPLICATION_JSON);
                        if (token != null) {
                            req.getHeaders().setBearerAuth(token);
                        }
                        byte[] bytes = new com.fasterxml.jackson.databind.ObjectMapper()
                                .writeValueAsBytes(body);
                        req.getBody().write(bytes);
                    },
                    response -> {
                        try (InputStream is = response.getBody();
                             BufferedReader reader = new BufferedReader(
                                     new InputStreamReader(is, StandardCharsets.UTF_8))) {
                            String line;
                            while ((line = reader.readLine()) != null) {
                                if (!line.startsWith("data:")) {
                                    continue;
                                }
                                String data = line.substring(5).trim();
                                if (data.isEmpty()) {
                                    continue;
                                }
                                if ("[DONE]".equals(data)) {
                                    emitter.send(SseEmitter.event().data("[DONE]"));
                                    break;
                                }
                                // sources 事件中包含 session_id，统一转为 sessionId 以保持与同步接口一致
                                String forwardData = normalizeSessionField(data);
                                emitter.send(SseEmitter.event().data(forwardData));
                                parseStreamEvent(data, replyBuilder, sourcesHolder);
                            }
                        } catch (Exception e) {
                            log.warn("SSE 流读取中断: {}", e.getMessage());
                        }
                        return null;
                    });
        } catch (Exception e) {
            log.error("流式调用 ai-service 失败: {}", e.getMessage());
            try {
                emitter.send(SseEmitter.event().data("{\"error\":\"" + e.getMessage() + "\"}"));
            } catch (Exception ignored) {
            }
        } finally {
            try {
                emitter.complete();
            } catch (Exception ignored) {
            }
            if (onReplyReady != null) {
                onReplyReady.accept(replyBuilder.toString());
            }
            if (onSourcesReady != null) {
                onSourcesReady.accept(sourcesHolder);
            }
        }
    }

    /**
     * 获取当前用户的会话列表
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> listSessions() {
        String url = baseUrl + "/api/ai/sessions";
        HttpEntity<Void> entity = new HttpEntity<>(buildAuthHeaders());
        List<Map<String, Object>> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, List.class).getBody();
        return response != null ? response : new ArrayList<>();
    }

    /**
     * 获取指定会话的消息列表
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getSessionMessages(Long sessionId) {
        String url = baseUrl + "/api/ai/sessions/" + sessionId + "/messages";
        HttpEntity<Void> entity = new HttpEntity<>(buildAuthHeaders());
        List<Map<String, Object>> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, List.class).getBody();
        return response != null ? response : new ArrayList<>();
    }

    /**
     * 删除会话（软删除）
     */
    public void deleteSession(Long sessionId) {
        String url = baseUrl + "/api/ai/sessions/" + sessionId;
        HttpEntity<Void> entity = new HttpEntity<>(buildAuthHeaders());
        restTemplate.exchange(url, HttpMethod.DELETE, entity, Void.class);
    }

    // ==================== 私有方法 ====================

    private Map<String, Object> buildChatBody(AiChatRequest request) {
        Map<String, Object> body = new HashMap<>();
        body.put("message", request.getMessage());
        if (request.getHeritageProjectId() != null) {
            body.put("heritage_project_id", request.getHeritageProjectId());
        }
        if (request.getSessionId() != null) {
            body.put("session_id", request.getSessionId());
        }
        return body;
    }

    @SuppressWarnings("unchecked")
    private List<SourceItem> parseSources(Object sourcesObj) {
        List<SourceItem> sources = new ArrayList<>();
        if (sourcesObj instanceof List<?> sourceList) {
            for (Object item : sourceList) {
                if (item instanceof Map<?, ?> map) {
                    SourceItem source = new SourceItem();
                    source.setChunkId(toLong(map.get("chunk_id")));
                    source.setDocumentId(toLong(map.get("document_id")));
                    source.setHeritageProjectId(toLong(map.get("heritage_project_id")));
                    source.setTitle((String) map.get("title"));
                    source.setDocType((String) map.get("doc_type"));
                    source.setSource((String) map.get("source"));
                    source.setContent((String) map.get("content"));
                    Object score = map.get("score");
                    if (score != null) {
                        source.setScore(new BigDecimal(score.toString()));
                    }
                    sources.add(source);
                }
            }
        }
        return sources;
    }

    @SuppressWarnings("unchecked")
    private void parseStreamEvent(String json, StringBuilder replyBuilder, List<SourceItem> sourcesHolder) {
        try {
            com.fasterxml.jackson.databind.JsonNode node =
                    new com.fasterxml.jackson.databind.ObjectMapper().readTree(json);
            if (node.has("delta")) {
                replyBuilder.append(node.get("delta").asText());
            } else if (node.has("sources") && node.get("sources").isArray()) {
                for (com.fasterxml.jackson.databind.JsonNode s : node.get("sources")) {
                    SourceItem item = new SourceItem();
                    item.setChunkId(s.has("chunk_id") && !s.get("chunk_id").isNull()
                            ? s.get("chunk_id").asLong() : null);
                    item.setDocumentId(s.has("document_id") && !s.get("document_id").isNull()
                            ? s.get("document_id").asLong() : null);
                    item.setHeritageProjectId(s.has("heritage_project_id") && !s.get("heritage_project_id").isNull()
                            ? s.get("heritage_project_id").asLong() : null);
                    item.setTitle(s.has("title") ? s.get("title").asText() : null);
                    item.setDocType(s.has("doc_type") && !s.get("doc_type").isNull()
                            ? s.get("doc_type").asText() : null);
                    item.setSource(s.has("source") && !s.get("source").isNull()
                            ? s.get("source").asText() : null);
                    item.setContent(s.has("content") ? s.get("content").asText() : null);
                    if (s.has("score") && !s.get("score").isNull()) {
                        item.setScore(new BigDecimal(s.get("score").asText()));
                    }
                    sourcesHolder.add(item);
                }
            }
        } catch (Exception e) {
            log.debug("解析 SSE 事件失败: {}", e.getMessage());
        }
    }

    /**
     * 将 SSE 事件 JSON 中的 session_id 字段统一为 sessionId，保持与同步接口一致。
     * 仅对包含 session_id 的事件做转换，其他事件原样返回。
     */
    private String normalizeSessionField(String json) {
        if (json.indexOf("session_id") < 0) {
            return json;
        }
        try {
            com.fasterxml.jackson.databind.JsonNode node =
                    new com.fasterxml.jackson.databind.ObjectMapper().readTree(json);
            if (node.has("session_id") && !node.get("session_id").isNull()) {
                com.fasterxml.jackson.databind.node.ObjectNode obj =
                        (com.fasterxml.jackson.databind.node.ObjectNode) node;
                long sid = obj.get("session_id").asLong();
                obj.remove("session_id");
                obj.put("sessionId", sid);
                return obj.toString();
            }
        } catch (Exception e) {
            log.debug("normalizeSessionField 解析失败，原样返回: {}", e.getMessage());
        }
        return json;
    }

    private Long toLong(Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof Number num) {
            return num.longValue();
        }
        try {
            return Long.parseLong(obj.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 调用 ai-service 提交图像生成异步任务。
     */
    @SuppressWarnings("unchecked")
    public String submitImageTask(String prompt, String style, String size, Integer count) {
        String url = baseUrl + "/api/ai/generate/image";
        Map<String, Object> body = new HashMap<>();
        body.put("prompt", prompt);
        if (style != null && !style.isBlank()) {
            body.put("style", style);
        }
        if (size != null && !size.isBlank()) {
            body.put("size", size);
        }
        if (count != null && count > 0) {
            body.put("n", count);
        }

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, buildAuthHeaders());
        Map<String, Object> response = restTemplate.postForObject(url, entity, Map.class);
        if (response == null) {
            throw new RuntimeException("ai-service 图像生成响应为空");
        }
        Object taskId = response.get("task_id");
        if (taskId == null) {
            throw new RuntimeException("ai-service 未返回 task_id: " + response);
        }
        return taskId.toString();
    }

    /**
     * 调用 ai-service 查询图像生成任务状态。
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getImageTaskStatus(String providerTaskId) {
        String url = baseUrl + "/api/ai/tasks/" + providerTaskId;
        HttpEntity<Void> entity = new HttpEntity<>(buildAuthHeaders());
        Map<String, Object> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, Map.class).getBody();
        if (response == null) {
            throw new RuntimeException("ai-service 任务查询响应为空");
        }
        return response;
    }
}