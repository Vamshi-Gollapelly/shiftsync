package com.shiftsync.staff;

import com.shiftsync.staff.dto.CreateStaffRequest;
import com.shiftsync.staff.dto.StaffResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/staff")
@RequiredArgsConstructor
public class StaffController {

    private final StaffService staffService;

    @PostMapping
    public ResponseEntity<StaffResponse> create(@Valid @RequestBody CreateStaffRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(staffService.createStaff(request));
    }

    @GetMapping
    public ResponseEntity<List<StaffResponse>> list() {
        return ResponseEntity.ok(staffService.listActiveStaff());
    }

    @GetMapping("/{staffId}")
    public ResponseEntity<StaffResponse> get(@PathVariable UUID staffId) {
        return ResponseEntity.ok(staffService.getStaff(staffId));
    }

    @DeleteMapping("/{staffId}")
    public ResponseEntity<Void> deactivate(@PathVariable UUID staffId) {
        staffService.deactivateStaff(staffId);
        return ResponseEntity.noContent().build();
    }
}