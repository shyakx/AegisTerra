package com.aegisterra.platform.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Phase 9 hardening fitness: application must not depend on presentation.
 */
class LayerArchitectureFitnessTest {

    @Test
    void applicationDoesNotImportPresentation() throws IOException {
        Path root = Path.of("src/main/java/com/aegisterra/platform/application");
        assertThat(root).exists();
        try (Stream<Path> files = Files.walk(root)) {
            files.filter(p -> p.toString().endsWith(".java")).forEach(path -> {
                try {
                    String source = Files.readString(path);
                    assertThat(source)
                        .as(path.toString())
                        .doesNotContain("com.aegisterra.platform.presentation");
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }

    @Test
    void domainDoesNotImportPresentationOrInfrastructure() throws IOException {
        Path root = Path.of("src/main/java/com/aegisterra/platform/domain");
        assertThat(root).exists();
        try (Stream<Path> files = Files.walk(root)) {
            files.filter(p -> p.toString().endsWith(".java")).forEach(path -> {
                try {
                    String source = Files.readString(path);
                    assertThat(source)
                        .as(path.toString())
                        .doesNotContain("com.aegisterra.platform.presentation")
                        .doesNotContain("com.aegisterra.platform.infrastructure");
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }
}
