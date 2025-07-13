package com.projects.airesumedeveloper.entity;

import lombok.Data;

import java.util.List;

@Data
// 简历分析结果
public class ResumeAnalysis {
    private String name;
    private String phone;
    private String email;
    private String workExperience;
    private String projectExperience;
    private List<String> skills;
    private String education;

    // 构造方法/getter/setter
}