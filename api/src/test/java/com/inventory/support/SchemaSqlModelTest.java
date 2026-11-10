package com.inventory.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;

class SchemaSqlModelTest {

    private final SchemaSqlModel model = SchemaSqlModel.parse(SchemaFile.locate());

    @Test
    void parsesAllFiveTables() {
        assertThat(model.tables())
                .containsExactlyInAnyOrder("users", "categories", "suppliers", "products", "stock_transactions");
    }

    @Test
    void parsesTableOptions() {
        assertThat(model.tableOptions("products"))
                .isEqualTo(new SchemaSqlModel.TableOptions("InnoDB", "utf8mb4_0900_ai_ci"));
    }

    @Test
    void parsesForeignKeyDetails() {
        assertThat(model.foreignKeys("products").get("fk_products_category"))
                .isEqualTo(new SchemaSqlModel.ForeignKey("category_id", "categories", "id", "RESTRICT", "RESTRICT"));
    }

    @Test
    void parsesIndexColumnOrder() {
        assertThat(model.indexes("stock_transactions").get("idx_stock_transactions_product_created"))
                .isEqualTo(List.of("product_id", "created_at"));
    }

    @Test
    void parsesPrimaryKeyAndUniques() {
        assertThat(model.primaryKey("users")).isEqualTo(List.of("id"));
        assertThat(model.uniques("products").get("uk_products_sku")).isEqualTo(List.of("sku"));
    }

    @Test
    void parsesCheckExpressions() {
        assertThat(model.checks("stock_transactions").get("chk_stock_transactions_quantity_range"))
                .isEqualTo("quantity <> 0 AND quantity BETWEEN -1000000 AND 1000000");
        assertThat(model.checks("stock_transactions").get("chk_stock_transactions_sign_by_type"))
                .isEqualTo("(type COLLATE utf8mb4_bin = 'ADJUST' AND quantity <> 0)"
                        + " OR (type COLLATE utf8mb4_bin IN ('IN', 'OUT') AND quantity > 0)");
    }

    @Test
    void normalisesMySqlCollateInClause() {
        String fromSchema = SchemaSqlModel.normaliseCheck("role COLLATE utf8mb4_bin IN ('USER', 'ADMIN')");
        String fromMySql = SchemaSqlModel.normaliseCheck(
                "((`role` collate utf8mb4_bin) in (_utf8mb4\\'USER\\',_utf8mb4\\'ADMIN\\'))");
        assertThat(fromSchema).isEqualTo("role collate utf8mb4_bin in ('USER','ADMIN')");
        assertThat(fromMySql).isEqualTo(fromSchema);
    }

    @Test
    void normalisesBetweenClause() {
        assertThat(SchemaSqlModel.normaliseCheck("quantity BETWEEN 1 AND 1000000"))
                .isEqualTo(SchemaSqlModel.normaliseCheck("(`quantity` between 1 and 1000000)"));
    }

    @Test
    void normalisesSimpleComparison() {
        assertThat(SchemaSqlModel.normaliseCheck("price >= 0"))
                .isEqualTo(SchemaSqlModel.normaliseCheck("(`price` >= 0)"));
    }

    @Test
    void literalCaseIsPreserved() {
        assertThat(SchemaSqlModel.normaliseCheck("role in ('USER')"))
                .isNotEqualTo(SchemaSqlModel.normaliseCheck("role in ('user')"));
    }

    @Test
    void meaningfulParenthesesArePreserved() {
        assertThat(SchemaSqlModel.normaliseCheck("(a or b) and c"))
                .isNotEqualTo(SchemaSqlModel.normaliseCheck("a or (b and c)"));
    }

    @Test
    void partialOuterParenthesesAreKept() {
        assertThat(SchemaSqlModel.normaliseCheck("(a > 0) and (b > 0)")).isEqualTo("(a > 0) and (b > 0)");
    }

    @Test
    void differentOperatorsDiffer() {
        assertThat(SchemaSqlModel.normaliseCheck("price >= 0"))
                .isNotEqualTo(SchemaSqlModel.normaliseCheck("price > 0"));
    }

    @Test
    void failsOnUnrecognisedConstraintLine() {
        String sql = "CREATE TABLE t (\n    id BIGINT NOT NULL,\n    CONSTRAINT x WEIRD (id)\n)"
                + " ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;";
        assertThatThrownBy(() -> SchemaSqlModel.parseText(sql)).isInstanceOf(IllegalStateException.class);
    }
}
