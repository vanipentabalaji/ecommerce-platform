package com.ecommerce.order_service.controller;

import com.ecommerce.order_service.service.AiService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;

    @PostMapping("/chat")
    public String chat(@RequestParam String prompt) {
        return aiService.chat(prompt);
    }
}