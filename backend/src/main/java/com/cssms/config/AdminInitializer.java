package com.cssms.config;

import com.cssms.entity.Role;
import com.cssms.entity.User;
import com.cssms.enums.RoleName;
import com.cssms.repository.RoleRepository;
import com.cssms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Creates the bootstrap administrator on first start when no user exists. */
@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class AdminInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AppProperties properties;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.count() > 0) {
            return;
        }
        AppProperties.Admin admin = properties.admin();
        Role role = roleRepository.findByName(RoleName.ADMIN)
                .orElseThrow(() -> new IllegalStateException("ADMIN role missing: run Flyway migrations"));
        User user = new User();
        user.setUsername(admin.username());
        user.setPasswordHash(passwordEncoder.encode(admin.password()));
        user.setFullName("System Administrator");
        user.setEmail(admin.email());
        user.setRole(role);
        user.setActive(true);
        userRepository.save(user);
        log.info("Created bootstrap administrator '{}'. Change the password after the first login.", admin.username());
    }
}
