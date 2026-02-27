package com.oceanviewresort.model;

import java.time.LocalDate;
import java.util.List;

public class Bill {

    private int billId;
    private Reservation reservation;
    private int numberOfNights;
    private double roomRatePerNight;
    private double totalAmount;
    private LocalDate generatedDate;
    private List<RoomDetail> roomDetails; // Room-wise breakdown

    // Inner class for room details
    public static class RoomDetail {
        private int roomId;
        private String roomNumber;
        private double pricePerNight;
        private double totalForRoom; // nights × pricePerNight

        public RoomDetail(int roomId, String roomNumber, double pricePerNight, double totalForRoom) {
            this.roomId = roomId;
            this.roomNumber = roomNumber;
            this.pricePerNight = pricePerNight;
            this.totalForRoom = totalForRoom;
        }

        public int getRoomId() {
            return roomId;
        }

        public void setRoomId(int roomId) {
            this.roomId = roomId;
        }

        public String getRoomNumber() {
            return roomNumber;
        }

        public void setRoomNumber(String roomNumber) {
            this.roomNumber = roomNumber;
        }

        public double getPricePerNight() {
            return pricePerNight;
        }

        public void setPricePerNight(double pricePerNight) {
            this.pricePerNight = pricePerNight;
        }

        public double getTotalForRoom() {
            return totalForRoom;
        }

        public void setTotalForRoom(double totalForRoom) {
            this.totalForRoom = totalForRoom;
        }
    }

    public Bill() {
    }

    public Bill(int billId, Reservation reservation, int numberOfNights,
            double roomRatePerNight, double totalAmount, LocalDate generatedDate) {
        this.billId = billId;
        this.reservation = reservation;
        this.numberOfNights = numberOfNights;
        this.roomRatePerNight = roomRatePerNight;
        this.totalAmount = totalAmount;
        this.generatedDate = generatedDate;
    }

    public int getBillId() {
        return billId;
    }

    public void setBillId(int billId) {
        this.billId = billId;
    }

    public Reservation getReservation() {
        return reservation;
    }

    public void setReservation(Reservation reservation) {
        this.reservation = reservation;
    }

    public int getNumberOfNights() {
        return numberOfNights;
    }

    public void setNumberOfNights(int numberOfNights) {
        this.numberOfNights = numberOfNights;
    }

    public double getRoomRatePerNight() {
        return roomRatePerNight;
    }

    public void setRoomRatePerNight(double roomRatePerNight) {
        this.roomRatePerNight = roomRatePerNight;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public LocalDate getGeneratedDate() {
        return generatedDate;
    }

    public void setGeneratedDate(LocalDate generatedDate) {
        this.generatedDate = generatedDate;
    }

    public List<RoomDetail> getRoomDetails() {
        return roomDetails;
    }

    public void setRoomDetails(List<RoomDetail> roomDetails) {
        this.roomDetails = roomDetails;
    }
}