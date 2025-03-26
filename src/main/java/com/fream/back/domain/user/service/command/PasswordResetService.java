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
//        return userRepository.findByEmailAndPhoneNumber(email, phoneNumber).map(user -> {
//            // 1. 랜덤 비밀번호 생성
//            String newPassword = PasswordUtils.generateRandomPassword();
//
//            // 2. 이메일 전송
//            String emailContent = String.format(
//                    "안녕하세요,\n\n새로운 임시 비밀번호는 다음과 같습니다: %s\n\n로그인 후 반드시 비밀번호를 변경해주세요.",
//                    newPassword
//            );
//            try {
//                emailService.sendEmail(user.getEmail(), "임시 비밀번호 안내", emailContent);
//
//                // 3. 임시 비밀번호 암호화 및 저장
//                String encodedPassword = passwordEncoder.encode(newPassword);
//                user.updatePassword(encodedPassword);
//                return true;
//            } catch (Exception e) {
//                throw new RuntimeException("이메일 전송 실패로 인해 비밀번호가 업데이트되지 않았습니다.", e);
//            }
//        }).orElse(false);
        return userRepository.findByEmailAndPhoneNumber(email, phoneNumber).map(user -> {
            // 1. 랜덤 비밀번호 생성
            String newPassword = PasswordUtils.generateRandomPassword();

            // 2. HTML 이메일 컨텐츠 생성
            String htmlContent =
                    "<div style='max-width: 600px; margin: 0 auto; padding: 20px; font-family: Arial, sans-serif; border: 1px solid #e0e0e0; border-radius: 5px;'>" +
                            "<div style='text-align: center; margin-bottom: 20px;'>" +
                            "<h1 style='color: #333; font-size: 36px; margin: 0; letter-spacing: 2px; font-weight: 700;'>FREAM</h1>" +
                            "</div>" +
                            "<h2 style='color: #333; text-align: center;'>임시 비밀번호 안내</h2>" +
                            "<p style='color: #666; line-height: 1.5;'>안녕하세요, " + user.getEmail() + "님!</p>" +
                            "<p style='color: #666; line-height: 1.5;'>요청하신 임시 비밀번호가 발급되었습니다.</p>" +
                            "<div style='background-color: #f5f5f5; padding: 15px; margin: 20px 0; text-align: center; border-radius: 4px;'>" +
                            "<p style='font-size: 18px; font-weight: bold; color: #333; margin: 0;'>" + newPassword + "</p>" +
                            "</div>" +
                            "<p style='color: #666; line-height: 1.5;'>로그인 후 반드시 비밀번호를 변경해주세요.</p>" +
                            "<div style='text-align: center; margin-top: 30px;'>" +
                            "<a href='https://www.pinjun.xyz/login' style='background-color: #4CAF50; color: white; padding: 10px 20px; text-decoration: none; border-radius: 4px;'>로그인 하기</a>" +
                            "</div>" +
                            "<p style='color: #999; font-size: 12px; margin-top: 30px; text-align: center;'>본 메일은 발신 전용이므로 회신되지 않습니다.</p>" +
                            "</div>";

            try {
                // HTML 이메일 보내기
                emailService.sendEmail(user.getEmail(), "임시 비밀번호 안내", htmlContent);

                // 3. 임시 비밀번호 암호화 및 저장
                String encodedPassword = passwordEncoder.encode(newPassword);
                user.updatePassword(encodedPassword);
                return true;
            } catch (Exception e) {
                throw new RuntimeException("이메일 전송 실패로 인해 비밀번호가 업데이트되지 않았습니다.", e);
            }
        }).orElse(false);
    }

    @Transactional
    public boolean AdminCheckPasswordResetAndSendEmail(String email, String phoneNumber) {
        return userRepository.findByEmailAndPhoneNumber(email, phoneNumber).map(user -> {
            // 1. 랜덤 비밀번호 생성
            String newPassword = PasswordUtils.generateRandomPassword();

            // 2. HTML 이메일 컨텐츠 생성 (로고를 텍스트로 대체)
            String htmlContent =
                    "<div style='max-width: 600px; margin: 0 auto; padding: 20px; font-family: Arial, sans-serif; border: 1px solid #e0e0e0; border-radius: 5px;'>" +
                            "<div style='text-align: center; margin-bottom: 30px;'>" +
                            "<h1 style='color: #333; font-size: 36px; margin: 0; letter-spacing: 2px; font-weight: 700;'>FREAM</h1>" +
                            "<p style='color: #666; margin: 5px 0 0;'>관리자 포털</p>" +
                            "</div>" +
                            "<h2 style='color: #333; text-align: center; margin-bottom: 20px;'>임시 비밀번호 안내</h2>" +
                            "<p style='color: #666; line-height: 1.5;'>안녕하세요, 관리자님!</p>" +
                            "<p style='color: #666; line-height: 1.5;'>요청하신 임시 비밀번호가 발급되었습니다.</p>" +
                            "<div style='background-color: #f5f5f5; padding: 20px; margin: 25px 0; text-align: center; border-radius: 4px;'>" +
                            "<p style='font-size: 20px; font-weight: bold; color: #333; margin: 0; letter-spacing: 1px;'>" + newPassword + "</p>" +
                            "</div>" +
                            "<p style='color: #666; line-height: 1.5;'>보안을 위해 로그인 후 반드시 새 비밀번호로 변경해주세요.</p>" +
                            "<div style='text-align: center; margin-top: 30px;'>" +
                            "<a href='https://www.fream.xyz/admin/login' style='background-color: #0d6efd; color: white; padding: 12px 25px; text-decoration: none; border-radius: 4px; font-weight: bold;'>관리자 로그인</a>" +
                            "</div>" +
                            "<div style='margin-top: 40px; padding-top: 20px; border-top: 1px solid #e0e0e0;'>" +
                            "<p style='color: #999; font-size: 12px; text-align: center;'>본 메일은 발신 전용이므로 회신되지 않습니다.<br>비밀번호 변경 요청을 하지 않으셨다면 관리자에게 문의해주세요.</p>" +
                            "</div>" +
                            "</div>";

            try {
                // 3. 이메일 전송 (기존 sendEmail 메서드가 이미 HTML을 지원함)
                emailService.sendEmail(user.getEmail(), "FREAM 관리자 임시 비밀번호 안내", htmlContent);

                // 4. 임시 비밀번호 암호화 및 저장
                String encodedPassword = passwordEncoder.encode(newPassword);
                user.updatePassword(encodedPassword);
                return true;
            } catch (Exception e) {
                throw new RuntimeException("이메일 전송 실패로 인해 비밀번호가 업데이트되지 않았습니다.", e);
            }
        }).orElse(false);
    }

}
