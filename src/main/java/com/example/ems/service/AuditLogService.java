package com.example.ems.service;
import com.example.ems.domain.AuditLog;
import com.example.ems.domain.UserAccount;
import com.example.ems.repository.AuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
@Service
public class AuditLogService {
  private final AuditLogRepository auditLogRepository;
  public AuditLogService(AuditLogRepository auditLogRepository) {
    this.auditLogRepository = auditLogRepository;
  }
  @Transactional
  public void log(UserAccount actor, String action, String entityType, String entityId, String detail, String ipAddress) {
    AuditLog log = new AuditLog();
    if (actor != null) {
      log.setActor(actor);
      log.setActorUsername(actor.getUsername());
      log.setActorRole(actor.getRole().name());
    } else {
      log.setActorUsername("SYSTEM");
      log.setActorRole("SYSTEM");
    }
    log.setAction(action);
    log.setEntityType(entityType);
    log.setEntityId(entityId);
    log.setDetail(detail);
    log.setIpAddress(ipAddress);
    auditLogRepository.save(log);
  }
  @Transactional
  public void logSystem(String action, String entityType, String entityId, String detail) {
    log(null, action, entityType, entityId, detail, null);
  }
  @Transactional(readOnly = true)
  public List<AuditLog> recent() {
    return auditLogRepository.findTop200ByOrderByCreatedAtDesc();
  }
}