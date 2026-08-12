package com.shiftsync.staff;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AppUserRepository extends JpaRepository<AppUser, UUID> {

    // Every finder is explicitly scoped by businessId. There is deliberately
    // no findByEmail(String) or findById(UUID) shortcut used anywhere in the
    // service layer without a businessId check alongside it — this is the
    // tenant-isolation guarantee for this table.
    Optional<AppUser> findByBusinessIdAndEmail(UUID businessId, String email);

    Optional<AppUser> findByIdAndBusinessId(UUID id, UUID businessId);

    List<AppUser> findAllByBusinessIdAndActiveTrue(UUID businessId);

    boolean existsByBusinessIdAndEmail(UUID businessId, String email);
}