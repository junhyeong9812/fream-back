package com.fream.back.domain.user.service.command;

import com.fream.back.domain.user.exception.UserErrorCode;
import com.fream.back.domain.user.exception.UserException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class EmailService {

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendEmail(String to, String subject, String content) {
        log.info("이메일 전송 시작: to={}, subject={}", to, subject);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            var helper = new MimeMessageHelper(message, "UTF-8");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(content, true); // HTML 지원

            mailSender.send(message);

            log.info("이메일 전송 완료: to={}, subject={}", to, subject);

        } catch (Exception e) {
            log.error("이메일 전송 실패: to={}, subject={}", to, subject, e);
            throw new UserException(UserErrorCode.USER_NOT_FOUND, "이메일 전송 중 문제가 발생했습니다.", e);
        }
    }
}