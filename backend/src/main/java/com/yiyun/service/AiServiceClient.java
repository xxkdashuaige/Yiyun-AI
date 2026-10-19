package com.yiyun.service;

import com.yiyun.dto.AiChatRequest;
import com.yiyun.dto.AiChatResponse;
import com.yiyun.dto.SourceItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
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
 */
@Service
public class AiServiceClient {

    private static final Logger log = LoggerFactory.getLogger(AiServiceClient.class);

    private final RestTemplate restTemplate;

    @Value("${ai-service.base-url}")
    private String baseUrl;

    public AiServiceClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        // 确保支持 UTF-8 文本响应
        restTemplate.getMessageConverters().add(0, new StringHttpMessageConverter(StandardCharsets.UTF_8));
    }

    /**
     * 调用 ai-service 的 chat 接口进行文本生成。
     *
     * @param request 用户对话请求
     * @return AI 回答及检索来源
     */
    @SuppressWarnings("unchecked")
    public AiChatResponse chat(AiChatRequest request) {
        String url = baseUrl + "/api/ai/chat";
        Map<String, Object> body = new HashMap<>();
        body.put("message", request.getMessage());
        if (request.getHeritageProjectId() != null) {
            body.put("heritage_project_id", request.getHeritageProjectId());
        }
        if (request.getConversationHistory() != null) {
            body.put("conversation_history", request.getConversationHistory());
        }

        Map<String, Object> response = restTemplate.postForObject(url, body, Map.class);
        if (response == null) {
            throw new RuntimeException("ai-service 响应为空");
        }

        AiChatResponse result = new AiChatResponse();
        result.setReply((String) response.get("reply"));

        List<SourceItem> sources = new ArrayList<>();
        Object sourcesObj = response.get("sources");
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
        result.setSources(sources);
        return result;
    }

    /**
     * 流式调用 ai-service 的 chat/stream 接口，将 SSE 事件转发给前端 SseEmitter。
     *
     * @param request       对话请求
     * @param emitter       前端 SSE 发射器
     * @param onReplyReady  回复文本完整接收后回调（参数为完整回复文本 + 来源列表）
     */
    public void chatStream(AiChatRequest request, SseEmitter emitter,
                           Consumer<String> onReplyReady,
                           Consumer<List<SourceItem>> onSourcesReady) {
        String url = baseUrl + "/api/ai/chat/stream";
        Map<String, Object> body = new HashMap<>();
        body.put("message", request.getMessage());
        if (request.getHeritageProjectId() != null) {
            body.put("heritage_project_id", request.getHeritageProjectId());
        }
        if (request.getConversationHistory() != null) {
            body.put("conversation_history", request.getConversationHistory());
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        StringBuilder replyBuilder = new StringBuilder();
        List<SourceItem> sourcesHolder = new ArrayList<>();

        try {
            restTemplate.execute(url, org.springframework.http.HttpMethod.POST,
                    req -> {
                        req.getHeaders().setContentType(MediaType.APPLICATION_JSON);
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
                                // 转发原始 JSON 数据给前端
                                emitter.send(SseEmitter.event().data(data));
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
}