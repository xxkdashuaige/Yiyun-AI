package com.yiyun.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * AI 对话请求参数
 */
@Data
public class AiChatRequest {

    /** 用户输入的问题（必填） */
    @NotBlank(message = "问题不能为空")
    private String message;

    /** 指定非遗项目 ID（可选，用于限定检索范围） */
    private Long heritageProjectId;

    /** 对话历史消息列表，每条 {"role": "user"|"assistant", "content": "..."}（可选） */
    private List<Map<String, String>> conversationHistory;
}