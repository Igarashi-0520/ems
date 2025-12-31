package com.example.ems.domain;
import jakarta.persistence.*;
import java.time.LocalDateTime;
@Entity
@Table(name = "users")
public class UserAccount {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  @Column(nullable = false, length = 50, unique = true)
  private String username;
  @Column(name = "display_name", length = 100)
  private String displayName;
  @Column(name = "password_hash", nullable = false, length = 100)
  private String passwordHash;
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private Role role;
  @Column(nullable = false)
  private boolean enabled = true;
  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;
  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;
  @PrePersist
  void prePersist() {
    LocalDateTime now = LocalDateTime.now();
    createdAt = now;
    updatedAt = now;
  }
  @PreUpdate
  void preUpdate() {
    updatedAt = LocalDateTime.now();
  }
  public Long getId() { return id; }
  public String getUsername() { return username; }
  public void setUsername(String username) { this.username = username; }
  public String getDisplayName() { return displayName; }
  public void setDisplayName(String displayName) { this.displayName = displayName; }
  public String getPasswordHash() { return passwordHash; }
  public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
  public Role getRole() { return role; }
  public void setRole(Role role) { this.role = role; }
  public boolean isEnabled() { return enabled; }
  public void setEnabled(boolean enabled) { this.enabled = enabled; }
  public LocalDateTime getCreatedAt() { return createdAt; }
  public LocalDateTime getUpdatedAt() { return updatedAt; }
}