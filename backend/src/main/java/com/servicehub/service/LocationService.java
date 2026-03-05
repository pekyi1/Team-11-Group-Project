package com.servicehub.service;

import com.servicehub.dto.request.CreateLocationRequest;
import com.servicehub.dto.request.UpdateLocationRequest;
import com.servicehub.dto.response.LocationResponse;
import com.servicehub.exception.DuplicateResourceException;
import com.servicehub.exception.ResourceNotFoundException;
import com.servicehub.model.Location;
import com.servicehub.repository.LocationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LocationService {

    private final LocationRepository locationRepository;

    public List<LocationResponse> getAllLocations() {
        return locationRepository.findByIsActiveTrue().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public LocationResponse getLocationById(Long id) {
        Location location = locationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Location not found with id: " + id));
        return toResponse(location);
    }

    @Transactional
    public LocationResponse createLocation(CreateLocationRequest request) {
        if (locationRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("Location already exists with name: " + request.getName());
        }

        Location location = Location.builder()
                .name(request.getName())
                .address(request.getAddress())
                .city(request.getCity())
                .isActive(true)
                .build();

        location = locationRepository.save(location);
        log.info("Location created: {}", location.getName());
        return toResponse(location);
    }

    @Transactional
    public LocationResponse updateLocation(Long id, UpdateLocationRequest request) {
        Location location = locationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Location not found with id: " + id));

        if (request.getName() != null) {
            location.setName(request.getName());
        }
        if (request.getAddress() != null) {
            location.setAddress(request.getAddress());
        }
        if (request.getCity() != null) {
            location.setCity(request.getCity());
        }
        if (request.getIsActive() != null) {
            location.setIsActive(request.getIsActive());
        }

        location = locationRepository.save(location);
        log.info("Location updated: {}", location.getName());
        return toResponse(location);
    }

    @Transactional
    public LocationResponse deactivateLocation(Long id) {
        Location location = locationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Location not found with id: " + id));

        location.setIsActive(false);
        location = locationRepository.save(location);
        log.info("Location deactivated: {}", location.getName());
        return toResponse(location);
    }

    private LocationResponse toResponse(Location l) {
        return LocationResponse.builder()
                .id(l.getId())
                .name(l.getName())
                .address(l.getAddress())
                .city(l.getCity())
                .isActive(l.getIsActive())
                .createdAt(l.getCreatedAt())
                .updatedAt(l.getUpdatedAt())
                .build();
    }
}
