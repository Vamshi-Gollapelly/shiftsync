package com.shiftsync.auth;

import com.shiftsync.audit.AuditService;
import com.shiftsync.auth.dto.AuthResponse;
import com.shiftsync.auth.dto.LoginRequest;
import com.shiftsync.auth.dto.RefreshRequest;
import com.shiftsync.auth.dto.RegisterBusinessRequest;
import com.shiftsync.common.exception.DuplicateResourceException;
import com.shiftsync.common.exception.InvalidCredentialsException;
import com.shiftsync.security.JwtService;
import com.shiftsync.staff.AppUser;
import com.shiftsync.staff.AppUserRepository;
import com.shiftsync.staff.Role;
import com.shiftsync.tenant.Business;
import com.shiftsync.tenant.BusinessRepository;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final BusinessRepository businessRepository;
    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuditService auditService;

    @Value("${jwt.access-token-ttl-minutes}")
    private long accessTtlMinutes;

    @Transactional
    public AuthResponse registerBusiness(RegisterBusinessRequest request) {
        if (businessRepository.existsBySlug(request.slug())) {
            throw new DuplicateResourceException("Business slug '" + request.slug() + "' is already taken");
        }

        Business business = Business.builder()
                .slug(request.slug())
                .name(request.businessName())
                .build();
        business = businessRepository.save(business);

        AppUser owner = AppUser.builder()
                .businessId(business.getId())
                .email(request.ownerEmail().toLowerCase())
                .passwordHash(passwordEncoder.encode(request.password()))
                .fullName(request.ownerFullName())
                .role(Role.OWNER)
                .build();
        owner = appUserRepository.save(owner);

        auditService.log(business.getId(), owner.getId(), "BUSINESS_REGISTERED", "Business", business.getId(),
                Map.of("slug", business.getSlug()));

        return issueTokens(owner, business.getSlug());
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        Business business = businessRepository.findBySlug(request.businessSlug())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid business, email, or password"));

        AppUser user = appUserRepository.findByBusinessIdAndEmail(business.getId(), request.email().toLowerCase())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid business, email, or password"));

        if (!user.isActive() || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("Invalid business, email, or password");
        }

        auditService.log(business.getId(), user.getId(), "LOGIN_SUCCESS", "AppUser", user.getId(), null);

        return issueTokens(user, business.getSlug());
    }

    @Transactional
    public AuthResponse refresh(RefreshRequest request) {
        Claims claims;
        try {
            claims = jwtService.parseClaims(request.refreshToken());
        } catch (Exception e) {
            throw new InvalidCredentialsException("Invalid or expired refresh token");
        }

        if (!jwtService.isRefreshToken(claims)) {
            throw new InvalidCredentialsException("Token provided is not a refresh token");
        }

        UUID userId = UUID.fromString(claims.getSubject());
        AppUser user = appUserRepository.findById(userId)
                .filter(AppUser::isActive)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid or expired refresh token"));

        Business business = businessRepository.findById(user.getBusinessId())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid or expired refresh token"));

        return issueTokens(user, business.getSlug());
    }

    private AuthResponse issueTokens(AppUser user, String businessSlug) {
        String accessToken = jwtService.generateAccessToken(user.getId(), user.getBusinessId(), user.getRole().name(), user.getEmail());
        String refreshToken = jwtService.generateRefreshToken(user.getId());
        return new AuthResponse(accessToken, refreshToken, Duration.ofMinutes(accessTtlMinutes).toSeconds(), user.getRole().name(), businessSlug);
    }
}