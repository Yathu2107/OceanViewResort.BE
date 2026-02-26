package com.oceanviewresort.service;

import com.oceanviewresort.exception.ValidationException;
import com.oceanviewresort.model.Guest;
import com.oceanviewresort.repository.GuestRepository;

import java.util.List;

/**
 * Service layer for Guest operations
 * Handles business logic and validation for guest management
 */
public class GuestService {

    private final GuestRepository guestRepository = new GuestRepository();

    /**
     * Add a new guest
     */
    public int addGuest(String name, String address, String contactNumber, String email) {
        // Validate inputs
        if (name == null || name.trim().isEmpty()) {
            throw new ValidationException("Guest name is required");
        }
        if (contactNumber == null || contactNumber.trim().isEmpty()) {
            throw new ValidationException("Contact number is required");
        }

        // Validate a contact number format (basic validation)
        if (!contactNumber.matches("^[+]?[0-9-\\s()]+$")) {
            throw new ValidationException("Invalid contact number format");
        }

        // Validate email format if provided
        if (email != null && !email.trim().isEmpty()) {
            if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
                throw new ValidationException("Invalid email format");
            }
        }

        // Check if a guest with the same contact number already exists
        if (guestRepository.existsByContactNumber(contactNumber.trim())) {
            throw new ValidationException("Guest with this contact number already exists");
        }

        // Create a guest object
        Guest guest = new Guest();
        guest.setName(name.trim());
        guest.setAddress(address != null ? address.trim() : null);
        guest.setContactNumber(contactNumber.trim());
        guest.setEmail(email != null && !email.trim().isEmpty() ? email.trim() : null);

        // Save to a database
        return guestRepository.createGuest(guest);
    }

    /**
     * Update an existing guest
     */
    public void updateGuest(int guestId, String name, String address, String contactNumber, String email) {
        // Check if a guest exists
        Guest existingGuest = guestRepository.findById(guestId);
        if (existingGuest == null) {
            throw new ValidationException("Guest not found with ID: " + guestId);
        }

        // Validate that at least one field is provided for update
        if ((name == null || name.trim().isEmpty()) &&
            (address == null) &&
            (contactNumber == null || contactNumber.trim().isEmpty()) &&
            (email == null)) {
            throw new ValidationException("At least one field must be provided for update");
        }

        // Validate a contact number format if provided
        if (contactNumber != null && !contactNumber.trim().isEmpty()) {
            if (!contactNumber.matches("^[+]?[0-9-\\s()]+$")) {
                throw new ValidationException("Invalid contact number format");
            }

            // Check if a new contact number is already used by another guest
            Guest guestWithSameContact = guestRepository.findByContactNumber(contactNumber.trim());
            if (guestWithSameContact != null && guestWithSameContact.getId() != guestId) {
                throw new ValidationException("Contact number is already used by another guest");
            }

            existingGuest.setContactNumber(contactNumber.trim());
        }

        // Validate email format if provided
        if (email != null && !email.trim().isEmpty()) {
            if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
                throw new ValidationException("Invalid email format");
            }
            existingGuest.setEmail(email.trim());
        } else if (email != null) {
            existingGuest.setEmail(null);
        }

        // Update other fields
        if (name != null && !name.trim().isEmpty()) {
            existingGuest.setName(name.trim());
        }
        if (address != null) {
            existingGuest.setAddress(address.trim().isEmpty() ? null : address.trim());
        }

        // Update in database
        guestRepository.updateGuest(existingGuest);
    }

    /**
     * Get guest by mobile/contact number
     */
    public Guest getGuestByContactNumber(String contactNumber) {
        if (contactNumber == null || contactNumber.trim().isEmpty()) {
            throw new ValidationException("Contact number is required");
        }

        return guestRepository.findByContactNumber(contactNumber.trim());
    }

    /**
     * Get guest by ID
     */
    public Guest getGuestById(int guestId) {
        Guest guest = guestRepository.findById(guestId);
        if (guest == null) {
            throw new ValidationException("Guest not found with ID: " + guestId);
        }
        return guest;
    }

    /**
     * Get all guests
     */
    public List<Guest> getAllGuests() {
        return guestRepository.findAll();
    }

    /**
     * Delete guest by ID
     */
    public void deleteGuest(int guestId) {
        if (!guestRepository.existsById(guestId)) {
            throw new ValidationException("Guest not found with ID: " + guestId);
        }
        guestRepository.deleteById(guestId);
    }
}
