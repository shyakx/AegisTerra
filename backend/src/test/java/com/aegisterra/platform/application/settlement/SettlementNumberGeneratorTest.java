package com.aegisterra.platform.application.settlement;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SettlementNumberGeneratorTest {

    @Test
    void generatesSetYearNineDigits() {
        String number = new SettlementNumberGenerator().next();
        assertTrue(number.matches("SET-\\d{4}-\\d{9}"), number);
    }
}
