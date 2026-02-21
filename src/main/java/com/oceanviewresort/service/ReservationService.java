package com.oceanviewresort.service;

import com.oceanviewresort.exception.ValidationException;
import com.oceanviewresort.model.Reservation;
import com.oceanviewresort.repository.ReservationRepository;

public class ReservationService {

    private final ReservationRepository reservationRepository =
            new ReservationRepository();

    public void addReservation(Reservation reservation) {

        if (reservation.getGuest() == null) {
            throw new ValidationException("Guest details are required");
        }

        reservation.setStatus("BOOKED");
        reservationRepository.saveReservation(reservation);
    }

    public Reservation getReservation(int reservationId) {

        Reservation reservation =
                reservationRepository.findById(reservationId);

        if (reservation == null) {
            throw new ValidationException("Reservation not found");
        }

        return reservation;
    }

    public void cancelReservation(int reservationId) {
        reservationRepository.updateStatus(reservationId, "CANCELLED");
    }

    public void checkoutReservation(int reservationId) {
        reservationRepository.updateStatus(reservationId, "CHECKED_OUT");
    }
}