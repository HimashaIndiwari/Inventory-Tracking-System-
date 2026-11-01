package com.inventory.util;

/** Builds "contains" LIKE patterns escaped with '!' so user input never acts as a wildcard. */
public final class LikePatterns {

    private LikePatterns() {
    }

    public static String contains(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String escaped = raw.trim().replace("!", "!!").replace("%", "!%").replace("_", "!_");
        return "%" + escaped + "%";
    }
}
