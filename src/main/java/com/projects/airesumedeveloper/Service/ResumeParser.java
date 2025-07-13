package com.projects.airesumedeveloper.Service;

import com.projects.airesumedeveloper.entity.ResumeAnalysis;

import java.io.InputStream;

public interface ResumeParser {
    ResumeAnalysis parse(InputStream inputStream) throws Exception;
}
