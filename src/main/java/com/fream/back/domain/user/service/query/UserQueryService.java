package com.fream.back.domain.user.service.query;

import com.fream.back.domain.user.dto.EmailFindRequestDto;
import com.fream.back.domain.user.dto.LoginInfoDto;
import com.fream.back.domain.user.entity.Role;
import com.fream.back.domain.user.entity.User;
import com.fream.back.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserQueryService {

    private final UserRepository userRepository;

    // 유저 이메일로 권한 확인
    @Transactional(readOnly = true)
    public void checkAdminRole(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("해당 사용자를 찾을 수 없습니다."));

        if (user.getRole() != Role.ADMIN) {
            throw new SecurityException("관리자 권한이 없습니다.");
        }
    }

    //휴대전화 번호로 이메일 정보 조회
    @Transactional(readOnly = true)
    public String findEmailByPhoneNumber(EmailFindRequestDto emailFindRequestDto) {
        return userRepository.findByPhoneNumber(emailFindRequestDto.getPhoneNumber())
                .map(User::getEmail)
                .orElseThrow(() -> new IllegalArgumentException("해당 휴대폰 번호로 등록된 사용자가 없습니다."));
    }

    //로그인 정보 조회
    @Transactional(readOnly = true)
    public LoginInfoDto getLoginInfo(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("해당 사용자를 찾을 수 없습니다."));

        return new LoginInfoDto(
                user.getEmail(),
                user.getPhoneNumber(),
                user.getShoeSize() != null ? user.getShoeSize().name() : null,
                user.isOptionalPrivacyAgreement(), // 개인정보 수집 및 이용 동의 여부
                user.isPhoneNotificationConsent(), // 문자 메시지 수신 동의 여부
                user.isEmailNotificationConsent() // 이메일 수신 동의 여부
        );
    }

    @Transactional(readOnly = true)
    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("해당 이메일을 가진 사용자가 없습니다: " + email));
    }
}
