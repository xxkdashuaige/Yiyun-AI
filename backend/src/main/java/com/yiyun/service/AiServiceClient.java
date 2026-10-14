package com.yiyun.service;

import com.yiyun.dto.AiChatRequest;
import com.yiyun.dto.AiChatResponse;
import com.yiyun.dto.SourceItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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