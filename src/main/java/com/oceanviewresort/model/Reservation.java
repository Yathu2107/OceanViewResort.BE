package com.oceanviewresort.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Reservation entity representing guest reservations
 * Now supports multiple rooms per reservation via room_ids
 * One guest can reserve multiple rooms in a single reservation
 * Status values: OCCUPIED, COMPLETED, CANCELLED
 */
public class Reservation {

    private int reservationId;
    private Guest guest;
    private List<Integer> roomIds; // Multiple rooms per reservation
    private List<String> roomNumbers; // Actual room numbers (e.g., "101", "102")
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private String status; // OCCUPIED, COMPLETED, CANCELLED

    public Reservation() {
        this.roomIds = new ArrayList<>();
        this.roomNumbers = new ArrayList<>();
    }

    public Reservation(int reservationId, Guest guest, List<Integer> roomIds,
            LocalDate checkInDate, LocalDate checkOutDate, String status) {
        this.reservationId = reservationId;
        this.guest = guest;
        this.roomIds = roomIds != null ? roomIds : new ArrayList<>();
        this.roomNumbers = new ArrayList<>();
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

    public List<Integer> getRoomIds() {
        return roomIds;
    }

    public void setRoomIds(List<Integer> roomIds) {
        this.roomIds = roomIds != null ? roomIds : new ArrayList<>();
    }

    public List<String> getRoomNumbers() {
        return roomNumbers;
    }

    public void setRoomNumbers(List<String> roomNumbers) {
        this.roomNumbers = roomNumbers != null ? roomNumbers : new ArrayList<>();
    }

    public void addRoomId(int roomId) {
        if (this.roomIds == null) {
            this.roomIds = new ArrayList<>();
        }
        this.roomIds.add(roomId);
    }

    /**
     * Legacy method for backward compatibility
     * Returns first room ID if available
     */
    public int getRoomId() {
        return (this.roomIds != null && !this.roomIds.isEmpty()) ? this.roomIds.get(0) : 0;
    }

    /**
     * Legacy method for backward compatibility
     * Sets single room as first room in list
     */
    public void setRoomId(int roomId) {
        if (this.roomIds == null) {
            this.roomIds = new ArrayList<>();
        }
        this.roomIds.clear();
        this.roomIds.add(roomId);
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