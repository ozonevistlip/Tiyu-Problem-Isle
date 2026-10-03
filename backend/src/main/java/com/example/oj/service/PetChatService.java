package com.example.oj.service;

import com.example.oj.common.BusinessException;
import com.example.oj.common.ErrorCode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PetChatService {
    private final ObjectMapper objectMapper;
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();

    @Value("${oj.deepseek.api-key:}")
    private String apiKey;
    @Value("${oj.deepseek.base-url:https://api.deepseek.com}")
    private String baseUrl;
    @Value("${oj.deepseek.model:deepseek-flash}")
    private String model;

    public String reply(String prompt, List<ChatMessage> history) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "宠物聊天尚未配置 DeepSeek API Key");
        }
        if (history == null || history.isEmpty() || history.size() > 20
                || !"user".equals(history.get(history.size() - 1).role())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "聊天记录需包含最近 1–20 条消息，最后一条须由用户发送");
        }
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content",
                "你正在与少儿编程学习网站的学生交流。回答要友善、简短、适龄；不要索取个人信息；编程题先引导思考。\n宠物性格设定：" + prompt));
        for (ChatMessage message : history) {
            if (message == null || !("user".equals(message.role()) || "assistant".equals(message.role()))
                    || message.content() == null || message.content().isBlank()
                    || message.content().length() > 2000) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "聊天消息格式或长度无效");
            }
            messages.add(Map.of("role", message.role(), "content", message.content()));
        }
        try {
            String body = objectMapper.writeValueAsString(Map.of(
                    "model", model, "messages", messages, "stream", false, "max_tokens", 400));
            HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl.replaceAll("/+$", "") + "/chat/completions"))
                    .timeout(Duration.ofSeconds(45))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body)).build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "宠物暂时无法回复，请稍后重试");
            }
            JsonNode content = objectMapper.readTree(response.body()).path("choices").path(0)
                    .path("message").path("content");
            if (!content.isTextual() || content.asText().isBlank()) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "宠物暂时无法回复，请稍后重试");
            }
            return content.asText();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            // Do not expose upstream responses or request bodies: they may contain children's chat messages.
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "宠物暂时无法回复，请稍后重试");
        }
    }

    public record ChatMessage(String role, String content) {}
}
