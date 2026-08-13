package com.shiftsync.staff;

import com.shiftsync.audit.AuditService;
import com.shiftsync.common.exception.DuplicateResourceException;
import com.shiftsync.common.exception.InvalidOperationException;
import com.shiftsync.security.UserPrincipal;
import com.shiftsync.staff.dto.CreateStaffRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StaffServiceTest {

    @Mock private AppUserRepository appUserRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuditService auditService;

    @InjectMocks
    private StaffService staffService;

    private final UUID businessId = UUID.randomUUID();
    private final UUID ownerId = UUID.randomUUID();

    private void loginAs(String role) {
        UserPrincipal principal = new UserPrincipal(ownerId, businessId, "owner@test.com", role);
        var auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createStaff_throwsInvalidOperation_whenAttemptingToCreateOwner() {
        loginAs("OWNER");
        CreateStaffRequest request = new CreateStaffRequest("Someone", "someone@test.com", "TempPass123", Role.OWNER, null);

        assertThatThrownBy(() -> staffService.createStaff(request))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("Cannot create another OWNER");

        verify(appUserRepository, never()).save(any());
    }

    @Test
    void createStaff_throwsInvalidOperation_whenManagerTriesToCreateManager() {
        loginAs("MANAGER"); // not OWNER
        CreateStaffRequest request = new CreateStaffRequest("Someone", "someone@test.com", "TempPass123", Role.MANAGER, null);

        assertThatThrownBy(() -> staffService.createStaff(request))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("Only an OWNER can create a MANAGER");

        verify(appUserRepository, never()).save(any());
    }

    @Test
    void createStaff_throwsDuplicateResource_whenEmailAlreadyExistsInBusiness() {
        loginAs("OWNER");
        CreateStaffRequest request = new CreateStaffRequest("Jamie", "jamie@test.com", "TempPass123", Role.STAFF, null);

        when(appUserRepository.existsByBusinessIdAndEmail(businessId, "jamie@test.com")).thenReturn(true);

        assertThatThrownBy(() -> staffService.createStaff(request))
                .isInstanceOf(DuplicateResourceException.class);

        verify(appUserRepository, never()).save(any());
    }
}