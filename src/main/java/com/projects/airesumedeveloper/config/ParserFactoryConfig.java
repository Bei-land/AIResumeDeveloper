package com.projects.airesumedeveloper.config;

import com.projects.airesumedeveloper.Service.Impl.DocxParser;
import com.projects.airesumedeveloper.Service.Impl.PdfParser;
import com.projects.airesumedeveloper.Service.ResumeParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ParserFactoryConfig {

//    该方法定义了一个Spring Bean
//    用于创建并返回一个ParserFactory实例
//    它通过@Autowired注入了PdfParser和DocxParser两个服务类
//    作为构造参数传入到ParserFactory的构造函数中
//    实现解析器工厂的依赖注入配置
    @Bean
    public ParserFactory parserFactory(@Autowired PdfParser pdfParser, @Autowired DocxParser docxParser) {
        return new ParserFactory(pdfParser, docxParser);
    }


    public static class ParserFactory {

        //定义了两个私有的、不可变的解析器对象
        //这两个对象在初始化后不能被修改（由final修饰），通常会在构造函数中被赋值。
        private final PdfParser pdfParser;
        private final DocxParser docxParser;

        //构造函数
        //接收两个参数pdfParser和docxParser，并分别赋值给两个成员变量pdfParser和docxParser。
        public ParserFactory(PdfParser pdfParser, DocxParser docxParser) {
            this.pdfParser = pdfParser;
            this.docxParser = docxParser;
        }

        public ResumeParser getParser(String fileName) {
            if (fileName.toLowerCase().endsWith(".pdf")) {
                return pdfParser;
            } else if (fileName.toLowerCase().endsWith(".docx")) {
                return docxParser;
            }
            throw new UnsupportedOperationException("不支持的文件格式: " + fileName);
        }
    }
}