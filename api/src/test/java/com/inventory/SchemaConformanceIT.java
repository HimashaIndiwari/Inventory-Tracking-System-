package com.inventory;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;

import com.inventory.support.ReadOnlyIntegrationSupport;
import com.inventory.support.SchemaFile;
import com.inventory.support.SchemaSqlModel;

/**
 * Read-only: compares the existing inventory_test structure (information_schema SELECTs only) with
 * database/schema.sql, which is parsed as text and never executed.
 */
class SchemaConformanceIT extends ReadOnlyIntegrationSupport {

    private static final String HINT =
            " — inventory_test does not match database/schema.sql; re-create it manually (database/README.md §3)";

    private final SchemaSqlModel expected = SchemaSqlModel.parse(SchemaFile.locate());

    @Test
    void tablesEnginesAndCollationsMatch() {
        Map<String, SchemaSqlModel.TableOptions> actual = new LinkedHashMap<>();
        jdbc.query("SELECT TABLE_NAME, ENGINE, TABLE_COLLATION FROM information_schema.TABLES"
                + " WHERE TABLE_SCHEMA = DATABASE()", rs -> {
                    actual.put(rs.getString(1), new SchemaSqlModel.TableOptions(rs.getString(2), rs.getString(3)));
                });
        assertThat(actual.keySet()).as("tables" + HINT).containsExactlyInAnyOrderElementsOf(expected.tables());
        for (String table : expected.tables()) {
            assertThat(actual.get(table)).as("engine/collation of " + table + HINT)
                    .isEqualTo(expected.tableOptions(table));
        }
    }

    @Test
    void primaryKeysMatch() {
        for (String table : expected.tables()) {
            assertThat(indexColumns(table, "PRIMARY")).as("primary key of " + table + HINT)
                    .isEqualTo(expected.primaryKey(table));
        }
    }

    @Test
    void uniqueConstraintsMatchNamesAndColumnOrder() {
        for (String table : expected.tables()) {
            Set<String> actualNames = new TreeSet<>(jdbc.queryForList("SELECT CONSTRAINT_NAME FROM"
                    + " information_schema.TABLE_CONSTRAINTS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?"
                    + " AND CONSTRAINT_TYPE = 'UNIQUE'", String.class, table));
            assertThat(actualNames).as("unique constraints of " + table + HINT)
                    .isEqualTo(new TreeSet<>(expected.uniques(table).keySet()));
            expected.uniques(table).forEach((name, columns) -> assertThat(indexColumns(table, name))
                    .as("columns of " + name + HINT).isEqualTo(columns));
        }
    }

    @Test
    void secondaryIndexesMatchNamesAndColumnOrder() {
        for (String table : expected.tables()) {
            Set<String> actualNames = new TreeSet<>(jdbc.queryForList("SELECT DISTINCT INDEX_NAME FROM"
                    + " information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?"
                    + " AND NON_UNIQUE = 1", String.class, table));
            assertThat(actualNames).as("secondary indexes of " + table + HINT)
                    .isEqualTo(new TreeSet<>(expected.indexes(table).keySet()));
            expected.indexes(table).forEach((name, columns) -> assertThat(indexColumns(table, name))
                    .as("columns of " + name + HINT).isEqualTo(columns));
        }
    }

    @Test
    void foreignKeysMatchColumnsTargetsAndRules() {
        for (String table : expected.tables()) {
            Map<String, SchemaSqlModel.ForeignKey> actual = new LinkedHashMap<>();
            jdbc.query("SELECT k.CONSTRAINT_NAME, k.COLUMN_NAME, k.REFERENCED_TABLE_NAME, k.REFERENCED_COLUMN_NAME,"
                    + " r.DELETE_RULE, r.UPDATE_RULE FROM information_schema.KEY_COLUMN_USAGE k"
                    + " JOIN information_schema.REFERENTIAL_CONSTRAINTS r ON r.CONSTRAINT_SCHEMA = k.CONSTRAINT_SCHEMA"
                    + " AND r.CONSTRAINT_NAME = k.CONSTRAINT_NAME AND r.TABLE_NAME = k.TABLE_NAME"
                    + " WHERE k.CONSTRAINT_SCHEMA = DATABASE() AND k.TABLE_NAME = ?", rs -> {
                        actual.put(rs.getString(1), new SchemaSqlModel.ForeignKey(rs.getString(2), rs.getString(3),
                                rs.getString(4), rs.getString(5), rs.getString(6)));
                    }, table);
            assertThat(actual).as("foreign keys of " + table + HINT).isEqualTo(expected.foreignKeys(table));
        }
    }

    @Test
    void checkConstraintsMatchNormalisedExpressions() {
        for (String table : expected.tables()) {
            Map<String, String> actual = new LinkedHashMap<>();
            jdbc.query("SELECT c.CONSTRAINT_NAME, c.CHECK_CLAUSE FROM information_schema.CHECK_CONSTRAINTS c"
                    + " JOIN information_schema.TABLE_CONSTRAINTS t ON t.CONSTRAINT_SCHEMA = c.CONSTRAINT_SCHEMA"
                    + " AND t.CONSTRAINT_NAME = c.CONSTRAINT_NAME"
                    + " WHERE c.CONSTRAINT_SCHEMA = DATABASE() AND t.TABLE_NAME = ?", rs -> {
                        actual.put(rs.getString(1), rs.getString(2));
                    }, table);
            assertThat(actual.keySet()).as("CHECK constraints of " + table + HINT)
                    .containsExactlyInAnyOrderElementsOf(expected.checks(table).keySet());
            expected.checks(table).forEach((name, expression) -> assertThat(
                    SchemaSqlModel.normaliseCheck(actual.get(name)))
                    .as("CHECK %s: expected <%s>, found <%s>%s", name, expression, actual.get(name), HINT)
                    .isEqualTo(SchemaSqlModel.normaliseCheck(expression)));
        }
    }

    private List<String> indexColumns(String table, String indexName) {
        return jdbc.queryForList("SELECT COLUMN_NAME FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE()"
                + " AND TABLE_NAME = ? AND INDEX_NAME = ? ORDER BY SEQ_IN_INDEX", String.class, table, indexName);
    }
}
