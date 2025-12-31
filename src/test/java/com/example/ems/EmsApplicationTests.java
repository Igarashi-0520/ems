package com.example.ems;
import com.example.ems.domain.Role;
import com.example.ems.domain.UserAccount;
import com.example.ems.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringBootTest
@AutoConfigureMockMvc
class EmsApplicationTests {
  @Autowired MockMvc mockMvc;
  @Autowired UserRepository userRepository;
  @Autowired PasswordEncoder passwordEncoder;
  private void ensureUser(String username, Role role, String rawPassword) {
    if (userRepository.findByUsername(username).isPresent()) return;
    UserAccount u = new UserAccount();
    u.setUsername(username);
    u.setDisplayName(username);
    u.setRole(role);
    u.setEnabled(true);
    u.setPasswordHash(passwordEncoder.encode(rawPassword));
    userRepository.save(u);
  }
  private long extractId(String body) {
    Matcher m = Pattern.compile("id=(\\d+)").matcher(body);
    if (!m.find()) throw new IllegalStateException("id not found in body: " + body);
    return Long.parseLong(m.group(1));
  }
  @Test
  void initialAdminCreated() {
    assertThat(userRepository.findByUsername("1")).isPresent();
  }
  @Test
  void adminCannotUseEmployeeEndpoint() throws Exception {
    mockMvc.perform(post("/api/employee/attendance/clock-in")
            .with(httpBasic("1", "1")))
        .andExpect(status().isForbidden());
  }
  @Test
  void employeeClockInTwice_conflictSecond() throws Exception {
    ensureUser("e1", Role.EMPLOYEE, "pass");
    mockMvc.perform(post("/api/employee/attendance/clock-in")
            .with(httpBasic("e1", "pass")))
        .andExpect(status().isOk())
        .andExpect(content().string(org.hamcrest.Matchers.containsString("OK")));
    mockMvc.perform(post("/api/employee/attendance/clock-in")
            .with(httpBasic("e1", "pass")))
        .andExpect(status().isConflict());
  }
  @Test
  void adminSelfRequestNeedsOtherAdmin_andCannotSelfApprove() throws Exception {
    ensureUser("2", Role.ADMIN, "2");
    var result = mockMvc.perform(post("/api/admin/self/requests/overtime")
            .with(httpBasic("1", "1"))
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .param("targetDate", "2030-01-02")
            .param("overtimeMinutes", "45")
            .param("reason", "admin self"))
        .andExpect(status().isOk())
        .andReturn();
    long requestId = extractId(result.getResponse().getContentAsString());
    mockMvc.perform(post("/api/admin/requests/" + requestId + "/approve")
            .with(httpBasic("1", "1"))
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .param("note", "no"))
        .andExpect(status().isForbidden());
    mockMvc.perform(post("/api/admin/requests/" + requestId + "/approve")
            .with(httpBasic("2", "2"))
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .param("note", "ok"))
        .andExpect(status().isOk())
        .andExpect(content().string(org.hamcrest.Matchers.containsString("status=APPROVED")));
  }
  @Test
  void messageSendRequiresConfirmPassword() throws Exception {
    ensureUser("e1", Role.EMPLOYEE, "pass");
    ensureUser("e2", Role.EMPLOYEE, "pass2");
    mockMvc.perform(post("/api/employee/messages/send")
            .with(httpBasic("e1", "pass"))
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .param("receiverUsername", "e2")
            .param("body", "hello"))
        .andExpect(status().isBadRequest());
    mockMvc.perform(post("/api/employee/messages/send")
            .with(httpBasic("e1", "pass"))
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .param("receiverUsername", "e2")
            .param("body", "hello")
            .param("confirmPassword", "WRONG"))
        .andExpect(status().isForbidden());
    mockMvc.perform(post("/api/employee/messages/send")
            .with(httpBasic("e1", "pass"))
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .param("receiverUsername", "e2")
            .param("body", "hello")
            .param("confirmPassword", "pass"))
        .andExpect(status().isOk())
        .andExpect(content().string(org.hamcrest.Matchers.containsString("OK")));
  }
  @Test
  void leaveRejectRequiresChecklist_allYes() throws Exception {
    ensureUser("e1", Role.EMPLOYEE, "pass");
    var created = mockMvc.perform(post("/api/employee/requests/paid-leave")
            .with(httpBasic("e1", "pass"))
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .param("startDate", "2030-02-01")
            .param("endDate", "2030-02-02")
            .param("reason", "trip"))
        .andExpect(status().isOk())
        .andReturn();
    long requestId = extractId(created.getResponse().getContentAsString());
    mockMvc.perform(post("/api/admin/requests/" + requestId + "/reject")
            .with(httpBasic("1", "1"))
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .param("note", "busy"))
        .andExpect(status().isBadRequest());
    mockMvc.perform(post("/api/admin/requests/" + requestId + "/reject")
            .with(httpBasic("1", "1"))
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .param("note", "busy")
            .param("confirmPolicyChecked", "true")
            .param("confirmEligibilityChecked", "true")
            .param("confirmAlternativeConsidered", "true")
            .param("confirmReasonRecorded", "true")
            .param("confirmConsulted", "true"))
        .andExpect(status().isOk())
        .andExpect(content().string(org.hamcrest.Matchers.containsString("status=REJECTED")));
  }
}