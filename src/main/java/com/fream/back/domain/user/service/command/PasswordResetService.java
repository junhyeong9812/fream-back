package com.fream.back.domain.user.service.command;

import com.fream.back.domain.user.dto.PasswordResetRequestDto;
import com.fream.back.domain.user.entity.User;
import com.fream.back.domain.user.repository.UserRepository;
import com.fream.back.domain.user.utils.PasswordUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder; // 암호화를 위한 PasswordEncoder
    private final EmailService emailService;

    @Transactional(readOnly = true)
    public boolean checkPasswordResetEligibility(String email, String phoneNumber) {
        return userRepository.findByEmailAndPhoneNumber(email, phoneNumber).isPresent();
    }
    @Transactional
    public boolean resetPassword(PasswordResetRequestDto requestDto) {
        // 비밀번호 검증
        requestDto.validatePasswords();

        // 이메일과 전화번호로 사용자 조회
        User user = userRepository.findByEmailAndPhoneNumber(requestDto.getEmail(), requestDto.getPhoneNumber())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 비밀번호 암호화 및 업데이트
        String encodedPassword = passwordEncoder.encode(requestDto.getNewPassword());
        user.updateUser(null, encodedPassword, null, null, null, null, null);

        // 더티체크에 의해 비밀번호 자동 저장
        return true; // 정상적으로 변경된 경우 true 반환
    }
    @Transactional
    public boolean checkPasswordResetAndSendEmail(String email, String phoneNumber) {
        return userRepository.findByEmailAndPhoneNumber(email, phoneNumber).map(user -> {
            // 1. 랜덤 비밀번호 생성
            String newPassword = PasswordUtils.generateRandomPassword();

            // 2. 이메일 전송
            String emailContent = String.format(
                    "안녕하세요,\n\n새로운 임시 비밀번호는 다음과 같습니다: %s\n\n로그인 후 반드시 비밀번호를 변경해주세요.",
                    newPassword
            );
            try {
                emailService.sendEmail(user.getEmail(), "임시 비밀번호 안내", emailContent);

                // 3. 임시 비밀번호 암호화 및 저장
                String encodedPassword = passwordEncoder.encode(newPassword);
                user.updatePassword(encodedPassword);
                return true;
            } catch (Exception e) {
                throw new RuntimeException("이메일 전송 실패로 인해 비밀번호가 업데이트되지 않았습니다.", e);
            }
        }).orElse(false);
    }

}
