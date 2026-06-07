package com.example.xapi.api;

import com.example.xapi.dto.TranslateRequest;
import com.example.xapi.dto.TranslateResponse;
import com.example.xapi.service.TranslationService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/x")
public class TranslationController {
    private final TranslationService translationService;

    public TranslationController(TranslationService translationService) {
        this.translationService = translationService;
    }

    @PostMapping("/translate")
    public TranslateResponse translate(@RequestBody TranslateRequest request) {
        return translationService.translate(request);
    }
}
