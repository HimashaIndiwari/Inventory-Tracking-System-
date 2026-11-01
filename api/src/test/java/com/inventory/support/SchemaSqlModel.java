package com.inventory.support;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Read-only model of database/schema.sql, parsed as text (never executed). Patterns match the file's
 * formatting; any CONSTRAINT/INDEX line that does not match fails loudly instead of being skipped.
 */
public final class SchemaSqlModel {

    public record TableOptions(String engine, String collation) {
    }

    public record ForeignKey(String column, String refTable, String refColumn, String onDelete, String onUpdate) {
    }

    private record Table(TableOptions options, List<String> primaryKey, Map<String, List<String>> uniques,
            Map<String, List<String>> indexes, Map<String, ForeignKey> foreignKeys, Map<String, String> checks) {
    }

    private static final Pattern TABLE = Pattern.compile(
            "CREATE TABLE (\\w+) \\((.*?)\\)\\s*ENGINE = (\\w+) DEFAULT CHARSET = \\w+ COLLATE = (\\w+);",
            Pattern.DOTALL);
    private static final Pattern PRIMARY_KEY = Pattern.compile("CONSTRAINT (\\w+) PRIMARY KEY \\(([^)]*)\\)");
    private static final Pattern UNIQUE = Pattern.compile("CONSTRAINT (\\w+) UNIQUE \\(([^)]*)\\)");
    private static final Pattern FOREIGN_KEY = Pattern.compile(
            "CONSTRAINT (\\w+) FOREIGN KEY \\((\\w+)\\) REFERENCES (\\w+) \\((\\w+)\\)"
                    + " ON DELETE (\\w+) ON UPDATE (\\w+)");
    private static final Pattern CHECK = Pattern.compile("CONSTRAINT (\\w+) CHECK \\((.*)\\)");
    private static final Pattern INDEX = Pattern.compile("INDEX (\\w+) \\(([^)]*)\\)");
    private static final Pattern LITERAL = Pattern.compile("'(?:[^'\\\\]|\\\\.)*'");
    private static final Pattern COLLATE_GROUP = Pattern.compile("\\((\\w+ collate \\w+)\\)");

    private final Map<String, Table> tables;

    private SchemaSqlModel(Map<String, Table> tables) {
        this.tables = tables;
    }

