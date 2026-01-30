package com.d102.crescendo.domain.user.entity;

import com.d102.crescendo.domain.common.entity.Tier;
import com.d102.crescendo.domain.sheet.entity.Instrument;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserInstrumentTier {

    @EmbeddedId
    private UserInstrumentTierId id;

    @MapsId("userId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @MapsId("instrumentId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instrument_id", nullable = false)
    private Instrument instrument;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tier_id", nullable = false)
    private Tier tier;

    @Builder.Default
    private Integer exp = 0;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Builder.Default
    private Integer practiceTime = 0;

    public void updatePracticeTime(Integer additionalTime) {
        this.practiceTime += additionalTime;
    }
}
