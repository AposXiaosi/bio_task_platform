package com.bioinfo.platform.controller;

import com.bioinfo.platform.dto.LoginRequest;
import com.bioinfo.platform.dto.RegisterRequest;
import com.bioinfo.platform.dto.UserDTO;
import com.bioinfo.platform.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        Map<String, Object> result = new HashMap<>();
        try {
            UserDTO userDTO = userService.register(request);
            Map<String, Object> data = new HashMap<>();
            data.put("id", userDTO.getId());
            data.put("username", userDTO.getUsername());
            data.put("nickname", userDTO.getNickname());
            result.put("success", true);
            result.put("message", "注册成功");
            result.put("data", data);
            return ResponseEntity.status(HttpStatus.CREATED).body(result);
        } catch (RuntimeException e) {
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "注册失败，请稍后重试");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        Map<String, Object> result = new HashMap<>();
        try {
            UserDTO userDTO = userService.login(request);
            String token = "token_" + userDTO.getId() + "_" + System.currentTimeMillis();
            userDTO.setToken(token);

            Map<String, Object> data = new HashMap<>();
            data.put("id", userDTO.getId());
            data.put("username", userDTO.getUsername());
            data.put("nickname", userDTO.getNickname());
            data.put("token", token);
            result.put("success", true);
            result.put("message", "登录成功");
            result.put("data", data);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "登录失败，请稍后重试");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }

    @GetMapping("/info")
    public ResponseEntity<?> getUserInfo(@RequestParam Long userId) {
        Map<String, Object> result = new HashMap<>();
        try {
            UserDTO userDTO = userService.getUserById(userId);
            Map<String, Object> data = new HashMap<>();
            data.put("id", userDTO.getId());
            data.put("username", userDTO.getUsername());
            data.put("nickname", userDTO.getNickname());
            data.put("email", userDTO.getEmail());
            data.put("avatar", userDTO.getAvatar());
            result.put("success", true);
            result.put("data", data);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "获取用户信息失败");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }
}
