package com.example.ems;
import com.example.ems.domain.Role;
import com.example.ems.domain.UserAccount;
import com.example.ems.repository.UserRepository;
import com.example.ems.service.AuditLogService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;
@SpringBootApplication
public class EmsApplication {
  public static void main(String[] args) {
    SpringApplication.run(EmsApplication.class, args);
  }
  @Bean
  CommandLineRunner seedInitialAdmin(UserRepository userRepository,
                                     PasswordEncoder passwordEncoder,
                                     AuditLogService auditLogService) {
    return args -> {
      if (userRepository.countByRole(Role.ADMIN) == 0) {
        UserAccount admin = new UserAccount();
        admin.setUsername("1");
        admin.setDisplayName("Initial Admin");
        admin.setRole(Role.ADMIN);
        admin.setPasswordHash(passwordEncoder.encode("1"));
        admin.setEnabled(true);
        userRepository.save(admin);
       auditLogService.logSystem(
            "SYSTEM_SEED_ADMIN",
            "users",
            String.valueOf(admin.getId()),
            "Created initial admin: username=1 password=1"
        );
      }
    };
  }
}