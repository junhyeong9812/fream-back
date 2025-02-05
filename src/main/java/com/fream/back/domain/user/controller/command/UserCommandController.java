package com.fream.back.domain.user.controller.command;

import com.fream.back.domain.user.dto.UserRegistrationDto;
import com.fream.back.domain.user.entity.User;
import com.fream.back.domain.user.service.command.UserCommandService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserCommandController {

    private final UserCommandService userCommandService;

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody UserRegistrationDto dto) {
        // 회원가입 로직
        User user = userCommandService.registerUser(dto);
        return ResponseEntity.ok("회원가입 성공: " + user.getEmail());
    }

    // 회원 탈퇴, 비번 변경 등...
}

