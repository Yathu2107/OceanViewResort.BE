package com.oceanviewresort.service;

import com.oceanviewresort.exception.ValidationException;
import com.oceanviewresort.model.Room;
import com.oceanviewresort.repository.RoomRepository;

import java.util.List;

/**
 * Service layer for Room operations
 * Handles business logic and validation for room management
 */
public class RoomService {

    private final RoomRepository roomRepository = new RoomRepository();

    /**
     * Create a new room (Manager only)
     */
    public int createRoom(String roomNumber, String roomType, int capacity, double pricePerNight, String status) {
        // Validate inputs
        if (roomNumber == null || roomNumber.trim().isEmpty()) {
            throw new ValidationException("Room number is required");
        }
        if (roomType == null || roomType.trim().isEmpty()) {
            throw new ValidationException("Room type is required");
        }
        if (!roomType.equals("SINGLE") && !roomType.equals("DOUBLE") && !roomType.equals("SUITE")) {
            throw new ValidationException("Room type must be SINGLE, DOUBLE, or SUITE");
        }
        if (capacity <= 0) {
            throw new ValidationException("Capacity must be greater than 0");
        }
        if (pricePerNight <= 0) {
            throw new ValidationException("Price per night must be greater than 0");
        }
        if (status == null || status.trim().isEmpty()) {
            status = "AVAILABLE";
        }
        if (!status.equals("AVAILABLE") && !status.equals("BOOKED") && !status.equals("MAINTENANCE")) {
            throw new ValidationException("Status must be AVAILABLE, BOOKED, or MAINTENANCE");
        }

        // Check if the room number already exists
        if (roomRepository.existsByRoomNumber(roomNumber)) {
            throw new ValidationException("Room number already exists");
        }

        // Create a room object
        Room room = new Room();
        room.setRoomNumber(roomNumber);
        room.setRoomType(roomType);
        room.setCapacity(capacity);
        room.setPricePerNight(pricePerNight);
        room.setStatus(status);

        // Save to a database
        return roomRepository.createRoom(room);
    }

    /**
     * Update an existing room (Manager only)
     */
    public void updateRoom(int roomId, String roomNumber, String roomType, Integer capacity, Double pricePerNight, String status) {
        // Check if the room exists
        Room existingRoom = roomRepository.findById(roomId);
        if (existingRoom == null) {
            throw new ValidationException("Room not found with ID: " + roomId);
        }

        // Validate inputs
        if (roomType != null && !roomType.trim().isEmpty()) {
            if (!roomType.equals("SINGLE") && !roomType.equals("DOUBLE") && !roomType.equals("SUITE")) {
                throw new ValidationException("Room type must be SINGLE, DOUBLE, or SUITE");
            }
            existingRoom.setRoomType(roomType);
        }
        if (capacity != null) {
            if (capacity <= 0) {
                throw new ValidationException("Capacity must be greater than 0");
            }
            existingRoom.setCapacity(capacity);
        }
        if (pricePerNight != null) {
            if (pricePerNight <= 0) {
                throw new ValidationException("Price per night must be greater than 0");
            }
            existingRoom.setPricePerNight(pricePerNight);
        }
        if (status != null && !status.trim().isEmpty()) {
            if (!status.equals("AVAILABLE") && !status.equals("BOOKED") && !status.equals("MAINTENANCE")) {
                throw new ValidationException("Status must be AVAILABLE, BOOKED, or MAINTENANCE");
            }
            existingRoom.setStatus(status);
        }
        if (roomNumber != null && !roomNumber.trim().isEmpty()) {
            // Check if the new room number is already used by another room
            Room roomWithSameNumber = roomRepository.findByRoomNumber(roomNumber);
            if (roomWithSameNumber != null && roomWithSameNumber.getId() != roomId) {
                throw new ValidationException("Room number already exists");
            }
            existingRoom.setRoomNumber(roomNumber);
        }

        // Update in database
        roomRepository.updateRoom(existingRoom);
    }

    /**
     * Get all rooms
     */
    public List<Room> getAllRooms() {
        return roomRepository.findAll();
    }

    /**
     * Get all rooms filtered by status
     * @param status The room status to filter by (AVAILABLE, BOOKED, MAINTENANCE)
     * @return List of rooms with the specified status
     */
    public List<Room> getAllRooms(String status) {
        if (status != null && !status.trim().isEmpty()) {
            String statusUpper = status.toUpperCase();
            if (!statusUpper.equals("AVAILABLE") && !statusUpper.equals("BOOKED") && !statusUpper.equals("MAINTENANCE")) {
                throw new ValidationException("Invalid status. Must be AVAILABLE, BOOKED, or MAINTENANCE");
            }
            return roomRepository.findByStatus(statusUpper);
        }
        return roomRepository.findAll();
    }

    /**
     * Get room by ID
     */
    public Room getRoomById(int roomId) {
        Room room = roomRepository.findById(roomId);
        if (room == null) {
            throw new ValidationException("Room not found with ID: " + roomId);
        }
        return room;
    }

    /**
     * Delete room by ID (Manager only)
     */
    public void deleteRoom(int roomId) {
        if (!roomRepository.existsById(roomId)) {
            throw new ValidationException("Room not found with ID: " + roomId);
        }
        roomRepository.deleteById(roomId);
    }
}
