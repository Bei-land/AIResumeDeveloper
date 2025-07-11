package com.projects.airesumedeveloper.Service.Impl;

import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.aigc.generation.models.QwenParam;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.projects.airesumedeveloper.Service.QianWenService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class QianWenServiceImpl implements QianWenService {
    @Value("${qianwen.api-key}")
    private String apiKey;  // 从配置读取密钥

    public String analyzeResume(String resumeText) throws NoApiKeyException {
        Generation gen = new Generation();
        QwenParam param = QwenParam.builder()
                .model(Generation.Models.QWEN_TURBO)
                .apiKey(apiKey)
                .prompt("你是一名资深HR，请为以下简历提供优化建议：\n" + resumeText)
                .topP(0.8)
                .resultFormat(QwenParam.ResultFormat.TEXT)
                .build();

        GenerationResult result = null;
        try {
            result = gen.call(param);
        } catch (InputRequiredException e) {
            throw new RuntimeException(e);
        }
        return result.getOutput().getText();
    }
}