    public static SchemaSqlModel parse(Path schemaFile) {
        try {
            return parseText(Files.readString(schemaFile, StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static SchemaSqlModel parseText(String sql) {
        String withoutComments = sql.replaceAll("(?m)--.*$", "");
        Map<String, Table> tables = new LinkedHashMap<>();
        Matcher table = TABLE.matcher(withoutComments);
        while (table.find()) {
            tables.put(table.group(1), parseBody(table.group(1), table.group(2),
                    new TableOptions(table.group(3), table.group(4))));
        }
        if (tables.isEmpty()) {
            throw new IllegalStateException("No CREATE TABLE statements recognised in schema.sql");
        }
        return new SchemaSqlModel(tables);
    }

    private static Table parseBody(String tableName, String body, TableOptions options) {
        List<String> primaryKey = List.of();
        Map<String, List<String>> uniques = new LinkedHashMap<>();
        Map<String, List<String>> indexes = new LinkedHashMap<>();
        Map<String, ForeignKey> foreignKeys = new LinkedHashMap<>();
        Map<String, String> checks = new LinkedHashMap<>();
        for (String raw : splitTopLevel(body)) {
            String item = raw.trim().replaceAll("\\s+", " ");
            Matcher m;
            if ((m = PRIMARY_KEY.matcher(item)).matches()) {
                primaryKey = columns(m.group(2));
            } else if ((m = UNIQUE.matcher(item)).matches()) {
                uniques.put(m.group(1), columns(m.group(2)));
            } else if ((m = FOREIGN_KEY.matcher(item)).matches()) {
                foreignKeys.put(m.group(1), new ForeignKey(m.group(2), m.group(3), m.group(4), m.group(5), m.group(6)));
            } else if ((m = CHECK.matcher(item)).matches()) {
                checks.put(m.group(1), m.group(2));
            } else if ((m = INDEX.matcher(item)).matches()) {
                indexes.put(m.group(1), columns(m.group(2)));
            } else if (item.startsWith("CONSTRAINT") || item.startsWith("INDEX")) {
                throw new IllegalStateException("Unrecognised line in table " + tableName + ": " + item);
            }
            // anything else is a column definition, covered by ddl-auto=validate
        }
        return new Table(options, primaryKey, uniques, indexes, foreignKeys, checks);
    }

    /** Splits a table body at commas that are not inside parentheses or string literals. */
    private static List<String> splitTopLevel(String body) {
        List<String> items = new ArrayList<>();
        int depth = 0;
        boolean inLiteral = false;
        int start = 0;
        for (int i = 0; i < body.length(); i++) {
            char c = body.charAt(i);
            if (c == '\'') {
                inLiteral = !inLiteral;
            } else if (!inLiteral && c == '(') {
                depth++;
            } else if (!inLiteral && c == ')') {
                depth--;
            } else if (!inLiteral && depth == 0 && c == ',') {
                items.add(body.substring(start, i));
                start = i + 1;
            }
        }
        items.add(body.substring(start));
        return items;
    }

    private static List<String> columns(String list) {
        return Arrays.stream(list.split(",")).map(String::trim).toList();
    }

    public Set<String> tables() {
        return tables.keySet();
    }

    public TableOptions tableOptions(String table) {
        return table(table).options();
    }

    public List<String> primaryKey(String table) {
        return table(table).primaryKey();
    }

    public Map<String, List<String>> uniques(String table) {
        return table(table).uniques();
    }

    public Map<String, List<String>> indexes(String table) {
        return table(table).indexes();
    }

    public Map<String, ForeignKey> foreignKeys(String table) {
        return table(table).foreignKeys();
    }

    public Map<String, String> checks(String table) {
        return table(table).checks();
    }

    private Table table(String name) {
        Table table = tables.get(name);
        if (table == null) {
            throw new IllegalArgumentException("Table not in schema.sql: " + name);
        }
        return table;
    }

    /**
     * Normalises a CHECK expression using only rewrites known to be semantically equivalent: unescaping,
     * removing the _utf8mb4 introducer and backticks, lower-casing outside string literals, collapsing
     * whitespace, and removing one pair of parentheses around the whole expression plus parentheses that
     * enclose only "identifier collate collation". String literals and all other parentheses are kept.
     */
    public static String normaliseCheck(String expression) {
        String unescaped = expression.replace("\\'", "'");
        StringBuilder out = new StringBuilder();
        Matcher literal = LITERAL.matcher(unescaped);
        int last = 0;
        while (literal.find()) {
            out.append(normaliseCode(unescaped.substring(last, literal.start()), true));
            out.append(literal.group());
            last = literal.end();
        }
        out.append(normaliseCode(unescaped.substring(last), false));
        String result = out.toString().trim();
        result = stripEnclosingPair(result);
        return COLLATE_GROUP.matcher(result).replaceAll("$1");
    }

    private static String normaliseCode(String code, boolean beforeLiteral) {
        String normalised = code.replace("`", "").toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
        if (beforeLiteral) {
            normalised = normalised.replaceAll("_utf8mb4$", "");
        }
        return normalised.replaceAll("\\( ", "(").replaceAll(" \\)", ")").replaceAll(" ?, ?", ",");
    }

    private static String stripEnclosingPair(String text) {
        if (!text.startsWith("(") || !text.endsWith(")")) {
            return text;
        }
        int depth = 0;
        boolean inLiteral = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\'') {
                inLiteral = !inLiteral;
            } else if (!inLiteral && c == '(') {
                depth++;
            } else if (!inLiteral && c == ')') {
                depth--;
                if (depth == 0 && i < text.length() - 1) {
                    return text; // first '(' closes before the end: not an enclosing pair
                }
            }
        }
        return text.substring(1, text.length() - 1);
    }
}
