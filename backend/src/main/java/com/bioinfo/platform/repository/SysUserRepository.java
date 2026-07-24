package com.bioinfo.platform.repository;

import com.bioinfo.platform.entity.SysUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SysUserRepository extends JpaRepository<SysUser, Long> {
    boolean existsByUsername(String username);

    Optional<SysUser> findByUsername(String username);
}
