package com.projects.airesumedeveloper.controller;

import com.projects.airesumedeveloper.Service.Impl.DocxParser;
import com.projects.airesumedeveloper.Service.Impl.PdfParser;
import com.projects.airesumedeveloper.Service.QianWenService;
import com.projects.airesumedeveloper.Service.ResumeParser;
import com.projects.airesumedeveloper.config.ParserFactoryConfig;
import com.projects.airesumedeveloper.entity.ResumeAnalysis;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/resume")
public class ResumeController {
    private static final Logger logger = LoggerFactory.getLogger(ResumeController.class);

    private final QianWenService qianWenService;
    private final ParserFactoryConfig.ParserFactory parserFactory; // 注入ParserFactory

    @Autowired
    public ResumeController(QianWenService qianWenService, ParserFactoryConfig.ParserFactory parserFactory) {
        this.qianWenService = qianWenService;
        this.parserFactory = parserFactory; // 注入ParserFactory
    }

    @PostMapping("/analyze")
    public String analyzeResume(@RequestBody String rawResume) {
        if (rawResume == null || rawResume.isEmpty()){
            return "请上传简历";
        }

        try {
            return qianWenService.analyzeResume(rawResume);
        } catch (Exception e) {
            logger.error("简历分析失败: {}", e.getMessage());
            return "服务暂不可用：请检查API密钥或网络连接";
        }
    }

    @PostMapping("/upload")
    public ResponseEntity<ResumeAnalysis> uploadResume(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            logger.warn("上传空文件");
            return ResponseEntity.badRequest().body(null);
        }

        try {
            // 使用注入的ParserFactory获取解析器
            ResumeParser parser = parserFactory.getParser(file.getOriginalFilename());
            ResumeAnalysis analysis = parser.parse(file.getInputStream());
            logger.info("成功解析简历: {}", file.getOriginalFilename());
            return ResponseEntity.ok(analysis);
        } catch (UnsupportedOperationException e) {
            logger.warn("不支持的文件格式: {}", file.getOriginalFilename());
            return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(null);
        } catch (Exception e) {
            logger.error("解析简历出错: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(null);
        }
    }
}