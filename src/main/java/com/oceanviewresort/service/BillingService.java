package com.oceanviewresort.service;

import com.oceanviewresort.model.Bill;
import com.oceanviewresort.model.Reservation;
import com.oceanviewresort.repository.BillRepository;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class BillingService {

    private final BillRepository billRepository = new BillRepository();
    private final ReservationService reservationService =
            new ReservationService();
    private final EmailService emailService = new EmailService();

    public Bill checkoutAndGenerateBill(int reservationId, double roomRate) {

        Reservation reservation =
                reservationService.getReservation(reservationId);

        long nights = ChronoUnit.DAYS.between(
                reservation.getCheckInDate(),
                reservation.getCheckOutDate()
        );

        double totalAmount = nights * roomRate;

        Bill bill = new Bill();
        bill.setReservation(reservation);
        bill.setNumberOfNights((int) nights);
        bill.setRoomRatePerNight(roomRate);
        bill.setTotalAmount(totalAmount);
        bill.setGeneratedDate(LocalDate.now());

        billRepository.saveBill(bill);
        reservationService.checkoutReservation(reservationId);

        emailService.sendBillEmail(
                reservation.getGuest().getEmail(),
                bill
        );

        return bill;
    }
}