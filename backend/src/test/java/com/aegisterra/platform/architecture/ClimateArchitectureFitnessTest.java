package com.aegisterra.platform.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class ClimateArchitectureFitnessTest {

    @Test
    void climateIntelligenceDoesNotImportClimateSpi() throws IOException {
        Path root = Path.of("src/main/java/com/aegisterra/platform/application/climateintelligence");
        assertThat(root).exists();
        try (Stream<Path> files = Files.walk(root)) {
            files.filter(p -> p.toString().endsWith(".java")).forEach(path -> {
                try {
                    String source = Files.readString(path);
                    assertThat(source)
                        .as(path.toString())
                        .doesNotContain("application.climate.spi");
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }

    @Test
    void claimsDoesNotImportClimateSpi() throws IOException {
        Path root = Path.of("src/main/java/com/aegisterra/platform/application/claims");
        if (!Files.exists(root)) {
            return;
        }
        try (Stream<Path> files = Files.walk(root)) {
            files.filter(p -> p.toString().endsWith(".java")).forEach(path -> {
                try {
                    String source = Files.readString(path);
                    assertThat(source)
                        .as(path.toString())
                        .doesNotContain("application.climate.spi")
                        .doesNotContain("ClimateDataProvider");
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }
}
