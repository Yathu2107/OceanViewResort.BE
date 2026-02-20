package com.oceanviewresort.model;

import java.time.LocalDate;

public class Bill {

    private int billId;
    private Reservation reservation;
    private int numberOfNights;
    private double roomRatePerNight;
    private double totalAmount;
    private LocalDate generatedDate;

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
}