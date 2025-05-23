package com.factoreal.backend.global.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;


public class IdGenerator {
    private static final Random RANDOM = new Random();
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    public static String generateId() {
        String now = LocalDateTime.now().format(FORMATTER);
        int randomSuffix = RANDOM.nextInt(900) + 100; // 100~999
        return now + "-" + randomSuffix;
    }
}
