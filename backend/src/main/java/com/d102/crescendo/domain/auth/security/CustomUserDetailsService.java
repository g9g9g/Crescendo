package com.d102.crescendo.domain.auth.security;

import com.d102.crescendo.domain.user.entity.User;
import com.d102.crescendo.domain.user.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetailsImpl loadUserByUsername(String email) throws UsernameNotFoundException {
        // 삭제되지 않은 사용자만 조회
        User user = userRepository.findByEmailAndDeletedYes(email, false)
                .orElseThrow(() -> new UsernameNotFoundException("이메일이 존재하지 않습니다."));
        return new UserDetailsImpl(user);
    }
}
