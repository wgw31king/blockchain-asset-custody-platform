package io.github.wahhh.bacp.bootstrap;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.github.wahhh.bacp.entity.SysRole;
import io.github.wahhh.bacp.entity.SysUser;
import io.github.wahhh.bacp.entity.SysUserRole;
import io.github.wahhh.bacp.mapper.SysRoleMapper;
import io.github.wahhh.bacp.mapper.SysUserMapper;
import io.github.wahhh.bacp.mapper.SysUserRoleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Seeds the initial {@code admin} super user when database is empty.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BacpBootstrapRunner implements ApplicationRunner {

    private final SysUserMapper sysUserMapper;

    private final SysRoleMapper sysRoleMapper;

    private final SysUserRoleMapper sysUserRoleMapper;

    private final PasswordEncoder passwordEncoder;

    /**
     * {@inheritDoc}
     */
    @Override
    public void run(ApplicationArguments args) {
        Long count = sysUserMapper.selectCount(null);
        if (count != null && count > 0) {
            return;
        }
        SysUser admin = new SysUser();
        admin.setUsername("admin");
        admin.setPasswordHash(passwordEncoder.encode("Admin@123"));
        admin.setEmail("admin@example.com");
        admin.setNickname("Administrator");
        admin.setStatus(1);
        sysUserMapper.insert(admin);
        SysRole superAdmin = sysRoleMapper.selectOne(Wrappers.<SysRole>lambdaQuery().eq(SysRole::getRoleCode, "SUPER_ADMIN"));
        if (superAdmin != null) {
            SysUserRole link = new SysUserRole();
            link.setUserId(admin.getId());
            link.setRoleId(superAdmin.getId());
            sysUserRoleMapper.insert(link);
        }
        log.warn("Seeded default admin user (username=admin password=Admin@123) — rotate immediately in production.");
    }
}
