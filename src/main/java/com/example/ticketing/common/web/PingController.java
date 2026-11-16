package com.example.ticketing.common.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 阶段 0 冒烟接口：证明应用起来了、能响应 HTTP。
 * 后面每个业务域会有自己的 Controller，这个最终会删掉。
 */
@RestController
public class PingController {

    @GetMapping("/ping")
    public Map<String, String> ping() {
        return Map.of(
                "status", "ok",
                "service", "ticketing"
        );
    }
}
