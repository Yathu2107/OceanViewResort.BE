package com.oceanviewresort.model;

import java.time.LocalDate;

/**
 * Reservation entity representing guest reservations
 * Uses room_id instead of room_type for better relationship management
 * Status values: OCCUPIED, COMPLETED
 */
public class Reservation {

    private int reservationId;
    private Guest guest;
    private int roomId;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private String status; // OCCUPIED, COMPLETED

    public Reservation() {
    }

    public Reservation(int reservationId, Guest guest, int roomId,
            LocalDate checkInDate, LocalDate checkOutDate, String status) {
        this.reservationId = reservationId;
        this.guest = guest;
        this.roomId = roomId;
        this.checkInDate = checkInDate;
        this.checkOutDate = checkOutDate;
        this.status = status;
    }

    public int getReservationId() {
        return reservationId;
    }

    public void setReservationId(int reservationId) {
        this.reservationId = reservationId;
    }

    public Guest getGuest() {
        return guest;
    }

    public void setGuest(Guest guest) {
        this.guest = guest;
    }

    public int getRoomId() {
        return roomId;
    }

    public void setRoomId(int roomId) {
        this.roomId = roomId;
    }

    public LocalDate getCheckInDate() {
        return checkInDate;
    }

    public void setCheckInDate(LocalDate checkInDate) {
        this.checkInDate = checkInDate;
    }

    public LocalDate getCheckOutDate() {
        return checkOutDate;
    }

    public void setCheckOutDate(LocalDate checkOutDate) {
        this.checkOutDate = checkOutDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}