package com.factoreal.backend.global.fileUtil;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Deprecated
//@Component
// 추후 PDF 사용성 논의 예정
public class Html2PdfUtil {

    public Path captureReportPageToPdf(String relativeUrl) throws IOException, InterruptedException {
        // ex) http://localhost:8080/report/detail?zone=Z1
//        String url = "http://localhost:8080" + relativeUrl;

        Path pdf = Files.createTempFile("detail-", ".pdf");

        Process proc = new ProcessBuilder("wkhtmltopdf", relativeUrl, pdf.toString())
                           .inheritIO()
                           .start();
        int exit = proc.waitFor();
        if (exit != 0) throw new IllegalStateException("wkhtmltopdf 실패");

        return pdf;
    }
}