package com.shiftsync.roster;

import com.shiftsync.roster.dto.CreateShiftRequest;
import com.shiftsync.roster.dto.ShiftResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/shifts")
@RequiredArgsConstructor
public class ShiftController {

    private final ShiftService shiftService;

    @PostMapping
    public ResponseEntity<ShiftResponse> create(@Valid @RequestBody CreateShiftRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(shiftService.createShift(request));
    }

    @GetMapping
    public ResponseEntity<List<ShiftResponse>> listAll() {
        return ResponseEntity.ok(shiftService.listShiftsForBusiness());
    }

    @GetMapping("/mine")
    public ResponseEntity<List<ShiftResponse>> listMine() {
        return ResponseEntity.ok(shiftService.listMyShifts());
    }

    @DeleteMapping("/{shiftId}")
    public ResponseEntity<Void> cancel(@PathVariable UUID shiftId) {
        shiftService.cancelShift(shiftId);
        return ResponseEntity.noContent().build();
    }
}