package com.projects.airesumedeveloper.Service.Impl;

import com.projects.airesumedeveloper.Service.ResumeParser;
import com.projects.airesumedeveloper.entity.ResumeAnalysis;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class DocxParser implements ResumeParser {
    private static final int MAX_CONTEXT_LINES = 5; // 上下文搜索行数

    @Override
    public ResumeAnalysis parse(InputStream inputStream) throws Exception {
        try (XWPFDocument doc = new XWPFDocument(inputStream)) {
            // 使用链表,按原始顺序存储文档内容行
            List<String> contentLines = new LinkedList<>();

            // 1. 读取文档原始结构（保持段落的原始顺序）
            for (IBodyElement element : doc.getBodyElements()) {
                    XWPFParagraph para = (XWPFParagraph) element;
                    String text = para.getText().trim();
                    if (!text.isEmpty()) {
                        contentLines.add(text);
                    }
            }

            // 2. 智能上下文提取（替代纯正则匹配）
            return extractWithContext(contentLines);
        }
    }

    // 使用上下文感知的提取方法
    private ResumeAnalysis extractWithContext(List<String> lines) {
        ResumeAnalysis analysis = new ResumeAnalysis();

        // 建立文本索引用于后续搜索
        String fullText = String.join("\n", lines);

        // 1. 智能姓名提取（支持多种格式）
        analysis.setName(extractName(lines));

        // 2. 多格式联系方式提取
        analysis.setPhone(extractPhone(fullText, lines));
        analysis.setEmail(extractEmail(fullText, lines));

        // 3. 动态定位章节（无固定标题格式）
        Map<String, String> sections = extractDynamicSections(lines);
        analysis.setWorkExperience(sections.getOrDefault("work", ""));
        analysis.setProjectExperience(sections.getOrDefault("project", ""));
        analysis.setEducation(sections.getOrDefault("education", ""));

        // 4. 智能技能提取（NLP增强）
        analysis.setSkills(extractSkills(fullText));

        return analysis;
    }

    // 智能姓名提取（多种匹配策略）
    private String extractName(List<String> lines) {
        // 策略1：标准格式匹配（姓名标签+中文名）
        Pattern namePattern = Pattern.compile("(?:姓名|名字|个人简介)[:：\\s]*(\\p{IsHan}{2,4})");
        for (String line : lines) {
            Matcher matcher = namePattern.matcher(line);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }

        // 策略2：文档标题位置定位（前3行中的有效中文名）
        for (int i = 0; i < Math.min(3, lines.size()); i++) {
            String name = lines.get(i).trim();
            // 过滤特殊字符并验证纯中文
            if (name.replaceAll("[^\\p{IsHan}]", "").length() >= 2 &&
                    name.length() <= 4 && !name.contains("简历") && !name.contains("求职")) {
                return name;
            }
        }

        return "未知";
    }

    // 多格式电话号码提取
    private String extractPhone(String fullText, List<String> lines) {
        // 多格式正则匹配（包含空格/分隔符/区号）
        Pattern phonePattern = Pattern.compile(
                "(?:(?:电话|手机|联系方式)[:：\\s]*)" + // 可选标签
                        "((?:(?:\\+|00)?86[-\\s]?)?" +     // 国际前缀
                        "1[3-9]\\d{1}[-\\s]?\\d{4}[-\\s]?\\d{4})" // 核心号码
        );

        Matcher matcher = phonePattern.matcher(fullText);
        if (matcher.find()) {
            // 标准化输出：去除非数字字符
            return matcher.group(1).replaceAll("[^\\d]", "");
        }

        // 尝试纯数字提取（避免格式干扰）
        Pattern pureDigits = Pattern.compile("(?:\\D|^)(1[3-9]\\d{9})(?:\\D|$)");
        matcher = pureDigits.matcher(fullText);
        if (matcher.find()) {
            return matcher.group(1);
        }

        return "未提供";
    }

    // 宽松邮箱提取
    private String extractEmail(String fullText, List<String> lines) {
        // 增强版邮箱正则（包含中文边界处理）
        Pattern emailPattern = Pattern.compile(
                "(?:\\s|[^\\w@.])" +   // 前导边界
                        "([\\w.+\\-]+@[\\w.\\-]+\\.[a-z]{2,})" + // 核心邮箱
                        "(?:\\s|[^\\w@.])"    // 后缀边界
        );

        Matcher matcher = emailPattern.matcher(fullText.toLowerCase());
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "未提供";
    }

    // 动态章节定位（不依赖固定标题）
    public Map<String, String> extractDynamicSections(List<String> lines) {
        // 创建存储章节的容器
        Map<String, String> sections = new HashMap<>();

        // 章节标识符和匹配规则
        String[] sectionKeys = {"work", "project", "education"};
        String[][] sectionMarkers = {
                {"工作经历", "工作经验", "工作履历"},
                {"项目经验", "项目经历", "研发项目", "项目描述"},
                {"教育背景", "教育经历", "学历", "教育情况", "毕业院校"}
        };

        // 当前处理状态
        StringBuilder currentSection = new StringBuilder();
        String currentKey = "header";
        int lastProcessedIndex = -1;

        // 创建标题检测函数 (Lambda表达式)
        Predicate<String> isSectionHeader = line -> {
            for (String[] markers : sectionMarkers) {
                for (String marker : markers) {
                    if (line.contains(marker)) {
                        return true;
                    }
                }
            }
            return false;
        };

        // 主处理循环
        for (int i = 0; i < lines.size(); i++) {
            // 跳过已处理的行
            if (i <= lastProcessedIndex) continue;

            String line = lines.get(i);
            boolean isHeader = false;

            // 检查是否是章节标题
            for (int j = 0; j < sectionKeys.length; j++) {
                for (String marker : sectionMarkers[j]) {
                    if (line.contains(marker)) {
                        // 保存前一个章节
                        if (!currentSection.isEmpty()) {
                            sections.put(currentKey, currentSection.toString().trim());
                            currentSection = new StringBuilder();
                        }

                        // 设置新章节
                        currentKey = sectionKeys[j];
                        isHeader = true;

                        // 优化的预读机制
                        int contextAdded = 0;
                        for (int k = i + 1;
                             k < lines.size() && contextAdded < MAX_CONTEXT_LINES;
                             k++) {

                            String contextLine = lines.get(k);

                            // 核心保护：检测标题时停止预读
                            if (isSectionHeader.test(contextLine)) {
                                break; // 发现新标题，立即停止
                            }

                            // 只添加非空行
                            if (!contextLine.trim().isEmpty()) {
                                currentSection.append(contextLine).append("\n");
                                contextAdded++;
                                lastProcessedIndex = k;
                            }
                        }
                        break;
                    }
                }
                if (isHeader) break;
            }

            // 普通行处理
            if (!isHeader) {
                // 只添加非空行或保留格式
                currentSection.append(line).append("\n");
            }
        }

        // 添加最后一个章节
        if (!currentSection.isEmpty()) {
            sections.put(currentKey, currentSection.toString().trim());
        }

        // ===== 特殊格式优化 =====
        // 1. 项目经验美化：添加分隔符
        if (sections.containsKey("project")) {
            String projectContent = sections.get("project");
            if (projectContent.contains("项目名称：")) {
                projectContent = Arrays.stream(projectContent.split("项目名称："))
                        .filter(p -> !p.isEmpty())
                        .map(p -> "项目名称：" + p.trim())
                        .collect(Collectors.joining("\n\n"));
                sections.put("project", projectContent);
            }
        }

        // 2. 教育背景清理：移除证书信息
        if (sections.containsKey("education")) {
            String eduContent = sections.get("education");
            // 处理多种证书描述形式
            String[] certificateMarkers = {"\n证书荣誉", "\n证书：", "\n荣誉："};
            for (String marker : certificateMarkers) {
                if (eduContent.contains(marker)) {
                    eduContent = eduContent.substring(0, eduContent.indexOf(marker));
                }
            }
            sections.put("education", eduContent.trim());
        }

        return sections;
    }

    // NLP增强型技能提取
    private List<String> extractSkills(String text) {
        // 多词根技能词典（包含同义词映射）
        Map<String, Set<String>> skillMapping = new HashMap<>();
        skillMapping.put("Java", Set.of("java", "j2ee", "jdk"));
        skillMapping.put("Spring Boot", Set.of("springboot", "spring boot", "sb"));
        skillMapping.put("Spring Cloud", Set.of("spring cloud", "springcloud"));
        skillMapping.put("MySQL", Set.of("mysql", "mariadb"));
        skillMapping.put("Redis", Set.of("redis"));
        skillMapping.put("RabbitMQ", Set.of("rabbitmq"));
        skillMapping.put("Kafka", Set.of("kafka"));
        skillMapping.put("Docker", Set.of("docker"));
        skillMapping.put("Git", Set.of("git"));
        // 添加更多技能映射...

        // 技术术语自动关联
        Map<String, String> techAssociations = Map.of(
                "微服务", "Spring Cloud",
                "分布式", "Dubbo",
                "ci/cd", "Jenkins",
                "对象存储", "OSS"
        );

        List<String> foundSkills = new ArrayList<>();
        String lowerText = text.toLowerCase();

        // 1. 直接匹配技能
        for (Map.Entry<String, Set<String>> entry : skillMapping.entrySet()) {
            for (String alias : entry.getValue()) {
                if (lowerText.contains(alias)) {
                    foundSkills.add(entry.getKey());
                    break;
                }
            }
        }

        // 2. 技术术语关联
        for (Map.Entry<String, String> association : techAssociations.entrySet()) {
            if (lowerText.contains(association.getKey().toLowerCase()) &&
                    !foundSkills.contains(association.getValue())) {
                foundSkills.add(association.getValue());
            }
        }

        // 3. 相邻技能识别（如"Java/Spring Boot/SQL"）
        Pattern skillPattern = Pattern.compile("([A-Za-z0-9+#]+(?:\\s*[/、]\\s*[A-Za-z0-9+#]+)+)");
        Matcher matcher = skillPattern.matcher(text);
        while (matcher.find()) {
            String[] skills = matcher.group(1).split("\\s*[/、]\\s*");
            for (String skill : skills) {
                if (!skill.trim().isEmpty() && !foundSkills.contains(skill)) {
                    foundSkills.add(skill);
                }
            }
        }

        // 去重并排序（按自然顺序）
        return foundSkills.stream()
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }
}