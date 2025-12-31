package com.example.ems.domain;
import jakarta.persistence.*;
import java.time.LocalDateTime;
@Entity
@Table(name = "audit_logs")
public class AuditLog {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  @ManyToOne
  @JoinColumn(name = "actor_id")
  private UserAccount actor;
  @Column(name = "actor_username", length = 50)
  private String actorUsername;
  @Column(name = "actor_role", length = 20)
  private String actorRole;
  @Column(nullable = false, length = 100)
  private String action;
  @Column(name = "entity_type", length = 100)
  private String entityType;
  @Column(name = "entity_id", length = 100)
  private String entityId;
  @Column(length = 2000)
  private String detail;
  @Column(name = "ip_address", length = 45)
  private String ipAddress;
  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;
  @PrePersist
  void prePersist() {
    createdAt = LocalDateTime.now();
  }
  public Long getId() { return id; }
  public UserAccount getActor() { return actor; }
  public void setActor(UserAccount actor) { this.actor = actor; }
  public String getActorUsername() { return actorUsername; }
  public void setActorUsername(String actorUsername) { this.actorUsername = actorUsername; }
  public String getActorRole() { return actorRole; }
  public void setActorRole(String actorRole) { this.actorRole = actorRole; }
  public String getAction() { return action; }
  public void setAction(String action) { this.action = action; }
  public String getEntityType() { return entityType; }
  public void setEntityType(String entityType) { this.entityType = entityType; }
  public String getEntityId() { return entityId; }
  public void setEntityId(String entityId) { this.entityId = entityId; }
  public String getDetail() { return detail; }
  public void setDetail(String detail) { this.detail = detail; }
  public String getIpAddress() { return ipAddress; }
  public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
  public LocalDateTime getCreatedAt() { return createdAt; }
}