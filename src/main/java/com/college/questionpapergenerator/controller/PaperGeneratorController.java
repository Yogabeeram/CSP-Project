package com.college.questionpapergenerator.controller;

import com.college.questionpapergenerator.dto.GeneratedPaperResponse;
import com.college.questionpapergenerator.dto.PaperGenerationRequest;
import com.college.questionpapergenerator.service.PdfService;
import com.college.questionpapergenerator.service.PaperGeneratorService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/papers")
public class PaperGeneratorController {

    private final PaperGeneratorService paperGeneratorService;
    private final PdfService pdfService;

    public PaperGeneratorController(PaperGeneratorService paperGeneratorService, PdfService pdfService) {
        this.paperGeneratorService = paperGeneratorService;
        this.pdfService = pdfService;
    }

    @PostMapping("/generate")
    @ResponseStatus(HttpStatus.CREATED)
    public GeneratedPaperResponse generate(@Valid @RequestBody PaperGenerationRequest request) {
        return paperGeneratorService.generate(request);
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> downloadPdf(@PathVariable @Positive Long id) {
        byte[] pdf = pdfService.generatePaperPdf(id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(pdf.length)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename("exam-paper-" + id + ".pdf")
                                .build()
                                .toString())
                .body(pdf);
    }
}
