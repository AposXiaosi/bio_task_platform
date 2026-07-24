package com.bioinfo.platform.service;

import com.bioinfo.platform.dto.LoginRequest;
import com.bioinfo.platform.dto.RegisterRequest;
import com.bioinfo.platform.dto.UserDTO;
import com.bioinfo.platform.entity.SysUser;
import com.bioinfo.platform.repository.SysUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final SysUserRepository sysUserRepository;

    @Override
    @Transactional
    public UserDTO register(RegisterRequest request) {
        if (sysUserRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("用户名已存在");
        }

        SysUser user = new SysUser();
        user.setUsername(request.getUsername());
        user.setPassword(request.getPassword());
        user.setNickname(request.getNickname() != null ? request.getNickname() : request.getUsername());
        user.setEmail(request.getEmail());
        user.setStatus(1);

        user = sysUserRepository.save(user);
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
            throw new RuntimeException("账号已被禁用");
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
