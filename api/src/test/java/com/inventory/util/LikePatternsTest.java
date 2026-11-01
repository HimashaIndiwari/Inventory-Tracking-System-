package com.inventory.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LikePatternsTest {

    @Test
    void escapesWildcards() {
        assertThat(LikePatterns.contains("50%_a!")).isEqualTo("%50!%!_a!!%");
    }

    @Test
    void trimsInput() {
        assertThat(LikePatterns.contains("  mouse ")).isEqualTo("%mouse%");
    }

    @Test
    void blankIsNull() {
        assertThat(LikePatterns.contains("  ")).isNull();
        assertThat(LikePatterns.contains(null)).isNull();
    }
}
