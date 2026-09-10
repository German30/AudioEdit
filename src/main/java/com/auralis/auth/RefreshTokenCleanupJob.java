package com.auralis.auth;

import com.auralis.auth.repo.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class RefreshTokenCleanupJob {

    private final RefreshTokenRepository repository;

    /** Drop refresh tokens that have been expired for more than a day. */
    @Scheduled(cron = "0 30 3 * * *")
    @Transactional
    public void purgeExpired() {
        int removed = repository.deleteExpired(Instant.now().minus(Duration.ofDays(1)));
        if (removed > 0) log.info("Purged {} expired refresh tokens", removed);
    }
}
