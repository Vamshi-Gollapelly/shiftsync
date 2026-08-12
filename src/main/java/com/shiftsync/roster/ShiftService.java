package com.shiftsync.roster;

import com.shiftsync.audit.AuditService;
import com.shiftsync.common.exception.InvalidOperationException;
import com.shiftsync.common.exception.ResourceNotFoundException;
import com.shiftsync.common.exception.ShiftConflictException;
import com.shiftsync.roster.dto.CreateShiftRequest;
import com.shiftsync.roster.dto.ShiftResponse;
import com.shiftsync.security.CurrentUser;
import com.shiftsync.staff.AppUser;
import com.shiftsync.staff.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Shift creation enforces two things beyond basic CRUD: no staff member can
 * be double-booked (see the overlap check, backed by
 * ShiftRepository.findOverlapping), and public holiday shifts get flagged
 * automatically so payroll knows a penalty rate applies. Both checks happen
 * BEFORE the shift is persisted — we never save a conflicting or
 * unflagged shift and try to reconcile it after the fact.
 */
@Service
@RequiredArgsConstructor
public class ShiftService {

    private final ShiftRepository shiftRepository;
    private final AppUserRepository appUserRepository;
    private final PublicHolidayService publicHolidayService;
    private final AuditService auditService;

    // Melbourne timezone hardcoded for now — the Business entity already has
    // a timezone field for future multi-timezone support, but that's beyond
    // what this milestone needs to prove.
    private static final ZoneId ROSTER_ZONE = ZoneId.of("Australia/Melbourne");

    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
    @Transactional
    public ShiftResponse createShift(CreateShiftRequest request) {
        UUID businessId = CurrentUser.businessId();

        if (!request.endTime().isAfter(request.startTime())) {
            throw new InvalidOperationException("Shift end time must be after start time");
        }

        AppUser staff = appUserRepository.findByIdAndBusinessId(request.staffId(), businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff member not found"));

        if (!staff.isActive()) {
            throw new InvalidOperationException("Cannot assign a shift to a deactivated staff member");
        }

        List<Shift> overlapping = shiftRepository.findOverlapping(
                businessId, request.staffId(), request.startTime(), request.endTime());
        if (!overlapping.isEmpty()) {
            throw new ShiftConflictException(
                    "This shift overlaps an existing shift for " + staff.getFullName()
                            + " (conflicting shift ID: " + overlapping.get(0).getId() + ")");
        }

        boolean isHoliday = publicHolidayService.isPublicHoliday(
                request.startTime().atZone(ROSTER_ZONE).toLocalDate());

        Shift shift = Shift.builder()
                .businessId(businessId)
                .staffId(request.staffId())
                .startTime(request.startTime())
                .endTime(request.endTime())
                .publicHoliday(isHoliday)
                .penaltyRateReason(isHoliday ? "PUBLIC_HOLIDAY" : null)
                .createdBy(CurrentUser.userId())
                .build();
        shift = shiftRepository.save(shift);

        auditService.log(businessId, CurrentUser.userId(), "SHIFT_CREATED", "Shift", shift.getId(),
                Map.of("staffId", staff.getId().toString(), "publicHoliday", isHoliday));

        return ShiftResponse.from(shift);
    }

    @Transactional(readOnly = true)
    public List<ShiftResponse> listShiftsForBusiness() {
        return shiftRepository.findAllByBusinessIdOrderByStartTimeDesc(CurrentUser.businessId())
                .stream().map(ShiftResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<ShiftResponse> listMyShifts() {
        UUID businessId = CurrentUser.businessId();
        UUID myUserId = CurrentUser.userId();
        return shiftRepository.findAllByBusinessIdAndStaffIdOrderByStartTimeDesc(businessId, myUserId)
                .stream().map(ShiftResponse::from).toList();
    }

    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
    @Transactional
    public void cancelShift(UUID shiftId) {
        UUID businessId = CurrentUser.businessId();
        Shift shift = shiftRepository.findById(shiftId)
                .filter(s -> s.getBusinessId().equals(businessId))
                .orElseThrow(() -> new ResourceNotFoundException("Shift not found"));

        shift.setStatus(ShiftStatus.CANCELLED);
        shiftRepository.save(shift);

        auditService.log(businessId, CurrentUser.userId(), "SHIFT_CANCELLED", "Shift", shift.getId(), null);
    }
}