package com.ridelink.ride_service.service;

import com.ridelink.ride_service.client.DriverClient;
import com.ridelink.ride_service.client.FareClient;
import com.ridelink.ride_service.dto.DriverResponse;
import com.ridelink.ride_service.dto.PaymentResponse;
import com.ridelink.ride_service.model.Ride;
import com.ridelink.ride_service.repository.RideRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class RideService {

    private final RideRepository rideRepository;
    private final DriverClient driverClient;
    private final FareClient fareClient;

    public RideService(RideRepository rideRepository, DriverClient driverClient, FareClient fareClient) {
        this.rideRepository = rideRepository;
        this.driverClient = driverClient;
        this.fareClient = fareClient;
    }

    public Ride requestRide(Ride ride) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("An authenticated passenger is required to request a ride.");
        }

        DriverResponse driver = driverClient.findAndReserveAvailableDriver();
        if (driver == null) {
            throw new NoDriverAvailableException("No driver is currently available.");
        }

        ride.setPassengerId(authentication.getName());
        ride.setDriverId(driver.getId());
        ride.setStatus("ASSIGNED");
        return rideRepository.save(ride);
    }

    @Transactional(readOnly = true)
    public Ride getRide(String id) {
        return rideRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ride not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<Ride> getRidesByPassenger(String passengerId) {
        return rideRepository.findByPassengerIdOrderByIdDesc(passengerId);
    }

    @Transactional(readOnly = true)
    public List<Ride> getRidesByDriver(String driverId) {
        return rideRepository.findByDriverIdOrderByIdDesc(driverId);
    }

    public Ride completeRide(String id, double distanceKm, double durationMin, String authorizationHeader) {
        Ride ride = getRide(id);
        if ("COMPLETED".equalsIgnoreCase(ride.getStatus())) {
            throw new PaymentAlreadyExistsException("Ride has already been completed: " + id);
        }
        if (!"ASSIGNED".equalsIgnoreCase(ride.getStatus())
                && !"IN_PROGRESS".equalsIgnoreCase(ride.getStatus())) {
            throw new InvalidStatusTransitionException("Ride cannot be completed from status: " + ride.getStatus());
        }

        PaymentResponse payment;
        try {
            payment = fareClient.finalizePayment(ride.getId(), ride.getPassengerId(), distanceKm, durationMin,
                    authorizationHeader);
        } catch (FareClient.DuplicatePaymentException e) {
            throw new PaymentAlreadyExistsException(e.getMessage());
        } catch (FareClient.PaymentServiceUnavailableException e) {
            throw new PaymentFailedException(e.getMessage());
        }
        if (payment == null) {
            throw new PaymentFailedException("Fare & payment service returned an empty response.");
        }

        ride.setDistance(distanceKm);
        ride.setDuration((int) Math.round(durationMin));
        ride.setFare(payment.getFinalFare());
        ride.setStatus("COMPLETED");
        return rideRepository.save(ride);
    }

    public static class NoDriverAvailableException extends RuntimeException {
        public NoDriverAvailableException(String message) {
            super(message);
        }
    }

    public static class InvalidStatusTransitionException extends RuntimeException {
        public InvalidStatusTransitionException(String message) {
            super(message);
        }
    }

    public static class PaymentAlreadyExistsException extends RuntimeException {
        public PaymentAlreadyExistsException(String message) {
            super(message);
        }
    }

    public static class PaymentFailedException extends RuntimeException {
        public PaymentFailedException(String message) {
            super(message);
        }
    }
}
