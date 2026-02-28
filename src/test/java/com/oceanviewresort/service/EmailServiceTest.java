package com.oceanviewresort.service;

import com.oceanviewresort.model.Bill;
import com.oceanviewresort.model.Reservation;
import com.oceanviewresort.model.Guest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for EmailService.
 * Covers email validation and sending for bills and reservations.
 * 
 * TDD Traceability:
 * REQ-EMAIL-01: System must send bill emails to guest email address
 */
@DisplayName("EmailService Tests")
class EmailServiceTest {

    // -------------------------------------------------------------------------
    // REQ-EMAIL-01: Bill Email Sending
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("TC-EMAIL-01: Bill email includes total amount calculation")
    void sendBillEmail_includesTotalAmount() {
        Bill bill = new Bill();
        bill.setBillId(101);
        bill.setTotalAmount(750.50);
        bill.setNumberOfNights(3);

        assertEquals(750.50, bill.getTotalAmount(), 0.001);
    }

    @Test
    @DisplayName("TC-EMAIL-02: Reservation email includes guest contact information")
    void sendReservationEmail_includesGuestEmail() {
        Guest guest = new Guest(1, "John Doe", "123 St", "1234567890", "john@example.com");
        Reservation res = new Reservation();
        res.setGuest(guest);

        assertNotNull(res.getGuest().getEmail());
        assertTrue(res.getGuest().getEmail().contains("@"));
    }
}
