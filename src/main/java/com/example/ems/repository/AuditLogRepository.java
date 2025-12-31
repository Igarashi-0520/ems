package com.example.ems.repository;
import com.example.ems.domain.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
  List<AuditLog> findTop200ByOrderByCreatedAtDesc();
}