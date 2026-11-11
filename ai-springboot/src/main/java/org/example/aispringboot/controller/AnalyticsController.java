package org.example.aispringboot.controller;

import org.example.aispringboot.DTO.response.AnalyticsOverviewDTO;
import org.example.aispringboot.common.Result;
import org.example.aispringboot.service.AnalyticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 数据分析Controller
 */
@RestController
@RequestMapping("/api/data-analytics")
public class AnalyticsController {
    @Autowired
    private AnalyticsService analyticsService;

    // 看板总览数据
    @GetMapping("/overview")
    public Result<AnalyticsOverviewDTO> overview() {
        return Result.ok(analyticsService.overview());
    }
}
