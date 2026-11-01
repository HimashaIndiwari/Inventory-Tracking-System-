package com.inventory;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import com.inventory.support.DataWritingIntegrationSupport;
import com.inventory.support.ReadOnlyIntegrationSupport;

/**
 * Enforces that every *IT class declares whether it only reads or also writes (and therefore cleans)
 * inventory_test.
 */
class IntegrationTestConventionsTest {

    static final Set<String> READ_ONLY_ALLOWLIST = Set.of("ApplicationContextIT", "SchemaConformanceIT");

    @Test
    void everyIntegrationTestUsesABaseClass() throws Exception {
        for (Class<?> it : integrationTestClasses()) {
            boolean readOnly = ReadOnlyIntegrationSupport.class.isAssignableFrom(it);
            boolean dataWriting = DataWritingIntegrationSupport.class.isAssignableFrom(it);
            assertThat(readOnly ^ dataWriting)
                    .as("%s must extend exactly one of ReadOnlyIntegrationSupport / DataWritingIntegrationSupport",
                            it.getName())
                    .isTrue();
        }
    }

    @Test
    void onlyAllowlistedTestsAreReadOnly() throws Exception {
        Set<String> readOnly = integrationTestClasses().stream()
                .filter(ReadOnlyIntegrationSupport.class::isAssignableFrom)
                .map(Class::getSimpleName)
                .collect(Collectors.toSet());
        assertThat(READ_ONLY_ALLOWLIST).containsAll(readOnly);
    }

    @Test
    void dataWritingBaseUsesPerClassLifecycle() {
        TestInstance lifecycle = DataWritingIntegrationSupport.class.getAnnotation(TestInstance.class);
        assertThat(lifecycle).isNotNull();
        assertThat(lifecycle.value()).isEqualTo(TestInstance.Lifecycle.PER_CLASS);
    }

    @Test
    void afterAllCleanupIsNonStatic() {
        List<Method> afterAll = Arrays.stream(DataWritingIntegrationSupport.class.getDeclaredMethods())
                .filter(m -> m.isAnnotationPresent(AfterAll.class))
                .toList();
        assertThat(afterAll).hasSize(1);
        assertThat(Modifier.isStatic(afterAll.get(0).getModifiers())).isFalse();
    }

    private static List<Class<?>> integrationTestClasses() throws IOException, URISyntaxException {
        // Locate the compiled test classes from this class itself, independent of the working directory.
        Path testClasses = Path.of(IntegrationTestConventionsTest.class.getProtectionDomain()
                .getCodeSource().getLocation().toURI());
        try (Stream<Path> files = Files.walk(testClasses)) {
            return files
                    .map(p -> testClasses.relativize(p).toString().replace('\\', '/'))
                    .filter(name -> name.endsWith("IT.class") && !name.contains("$"))
                    .map(name -> name.substring(0, name.length() - ".class".length()).replace('/', '.'))
                    .map(IntegrationTestConventionsTest::load)
                    .filter(c -> !Modifier.isAbstract(c.getModifiers()))
                    .collect(Collectors.toList());
        }
    }

    private static Class<?> load(String className) {
        try {
            return Class.forName(className, false, IntegrationTestConventionsTest.class.getClassLoader());
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }
}
