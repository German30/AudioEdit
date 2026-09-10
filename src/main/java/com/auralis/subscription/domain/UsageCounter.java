package com.auralis.subscription.domain;

import com.auralis.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Per-user resource consumption. {@code renderSecondsUsed} is scoped to a
 * calendar month ({@code periodMonth} = first day of that month) and resets when
 * a new month is first touched; {@code storageBytesUsed} is a running total kept
 * in sync by the asset / render modules.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "usage_counters", indexes = @Index(name = "ux_usage_user", columnList = "user_id", unique = true))
public class UsageCounter extends BaseEntity {

    @Column(name = "user_id", nullable = false, length = 36)
    private UUID userId;

    @Column(name = "period_month", nullable = false)
    private LocalDate periodMonth = firstOfCurrentMonth();

    @Column(name = "render_seconds_used", nullable = false)
    private long renderSecondsUsed;

    @Column(name = "storage_bytes_used", nullable = false)
    private long storageBytesUsed;

    public static LocalDate firstOfCurrentMonth() {
        return LocalDate.now().withDayOfMonth(1);
    }

    /** Roll the monthly render bucket over if we've crossed into a new month. */
    public void rollPeriodIfNeeded() {
        LocalDate current = firstOfCurrentMonth();
        if (!current.equals(periodMonth)) {
            periodMonth = current;
            renderSecondsUsed = 0;
        }
    }
}
