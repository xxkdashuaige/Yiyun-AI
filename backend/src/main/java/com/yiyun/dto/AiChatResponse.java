package com.yiyun.dto;

import lombok.Data;

import java.util.List;

/**
 * AI 对话响应
 */
@Data
public class AiChatResponse {

    /** AI 生成的回答 */
    private String reply;

    /** RAG 检索到的知识来源列表 */
    private List<SourceItem> sources;

    /** 任务 ID（用于追踪和作品关联） */
    private Long taskId;

    /** 会话 ID（新建或续接的会话） */
    private Long sessionId;
}