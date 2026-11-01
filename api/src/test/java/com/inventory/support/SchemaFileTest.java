package com.inventory.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SchemaFileTest {

    // Instance field (not static): a failed lookup fails these tests with the SchemaFile message
    // instead of an ExceptionInInitializerError that would hide it.
    private final Path repoRoot = SchemaFile.locate().getParent().getParent();

    @Test
    void locatesSchemaFile() {
        assertThat(SchemaFile.locate()).exists().hasFileName("schema.sql");
    }

    @Test
    void fallbackFindsSchemaFromApiDir() {
        assertThat(SchemaFile.locate(null, repoRoot.resolve("api")))
                .isEqualTo(repoRoot.resolve("database/schema.sql"));
    }

    @Test
    void fallbackFindsSchemaFromRepoRoot() {
        assertThat(SchemaFile.locate(null, repoRoot)).isEqualTo(repoRoot.resolve("database/schema.sql"));
    }

    @Test
    void explicitMissingPathFailsFast() {
        assertThatThrownBy(() -> SchemaFile.locate("C:/nope/schema.sql", repoRoot))
                .hasMessageContaining("inventory.schemaFile");
    }

    @Test
    void notFoundMessageExplainsFixes(@TempDir Path elsewhere) {
        assertThatThrownBy(() -> SchemaFile.locate(null, elsewhere))
                .hasMessageContaining("working directory")
                .hasMessageContaining("-Dinventory.schemaFile");
    }
}
