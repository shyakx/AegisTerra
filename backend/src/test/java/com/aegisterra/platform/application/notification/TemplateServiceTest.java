package com.aegisterra.platform.application.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import org.junit.jupiter.api.Test;

class TemplateServiceTest {

    @Test
    void replacesPlaceholders() {
        String rendered = TemplateService.replace(
            "Hello {{name}} — task {{taskId}}",
            Map.of("name", "Ada", "taskId", "abc")
        );
        assertEquals("Hello Ada — task abc", rendered);
        assertEquals("xy", TemplateService.replace("x{{missing}}y", Map.of("missing", "")));
    }
}
