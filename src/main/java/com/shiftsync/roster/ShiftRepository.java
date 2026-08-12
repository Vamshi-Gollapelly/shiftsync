package com.shiftsync.roster;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface ShiftRepository extends JpaRepository<Shift, UUID> {

    /**
     * Two time ranges overlap if one starts before the other ends, in both
     * directions: (existing.start < new.end) AND (existing.end > new.start).
     * CANCELLED shifts don't block new bookings — a cancelled shift frees up
     * that time slot again.
     */
    @Query("""
        SELECT s FROM Shift s
        WHERE s.businessId = :businessId
          AND s.staffId = :staffId
          AND s.status <> com.shiftsync.roster.ShiftStatus.CANCELLED
          AND s.startTime < :endTime
          AND s.endTime > :startTime
        """)
    List<Shift> findOverlapping(
            @Param("businessId") UUID businessId,
            @Param("staffId") UUID staffId,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime
    );

    List<Shift> findAllByBusinessIdAndStaffIdOrderByStartTimeDesc(UUID businessId, UUID staffId);

    List<Shift> findAllByBusinessIdOrderByStartTimeDesc(UUID businessId);
}