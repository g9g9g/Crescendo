package com.d102.crescendo.domain.user.entity;

import com.d102.crescendo.domain.common.entity.ExpLog;
import com.d102.crescendo.domain.rank.entity.UserInstrumentRankDaily;
import com.d102.crescendo.domain.sheet.entity.UserSheet;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "\"user\"")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer userId;

    @Column(nullable = false, unique = true, length = 50)
    private String email;

    @Column(nullable = false, length = 50)
    private String nickname;

    @Column(length = 2048)
    private String profileUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Provider provider;

    @Column(nullable = false)
    @Builder.Default
    private Integer totalPracticeTime = 0;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    @Builder.Default
    private boolean deletedYes = false;

    private LocalDateTime deletedAt;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<UserGenre> userGenres = new HashSet<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<UserInstrumentTier> userInstrumentTiers = new HashSet<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserSheet> userSheets;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<UserInstrumentRankDaily> userInstrumentRankDailies = new HashSet<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ExpLog> expLogs;

    public enum Provider {
        GOOGLE, KAKAO
    }

    public enum Role {
        USER, ADMIN
    }

    public static User createOAuthUser(String loginEmail, String nickname, Provider provider, String profileUrl) {
        return User.builder()
                .email(loginEmail)
                .nickname(nickname)
                .provider(provider)
                .profileUrl(profileUrl)
                .role(Role.USER)
                .build();
    }

    public void updateNickname(String nickname) {
        if (nickname != null && !nickname.isEmpty()) {
            this.nickname = nickname;
        }
    }

    public void updateProfileUrl(String profileUrl) {
        this.profileUrl = profileUrl;
    }

    public void softDelete() {
        this.deletedYes = true;
        this.deletedAt = LocalDateTime.now();

        // unique 제약조건 회피를 위해 email 변경
        this.email = this.email + "_deleted_" + this.userId + "_" + System.currentTimeMillis();

        // 연관된 UserSheet도 모두 소프트 삭제
        if (this.userSheets != null) {
            this.userSheets.forEach(UserSheet::softDelete);
        }
    }

    public void updateTotalPracticeTime(Integer playTime) {
        this.totalPracticeTime += playTime;
    }
}
