package com.shiftsync.staff;

import com.shiftsync.audit.AuditService;
import com.shiftsync.common.exception.DuplicateResourceException;
import com.shiftsync.common.exception.InvalidOperationException;
import com.shiftsync.common.exception.ResourceNotFoundException;
import com.shiftsync.security.CurrentUser;
import com.shiftsync.staff.dto.CreateStaffRequest;
import com.shiftsync.staff.dto.StaffResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Every method here scopes app_users queries by CurrentUser.businessId() —
 * i.e. from the caller's own JWT — never from a client-supplied parameter.
 * findByIdAndBusinessId (rather than findById) is the load-bearing
 * tenant-isolation guarantee: if a manager at Business A requests a staff id
 * belonging to Business B, this returns empty and the caller gets a 404,
 * not another tenant's data.
 */
@Service
@RequiredArgsConstructor
public class StaffService {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
    @Transactional
    public StaffResponse createStaff(CreateStaffRequest request) {
        UUID businessId = CurrentUser.businessId();
        String requesterRole = CurrentUser.get().getRole();

        if (request.role() == Role.OWNER) {
            throw new InvalidOperationException("Cannot create another OWNER via the staff endpoint");
        }
        if (request.role() == Role.MANAGER && !"OWNER".equals(requesterRole)) {
            throw new InvalidOperationException("Only an OWNER can create a MANAGER");
        }
        if (appUserRepository.existsByBusinessIdAndEmail(businessId, request.email().toLowerCase())) {
            throw new DuplicateResourceException("A staff member with this email already exists at this business");
        }

        AppUser staff = AppUser.builder()
                .businessId(businessId)
                .email(request.email().toLowerCase())
                .passwordHash(passwordEncoder.encode(request.temporaryPassword()))
                .fullName(request.fullName())
                .role(request.role())
                .hourlyRate(request.hourlyRate())
                .build();
        staff = appUserRepository.save(staff);

        auditService.log(businessId, CurrentUser.userId(), "STAFF_CREATED", "AppUser", staff.getId(),
                Map.of("email", staff.getEmail(), "role", staff.getRole().name()));

        return StaffResponse.from(staff);
    }

    @Transactional(readOnly = true)
    public List<StaffResponse> listActiveStaff() {
        UUID businessId = CurrentUser.businessId();
        return appUserRepository.findAllByBusinessIdAndActiveTrue(businessId).stream()
                .map(StaffResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public StaffResponse getStaff(UUID staffId) {
        UUID businessId = CurrentUser.businessId();
        AppUser staff = appUserRepository.findByIdAndBusinessId(staffId, businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff member not found"));
        return StaffResponse.from(staff);
    }

    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
    @Transactional
    public void deactivateStaff(UUID staffId) {
        UUID businessId = CurrentUser.businessId();
        AppUser staff = appUserRepository.findByIdAndBusinessId(staffId, businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff member not found"));

        if (staff.getRole() == Role.OWNER) {
            throw new InvalidOperationException("Cannot deactivate the business owner");
        }

        staff.setActive(false);
        appUserRepository.save(staff);

        auditService.log(businessId, CurrentUser.userId(), "STAFF_DEACTIVATED", "AppUser", staff.getId(), null);
    }
}