package com.projects.airesumedeveloper.controller;

import com.projects.airesumedeveloper.Service.QianWenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/resume")
public class ResumeController {
    @Autowired
    private QianWenService qianWenService;

    @Autowired
    public ResumeController(QianWenService qianWenService) {
        this.qianWenService = qianWenService;
    }

    @PostMapping("/analyze")
    public String analyzeResume(@RequestBody String rawResume) {
        if (rawResume == null || rawResume.isEmpty()){
            return "请上传简历";
        }

        try {
            return qianWenService.analyzeResume(rawResume);
        } catch (Exception e) {
            return "服务暂不可用：请检查API密钥或网络连接";
        }
    }
}