package com.example.ems.repository;
import com.example.ems.domain.Role;
import com.example.ems.domain.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface UserRepository extends JpaRepository<UserAccount, Long> {
  Optional<UserAccount> findByUsername(String username);
  long countByRole(Role role);
  long countByRoleAndEnabledTrueAndIdNot(Role role, Long id);
}