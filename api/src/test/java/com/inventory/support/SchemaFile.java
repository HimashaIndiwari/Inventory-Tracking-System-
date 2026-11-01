package com.inventory.support;

import java.nio.file.Files;
import java.nio.file.Path;

/** Locates the read-only database/schema.sql independent of how the tests are launched. */
public final class SchemaFile {

    static final String PROPERTY = "inventory.schemaFile";
    private static final int MAX_PARENT_LEVELS = 3;

    private SchemaFile() {
    }

    public static Path locate() {
        return locate(System.getProperty(PROPERTY), Path.of(System.getProperty("user.dir")));
    }

    public static Path locate(String propertyValue, Path startDir) {
        if (propertyValue != null) {
            Path explicit = Path.of(propertyValue).toAbsolutePath().normalize();
            if (!Files.isRegularFile(explicit)) {
                throw new IllegalStateException("System property " + PROPERTY + " points to a missing file: "
                        + explicit);
            }
            return explicit;
        }
        Path dir = startDir.toAbsolutePath().normalize();
        for (int level = 0; level <= MAX_PARENT_LEVELS && dir != null; level++) {
            Path candidate = dir.resolve("database").resolve("schema.sql");
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
            dir = dir.getParent();
        }
        throw new IllegalStateException("Cannot locate database/schema.sql. Tried system property " + PROPERTY
                + " (not set) and searched upward from " + startDir + ". Run tests through Maven, or set the JUnit"
                + " working directory to the repository root or api/, or add"
                + " -Dinventory.schemaFile=<absolute path to database/schema.sql>.");
    }
}
