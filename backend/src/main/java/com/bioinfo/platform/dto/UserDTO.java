package com.bioinfo.platform.dto;

import com.bioinfo.platform.entity.SysUser;
import lombok.Data;

@Data
public class UserDTO {
    private Long id;
    private String username;
    private String nickname;
    private String email;
    private String avatar;
    private Integer status;
    private String token;

    public static UserDTO fromUser(SysUser user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setNickname(user.getNickname());
        dto.setEmail(user.getEmail());
        dto.setAvatar(user.getAvatar());
        dto.setStatus(user.getStatus());
        return dto;
    }
}
