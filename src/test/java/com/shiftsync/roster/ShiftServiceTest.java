package com.shiftsync.roster;

import com.shiftsync.audit.AuditService;
import com.shiftsync.common.exception.InvalidOperationException;
import com.shiftsync.common.exception.ResourceNotFoundException;
import com.shiftsync.common.exception.ShiftConflictException;
import com.shiftsync.roster.dto.CreateShiftRequest;
import com.shiftsync.security.CurrentUser;
import com.shiftsync.security.UserPrincipal;
import com.shiftsync.staff.AppUser;
import com.shiftsync.staff.AppUserRepository;
import com.shiftsync.staff.Role;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Pure unit tests — no Spring context, no real database. All dependencies
 * are mocked so these run in milliseconds and test ONLY ShiftService's own
 * decision logic (overlap detection, validation), not the wiring around it.
 * That wiring gets tested separately by the Testcontainers integration test.
 */
@ExtendWith(MockitoExtension.class)
class ShiftServiceTest {

    @Mock private ShiftRepository shiftRepository;
    @Mock private AppUserRepository appUserRepository;
    @Mock private PublicHolidayService publicHolidayService;
    @Mock private AuditService auditService;

    @InjectMocks
    private ShiftService shiftService;

    private final UUID businessId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private final UUID staffId = UUID.randomUUID();

    @BeforeEach
    void setUpSecurityContext() {
        // ShiftService reads the current business/user via CurrentUser,
        // which reads Spring Security's context — so tests need to fake
        // "someone is logged in" the same way a real request would.
        UserPrincipal principal = new UserPrincipal(userId, businessId, "owner@test.com", "OWNER");
        var auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private AppUser activeStaffMember() {
        return AppUser.builder()
                .id(staffId)
                .businessId(businessId)
                .fullName("Jamie Chen")
                .email("jamie@test.com")
                .role(Role.STAFF)
                .active(true)
                .build();
    }

    @Test
    void createShift_succeeds_whenNoOverlapExists() {
        AppUser staff = activeStaffMember();
        Instant start = Instant.now().plus(1, ChronoUnit.DAYS);
        Instant end = start.plus(8, ChronoUnit.HOURS);
        CreateShiftRequest request = new CreateShiftRequest(staffId, start, end);

        when(appUserRepository.findByIdAndBusinessId(staffId, businessId)).thenReturn(Optional.of(staff));
        when(shiftRepository.findOverlapping(businessId, staffId, start, end)).thenReturn(List.of());
        when(publicHolidayService.isPublicHoliday(any())).thenReturn(false);
        when(shiftRepository.save(any(Shift.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = shiftService.createShift(request);

        assertThat(response.staffId()).isEqualTo(staffId);
        assertThat(response.publicHoliday()).isFalse();
        verify(shiftRepository).save(any(Shift.class));
        verify(auditService).log(eq(businessId), eq(userId), eq("SHIFT_CREATED"), eq("Shift"), any(), any());
    }

    @Test
    void createShift_throwsShiftConflict_whenOverlapExists() {
        AppUser staff = activeStaffMember();
        Instant start = Instant.now().plus(1, ChronoUnit.DAYS);
        Instant end = start.plus(8, ChronoUnit.HOURS);
        CreateShiftRequest request = new CreateShiftRequest(staffId, start, end);

        Shift conflicting = Shift.builder().id(UUID.randomUUID()).build();

        when(appUserRepository.findByIdAndBusinessId(staffId, businessId)).thenReturn(Optional.of(staff));
        when(shiftRepository.findOverlapping(businessId, staffId, start, end)).thenReturn(List.of(conflicting));

        assertThatThrownBy(() -> shiftService.createShift(request))
                .isInstanceOf(ShiftConflictException.class)
                .hasMessageContaining("Jamie Chen");

        // Critically: never save a conflicting shift, no matter what.
        verify(shiftRepository, never()).save(any());
    }

    @Test
    void createShift_throwsInvalidOperation_whenEndTimeBeforeStartTime() {
        Instant start = Instant.now().plus(1, ChronoUnit.DAYS);
        Instant end = start.minus(1, ChronoUnit.HOURS); // end before start — invalid
        CreateShiftRequest request = new CreateShiftRequest(staffId, start, end);

        assertThatThrownBy(() -> shiftService.createShift(request))
                .isInstanceOf(InvalidOperationException.class);

        verifyNoInteractions(shiftRepository, publicHolidayService);
    }

    @Test
    void createShift_throwsResourceNotFound_whenStaffDoesNotExistInThisBusiness() {
        Instant start = Instant.now().plus(1, ChronoUnit.DAYS);
        Instant end = start.plus(8, ChronoUnit.HOURS);
        CreateShiftRequest request = new CreateShiftRequest(staffId, start, end);

        when(appUserRepository.findByIdAndBusinessId(staffId, businessId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> shiftService.createShift(request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createShift_throwsInvalidOperation_whenStaffIsDeactivated() {
        AppUser inactiveStaff = AppUser.builder()
                .id(staffId).businessId(businessId).fullName("Jamie Chen")
                .role(Role.STAFF).active(false)
                .build();
        Instant start = Instant.now().plus(1, ChronoUnit.DAYS);
        Instant end = start.plus(8, ChronoUnit.HOURS);
        CreateShiftRequest request = new CreateShiftRequest(staffId, start, end);

        when(appUserRepository.findByIdAndBusinessId(staffId, businessId)).thenReturn(Optional.of(inactiveStaff));

        assertThatThrownBy(() -> shiftService.createShift(request))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("deactivated");
    }
}