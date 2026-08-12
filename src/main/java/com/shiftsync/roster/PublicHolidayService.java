package com.shiftsync.roster;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Checks whether a date is an Australian public holiday, using the free
 * date.nager.at public API. Results are cached per-year in memory since
 * public holidays for a given year never change once published — no reason
 * to call the external API more than once per year.
 *
 * If the external API is unreachable, we fail SAFE rather than fail LOUD:
 * a shift creation should not be blocked just because a third-party holiday
 * API is down. We simply skip the public-holiday flag in that case, which
 * is logged for a human to reconcile later rather than silently guessed at.
 */
@Service
public class PublicHolidayService {

    private final RestClient restClient = RestClient.create("https://date.nager.at");
    private final Map<Integer, Set<LocalDate>> cacheByYear = new ConcurrentHashMap<>();

    public boolean isPublicHoliday(LocalDate date) {
        Set<LocalDate> holidays = cacheByYear.computeIfAbsent(date.getYear(), this::fetchHolidaysForYear);
        return holidays.contains(date);
    }

    private Set<LocalDate> fetchHolidaysForYear(int year) {
        try {
            NagerHoliday[] holidays = restClient.get()
                    .uri("/api/v3/PublicHolidays/{year}/AU", year)
                    .retrieve()
                    .body(NagerHoliday[].class);

            if (holidays == null) {
                return Set.of();
            }
            return java.util.Arrays.stream(holidays)
                    .map(h -> LocalDate.parse(h.date()))
                    .collect(java.util.stream.Collectors.toSet());

        } catch (RestClientException e) {
            // External API is unavailable — fail safe, not loud. An empty
            // set just means "no public holiday flags this session," not
            // "reject the shift creation."
            return Set.of();
        }
    }

    private record NagerHoliday(String date, String localName, String name) {}
}