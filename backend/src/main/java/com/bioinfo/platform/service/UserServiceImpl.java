package com.bioinfo.platform.service;

import com.bioinfo.platform.dto.LoginRequest;
import com.bioinfo.platform.dto.RegisterRequest;
import com.bioinfo.platform.dto.UserDTO;
import com.bioinfo.platform.entity.SysUser;
import com.bioinfo.platform.entity.TaskLog;
import com.bioinfo.platform.repository.SysUserRepository;
import com.bioinfo.platform.repository.TaskLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final SysUserRepository sysUserRepository;
    private final TaskLogRepository taskLogRepository;

    @Override
    @Transactional
    public UserDTO register(RegisterRequest request) {
        if (sysUserRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("用户名已存在，请更换用户名");
        }

        SysUser user = new SysUser();
        user.setUsername(request.getUsername());
        user.setPassword(request.getPassword());
        user.setNickname(request.getNickname() != null ? request.getNickname() : request.getUsername());
        user.setEmail(request.getEmail());
        user.setStatus(1);

        user = sysUserRepository.save(user);

        try {
            TaskLog taskLog = new TaskLog();
            taskLog.setLevel("INFO");
            taskLog.setMessage("新用户注册: " + user.getUsername() + " (ID: " + user.getId() + ")");
            taskLogRepository.save(taskLog);
        } catch (Exception e) {
            log.warn("记录注册日志失败: {}", e.getMessage());
        }

        return UserDTO.fromUser(user);
    }

    @Override
    public UserDTO login(LoginRequest request) {
        Optional<SysUser> userOpt = sysUserRepository.findByUsername(request.getUsername());
        if (userOpt.isEmpty()) {
            throw new RuntimeException("用户名或密码错误");
        }

        SysUser user = userOpt.get();
        if (!user.getPassword().equals(request.getPassword())) {
            throw new RuntimeException("用户名或密码错误");
        }

        if (user.getStatus() != 1) {
            throw new RuntimeException("该账号已被禁用，请联系管理员");
        }

        return UserDTO.fromUser(user);
    }

    @Override
    public UserDTO getUserById(Long id) {
        SysUser user = sysUserRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        return UserDTO.fromUser(user);
    }

    @Override
    public UserDTO getUserByUsername(String username) {
        SysUser user = sysUserRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        return UserDTO.fromUser(user);
    }
}
