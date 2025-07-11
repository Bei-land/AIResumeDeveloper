package com.projects.airesumedeveloper.Service;

import com.alibaba.dashscope.exception.NoApiKeyException;

public interface QianWenService {
    String analyzeResume(String resumeText) throws NoApiKeyException;
}
