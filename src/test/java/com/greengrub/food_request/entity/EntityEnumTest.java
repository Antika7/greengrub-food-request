package com.greengrub.food_request.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class EntityEnumTest {

    // ── FoodStatus ────────────────────────────────────────────────────────────

    @Test
    void foodStatus_containsExpectedValues() {
        assertThat(FoodStatus.values()).containsExactlyInAnyOrder(
                FoodStatus.PENDING, FoodStatus.APPROVED, FoodStatus.DONATED, FoodStatus.EXPIRED);
    }

    @Test
    void foodStatus_valueOf_pending() {
        assertThat(FoodStatus.valueOf("PENDING")).isEqualTo(FoodStatus.PENDING);
    }

    @Test
    void foodStatus_valueOf_approved() {
        assertThat(FoodStatus.valueOf("APPROVED")).isEqualTo(FoodStatus.APPROVED);
    }

    @Test
    void foodStatus_valueOf_donated() {
        assertThat(FoodStatus.valueOf("DONATED")).isEqualTo(FoodStatus.DONATED);
    }

    @Test
    void foodStatus_valueOf_expired() {
        assertThat(FoodStatus.valueOf("EXPIRED")).isEqualTo(FoodStatus.EXPIRED);
    }

    // ── Unit ──────────────────────────────────────────────────────────────────

    @Test
    void unit_containsExpectedValues() {
        assertThat(Unit.values()).containsExactlyInAnyOrder(Unit.KG, Unit.SERVINGS);
    }

    @Test
    void unit_valueOf_kg() {
        assertThat(Unit.valueOf("KG")).isEqualTo(Unit.KG);
    }

    @Test
    void unit_valueOf_servings() {
        assertThat(Unit.valueOf("SERVINGS")).isEqualTo(Unit.SERVINGS);
    }

    // ── Quantity ──────────────────────────────────────────────────────────────

    @Test
    void quantity_constructorAndGetters() {
        java.math.BigDecimal amount = new java.math.BigDecimal("2.50");
        Quantity q = new Quantity(amount, Unit.KG);
        assertThat(q.getAmount()).isEqualByComparingTo(amount);
        assertThat(q.getUnit()).isEqualTo(Unit.KG);
    }

    @Test
    void quantity_setters() {
        Quantity q = new Quantity();
        q.setAmount(new java.math.BigDecimal("5.00"));
        q.setUnit(Unit.SERVINGS);
        assertThat(q.getUnit()).isEqualTo(Unit.SERVINGS);
        assertThat(q.getAmount()).isEqualByComparingTo("5.00");
    }
}
