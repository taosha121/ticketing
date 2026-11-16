package com.example.ticketing;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 阶段 0 占位测试：不加载 Spring 上下文，所以 `mvn test` 不需要数据库在线。
 * 真正的集成测试（用 Testcontainers 拉真 PG）放到阶段 6。
 */
class SanityTest {

    @Test
    void onePlusOne() {
        assertEquals(2, 1 + 1);
    }
}
