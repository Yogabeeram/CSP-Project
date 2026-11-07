package com.college.questionpapergenerator.controller;

import com.college.questionpapergenerator.dto.ExcelUploadReport;
import com.college.questionpapergenerator.service.ExcelUploadService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/questions/upload")
public class UploadController {

    private static final MediaType XLSX_MEDIA_TYPE =
            MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final ExcelUploadService excelUploadService;

    public UploadController(ExcelUploadService excelUploadService) {
        this.excelUploadService = excelUploadService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ExcelUploadReport upload(@RequestParam("file") MultipartFile file) {
        return excelUploadService.upload(file);
    }

    @GetMapping("/template")
    public ResponseEntity<byte[]> downloadTemplate() {
        byte[] template = excelUploadService.createTemplate();
        return ResponseEntity.ok()
                .contentType(XLSX_MEDIA_TYPE)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("question-bank-template.xlsx").build().toString())
                .body(template);
    }
}
