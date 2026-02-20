package com.oceanviewresort.model;

import java.time.LocalDate;

public class Reservation {

    private int reservationId;
    private Guest guest;
    private String roomType;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private String status; // BOOKED, CHECKED_OUT, CANCELLED

    public Reservation() {
    }

    public Reservation(int reservationId, Guest guest, String roomType,
                       LocalDate checkInDate, LocalDate checkOutDate, String status) {
        this.reservationId = reservationId;
        this.guest = guest;
        this.roomType = roomType;
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

    public String getRoomType() {
        return roomType;
    }

    public void setRoomType(String roomType) {
        this.roomType = roomType;
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