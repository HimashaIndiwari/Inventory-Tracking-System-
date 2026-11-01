package com.inventory.service;

/** Small input-normalisation helpers shared by the services. */
final class Text {

    private Text() {
    }

    /** Trims; blank or null becomes null. */
    static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
