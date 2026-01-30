package com.d102.crescendo.domain.fcm.repository;

import com.d102.crescendo.domain.fcm.entity.FcmToken;
import com.d102.crescendo.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FcmTokenRepository extends JpaRepository<FcmToken, Integer> {
    List<FcmToken> findAllByUser(User user);
    Optional<FcmToken> findByUserAndToken(User user, String token);
}
