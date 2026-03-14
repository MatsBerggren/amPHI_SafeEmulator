package com.dedalus.amphi_integration.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LogAnalysisPageController {

    @GetMapping("/log-analyzer")
    public String index() {
        return "redirect:/log-analyzer.html";
    }
}