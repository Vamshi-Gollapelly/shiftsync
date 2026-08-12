package com.shiftsync.roster;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * A later milestone will add the overlap-detection query here. Kept minimal
 * now on purpose.
 */
public interface ShiftRepository extends JpaRepository<Shift, UUID> {
}