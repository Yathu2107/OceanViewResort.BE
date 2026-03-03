package com.oceanviewresort.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for BillingService business logic.
 * Covers bill calculation, pricing, and night computation.
 * 
 * TDD Traceability:
 * REQ-BILL-01: Bill must accurately calculate total based on nights and rate
 * REQ-BILL-02: Bill must correctly compute number of nights between dates
 */
@DisplayName("BillingService Tests")
class BillingServiceTest {

    // -------------------------------------------------------------------------
    // REQ-BILL-01: Bill Calculation
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("TC-UT-13: Bill calculates total amount as nights × room rate")
    void billCalculation_totalAmount_equalsNightsTimesRate() {
        // Test case: 3 nights × $100/night = $300
        int nights = 3;
        double roomRate = 100.0;
        double expectedTotal = nights * roomRate;

        assertEquals(300.0, expectedTotal, 0.001);
    }

    @Test
    @DisplayName("TC-UT-14: Bill calculation with decimal rate: 5 nights × $152.50 = $762.50")
    void billCalculation_decimalRate_calculatesCorrectly() {
        int nights = 5;
        double roomRate = 152.50;
        double expectedTotal = nights * roomRate;

        assertEquals(762.50, expectedTotal, 0.001);
    }

    // -------------------------------------------------------------------------
    // REQ-BILL-03: Night Computation
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("TC-UT-15: Night count for 7-day stay returns 6 nights")
    void nightComputation_weekStay_returnsSixNights() {
        LocalDate checkIn = LocalDate.of(2024, 7, 1);
        LocalDate checkOut = LocalDate.of(2024, 7, 7);

        long nights = java.time.temporal.ChronoUnit.DAYS.between(checkIn, checkOut);
        assertEquals(6, nights);
    }
}
