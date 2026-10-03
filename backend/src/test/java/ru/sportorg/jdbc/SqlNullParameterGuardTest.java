package ru.sportorg.jdbc;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * PostgreSQL не может определить тип параметра, который стоит один в ":p IS NULL" или в полиморфной
 * функции вроде jsonb_build_array(:p): при значении null запрос падает с
 * "could not determine data type of parameter". Такие параметры нужно приводить явно:
 * CAST(:p AS text) IS NULL. Юнит-тесты с моками эту ошибку не ловят, поэтому проверяем текст SQL.
 */
class SqlNullParameterGuardTest {

    private static final Pattern UNTYPED_NULL_CHECK =
            Pattern.compile(":(\\w+)\\s+IS\\s+(NOT\\s+)?NULL", Pattern.CASE_INSENSITIVE);
    private static final Pattern UNTYPED_POLYMORPHIC_ARGUMENT =
            Pattern.compile("jsonb_build_array\\(\\s*:\\w+\\s*\\)", Pattern.CASE_INSENSITIVE);

    @Test
    void nullableSqlParametersHaveExplicitType() throws IOException {
        List<String> violations = new ArrayList<>();
        try (Stream<Path> files = Files.walk(Path.of("src/main/java"))) {
            for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                List<String> lines = Files.readAllLines(file);
                for (int i = 0; i < lines.size(); i++) {
                    String line = lines.get(i);
                    if (UNTYPED_NULL_CHECK.matcher(line).find() || UNTYPED_POLYMORPHIC_ARGUMENT.matcher(line).find()) {
                        violations.add(file + ":" + (i + 1));
                    }
                }
            }
        }
        assertTrue(violations.isEmpty(),
                () -> "Параметр SQL без явного типа (нужен CAST(:p AS тип)):\n" + String.join("\n", violations));
    }
}
