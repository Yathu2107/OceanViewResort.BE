package com.oceanviewresort.service;

import com.oceanviewresort.model.Bill;
import com.oceanviewresort.model.Reservation;
import com.oceanviewresort.model.Room;
import com.oceanviewresort.repository.BillRepository;
import com.oceanviewresort.repository.RoomRepository;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public class BillingService {

        private final BillRepository billRepository = new BillRepository();
        private final ReservationService reservationService = new ReservationService();
        private final RoomRepository roomRepository = new RoomRepository();
        private final EmailService emailService = new EmailService();

        public Bill checkoutAndGenerateBill(int reservationId, double roomRate) {

                Reservation reservation = reservationService.getReservation(reservationId);

                long nights = ChronoUnit.DAYS.between(
                                reservation.getCheckInDate(),
                                reservation.getCheckOutDate());

                double totalAmount = nights * roomRate;

                Bill bill = new Bill();
                bill.setReservation(reservation);
                bill.setNumberOfNights((int) nights);
                bill.setTotalAmount(totalAmount);
                bill.setGeneratedDate(LocalDate.now());

                billRepository.saveBill(bill);
                reservationService.completeReservation(reservationId);

                emailService.sendBillEmail(
                                reservation.getGuest().getEmail(),
                                bill);

                return bill;
        }

        /**
         * Get all bills enriched with full reservation, guest and room details.
         *
         * @return List of fully hydrated Bill objects
         */
        public List<Bill> getAllBillsWithDetails() {
                List<Bill> bills = billRepository.findAll();
                List<Bill> enriched = new ArrayList<>();

                for (Bill bill : bills) {
                        int reservationId = bill.getReservation().getReservationId();

                        // Hydrate reservation with guest + room IDs
                        Reservation reservation = reservationService.getReservation(reservationId);
                        bill.setReservation(reservation);

                        // Compute nights from reservation dates
                        long nights = ChronoUnit.DAYS.between(
                                        reservation.getCheckInDate(),
                                        reservation.getCheckOutDate());
                        bill.setNumberOfNights((int) nights);

                        // Build room details
                        List<Bill.RoomDetail> roomDetails = new ArrayList<>();
                        for (Integer roomId : reservation.getRoomIds()) {
                                Room room = roomRepository.findById(roomId);
                                if (room != null) {
                                        double roomTotal = nights * room.getPricePerNight();
                                        roomDetails.add(new Bill.RoomDetail(
                                                        roomId, room.getRoomNumber(),
                                                        room.getPricePerNight(), roomTotal));
                                }
                        }
                        bill.setRoomDetails(roomDetails);

                        enriched.add(bill);
                }

                return enriched;
        }
}