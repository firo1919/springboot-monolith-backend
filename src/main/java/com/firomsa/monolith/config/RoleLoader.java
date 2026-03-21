package com.firomsa.monolith.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import com.firomsa.monolith.model.Role;
import com.firomsa.monolith.model.Roles;
import com.firomsa.monolith.repository.RoleRepository;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class RoleLoader implements CommandLineRunner {
    private final RoleRepository roleRepository;

    public RoleLoader(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        log.info("Attempting to register roles");
        var roles = Roles.values();
        for (var role : roles) {
            if (roleRepository.findByName(role).isEmpty()) {
                roleRepository.save(Role.builder().name(role).build());
            }
        }
        log.info("Successfully registered roles");
    }
}
