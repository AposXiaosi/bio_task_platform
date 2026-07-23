package com.bioinfo.platform.service;

import com.bioinfo.platform.dto.LoginRequest;
import com.bioinfo.platform.dto.RegisterRequest;
import com.bioinfo.platform.dto.UserDTO;

public interface UserService {
    UserDTO register(RegisterRequest request);
    UserDTO login(LoginRequest request);
    UserDTO getUserById(Long id);
    UserDTO getUserByUsername(String username);
}
