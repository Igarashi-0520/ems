package com.example.ems.cli;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Map;
import javax.sql.DataSource;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.example.ems.EmsApplication;
import com.example.ems.cli.ConsoleIO;
public class EmsConsoleCli {
    public static void main(String[] args) {
        try (ConfigurableApplicationContext ctx = new SpringApplicationBuilder(EmsApplication.class)
                .web(WebApplicationType.NONE)
                .run(args)) {
            DataSource ds = ctx.getBean(DataSource.class);
            JdbcTemplate jdbc = new JdbcTemplate(ds);
            PasswordEncoder pe = ctx.getBean(PasswordEncoder.class);
            ConsoleIO io = new ConsoleIO();
            new EmsConsoleCli(io, jdbc, pe).run();
        }
    }
    private final ConsoleIO io;
    private final JdbcTemplate jdbc;
    private final PasswordEncoder passwordEncoder;
    private Session session; 
    public EmsConsoleCli(ConsoleIO io, JdbcTemplate jdbc, PasswordEncoder passwordEncoder) {
        this.io = Objects.requireNonNull(io);
        this.jdbc = Objects.requireNonNull(jdbc);
        this.passwordEncoder = Objects.requireNonNull(passwordEncoder);
    }
    private static final class DbUser {
        final long id;
        final String username;
        final String displayName;
        final String passwordHash;
        final String role;
        final boolean enabled;
        DbUser(long id, String username, String displayName, String passwordHash, String role, boolean enabled) {
            this.id = id;
            this.username = username;
            this.displayName = displayName;
            this.passwordHash = passwordHash;
            this.role = role;
            this.enabled = enabled;
        }
    }
    private static final class Session {
        final long id;
        final String username;
        final String displayName;
        final String role;
        Session(long id, String username, String displayName, String role) {
            this.id = id;
            this.username = username;
            this.displayName = displayName;
            this.role = role;
        }
        boolean isAdmin() {
            return "ADMIN".equalsIgnoreCase(role);
        }
        String nameForPassword() {
            if (displayName != null && !displayName.isBlank()) return displayName;
            return username;
        }
    }
    private void run() {
        printBanner();
        while (true) {
            io.blank();
            io.println("【最初の画面】");
            io.println("1) ログイン");
            io.println("2) パスワードを忘れた（初期化依頼）");
            io.println("0) 終了");
            int sel = io.readIntInRange("選択: ", 0, 2, -1);
            if (sel == 0) {
                io.println("終了します。");
                return;
            } else if (sel == 1) {
                loginFlow();
            } else if (sel == 2) {
                passwordResetRequestBeforeLogin();
            }
        }
    }
    private void printBanner() {
        io.println("====================================");
        io.println(" 従業員管理システム（Javaコンソール操作）");
        io.println("====================================");
        io.println("※注意: パスワードは表示されます（周囲に注意）");
        io.println("※CLI文字コード: " + io.getCharset().displayName());
    }
    private void loginFlow() {
        io.blank();
        io.println("【ログイン】（Enterで戻る）");
        String username = io.readLine("ユーザー名: ");
        if (username == null || username.isBlank()) return;
        String password = io.readLine("パスワード（表示されます）: ");
        if (password == null) return;
        io.println("入力したパスワード: " + password);
        io.println("1) これでOK");
        io.println("2) 入れ直す");
        io.println("0) 戻る");
        int ok = io.readIntInRange("選択: ", 0, 2, 0);
        if (ok == 0) return;
        if (ok == 2) {
            loginFlow();
            return;
        }
        DbUser u = findUserByUsername(username.trim());
        if (u == null) {
            io.println("ログイン失敗: ユーザーが見つかりません");
            io.pause();
            return;
        }
        if (!u.enabled) {
            io.println("ログイン失敗: ユーザーが無効です");
            io.pause();
            return;
        }
        if (!passwordEncoder.matches(password, u.passwordHash)) {
            io.println("ログイン失敗: パスワードが違います");
            io.pause();
            return;
        }
        this.session = new Session(u.id, u.username, u.displayName, u.role);
        audit("LOGIN", "users", String.valueOf(u.id), "username=" + u.username);
        io.println("ログイン成功（権限: " + (session.isAdmin() ? "管理者" : "従業員") + "）");
        io.pause("Enterで戻る...");
        if (session.isAdmin()) {
            int pending = countPendingPasswordReset();
            if (pending > 0) {
                io.blank();
                io.println("【通知】未処理のパスワード初期化依頼があります: " + pending + "件");
                io.pause("Enterで続行...");
            }
            adminMenu();
        } else {
            employeeMenu();
        }
        this.session = null;
    }
    private void adminMenu() {
        while (true) {
            io.blank();
            io.println("=== 【管理者メニュー】 ===");
            io.println("1) 自分の情報");
            io.println("2) 勤怠（出勤/退勤/最近）");
            io.println("3) 職員管理（追加/一覧/退職/削除）");
            io.println("4) 申請管理（未処理/詳細/承認/却下）");
            io.println("5) 申請（管理者）");
            io.println("6) メッセージ（受信/送信/既読）");
            io.println("7) メンタル閲覧（ユーザー指定）");
            io.println("8) 監査ログ（最新）");
            io.println("9) パスワード初期化依頼（未処理/承認/却下）");
            io.println("10) パスワード変更");
            io.println("11) 自分のメンタル記録（今日）");
            io.println("0) ログアウト（戻る）");
            int sel = io.readIntInRange("選択: ", 0, 11, -1);
            if (sel == 0) return;
            switch (sel) {
                case 1 -> showSelfInfo();
                case 2 -> attendanceMenu();
                case 3 -> staffManagementMenu();
                case 4 -> requestManagementMenu();
                case 5 -> createRequestMenu(true);
                case 6 -> messageMenu();
                case 7 -> mentalViewByUserMenu();
                case 8 -> showAuditLogs();
                case 9 -> passwordResetAdminMenu();
                case 10 -> changePasswordMenu();
                case 11 -> upsertMyMentalToday();
                default -> {}
            }
        }
    }
    private void employeeMenu() {
        while (true) {
            io.blank();
            io.println("=== 【従業員メニュー】 ===");
            io.println("1) 自分の情報");
            io.println("2) 勤怠（出勤/退勤/最近）");
            io.println("3) 申請（作成）");
            io.println("4) メッセージ（受信/送信/既読）");
            io.println("5) 自分のメンタル記録（今日）");
            io.println("6) パスワード変更");
            io.println("0) ログアウト（戻る）");
            int sel = io.readIntInRange("選択: ", 0, 6, -1);
            if (sel == 0) return;
            switch (sel) {
                case 1 -> showSelfInfo();
                case 2 -> attendanceMenu();
                case 3 -> createRequestMenu(false);
                case 4 -> messageMenu();
                case 5 -> upsertMyMentalToday();
                case 6 -> changePasswordMenu();
                default -> {}
            }
        }
    }
    private void showSelfInfo() {
        io.blank();
        DbUser u = findUserById(session.id);
        if (u == null) {
            io.println("ユーザー情報が取得できません。");
            io.pause();
            return;
        }
        io.println("【自分の情報】");
        io.println("ユーザーID: " + u.id);
        io.println("ユーザー名: " + u.username);
        io.println("表示名: " + (u.displayName == null ? "なし" : u.displayName));
        io.println("権限: " + u.role);
        io.println("状態: " + (u.enabled ? "有効" : "無効"));
        io.pause();
    }
    private void attendanceMenu() {
        while (true) {
            io.blank();
            io.println("=== 【勤怠（" + (session.isAdmin() ? "管理者" : "従業員") + "）】 ===");
            io.println("1) 出勤（打刻）");
            io.println("2) 退勤（打刻）");
            io.println("3) 最近の勤怠を見る");
            io.println("0) 戻る");
            int sel = io.readIntInRange("選択: ", 0, 3, -1);
            if (sel == 0) return;
            switch (sel) {
                case 1 -> clockIn();
                case 2 -> clockOut();
                case 3 -> showRecentAttendance();
                default -> {}
            }
        }
    }
    private void clockIn() {
        LocalDate today = LocalDate.now();
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM attendance_records WHERE user_id=? AND work_date=?",
                Integer.class, session.id, Date.valueOf(today)
        );
        if (count != null && count > 0) {
            Timestamp existing = jdbc.queryForObject(
                    "SELECT clock_in FROM attendance_records WHERE user_id=? AND work_date=?",
                    Timestamp.class, session.id, Date.valueOf(today)
            );
            if (existing != null) {
                io.println("すでに出勤済みです: " + existing);
                io.pause();
                return;
            }
            jdbc.update(
                    "UPDATE attendance_records SET clock_in=?, updated_at=? WHERE user_id=? AND work_date=?",
                    now, now, session.id, Date.valueOf(today)
            );
        } else {
            jdbc.update(
                    "INSERT INTO attendance_records (user_id, work_date, clock_in, clock_out, created_at, updated_at) VALUES (?,?,?,?,?,?)",
                    session.id, Date.valueOf(today), now, null, now, now
            );
        }
        audit("CLOCK_IN", "attendance_records", session.id + ":" + today, null);
        io.println("出勤しました: " + now);
        io.pause();
    }
    private void clockOut() {
        LocalDate today = LocalDate.now();
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());
        try {
            Timestamp clockIn = jdbc.queryForObject(
                    "SELECT clock_in FROM attendance_records WHERE user_id=? AND work_date=?",
                    Timestamp.class, session.id, Date.valueOf(today)
            );
            if (clockIn == null) {
                io.println("まだ出勤していません。");
                io.pause();
                return;
            }
            Timestamp clockOut = jdbc.queryForObject(
                    "SELECT clock_out FROM attendance_records WHERE user_id=? AND work_date=?",
                    Timestamp.class, session.id, Date.valueOf(today)
            );
            if (clockOut != null) {
                io.println("すでに退勤済みです: " + clockOut);
                io.pause();
                return;
            }
            jdbc.update(
                    "UPDATE attendance_records SET clock_out=?, updated_at=? WHERE user_id=? AND work_date=?",
                    now, now, session.id, Date.valueOf(today)
            );
            audit("CLOCK_OUT", "attendance_records", session.id + ":" + today, null);
            io.println("退勤しました: " + now);
            io.pause();
        } catch (EmptyResultDataAccessException e) {
            io.println("今日の勤怠がありません（先に出勤してください）。");
            io.pause();
        }
    }
    private void showRecentAttendance() {
        io.blank();
        io.println("【最近の勤怠】");
        List<String> rows = jdbc.query(
                "SELECT work_date, clock_in, clock_out FROM attendance_records WHERE user_id=? ORDER BY work_date DESC LIMIT 10",
                (rs, rn) -> {
                    Date d = rs.getDate("work_date");
                    Timestamp in = rs.getTimestamp("clock_in");
                    Timestamp out = rs.getTimestamp("clock_out");
                    return String.format("%s  出:%s  退:%s",
                            d,
                            (in == null ? "-" : in),
                            (out == null ? "-" : out));
                },
                session.id
        );
        if (rows.isEmpty()) {
            io.println("勤怠がありません。");
        } else {
            for (String r : rows) io.println(r);
        }
        io.pause();
    }
    private void staffManagementMenu() {
        while (true) {
            io.blank();
            io.println("=== 【職員管理（管理者）】 ===");
            io.println("1) 職員を追加（初期パスワード＝ユーザー名）");
            io.println("2) 職員一覧");
            io.println("3) 退職（利用停止）");
            io.println("4) 完全削除（履歴ゼロのみ）");
            io.println("0) 戻る");
            int sel = io.readIntInRange("選択: ", 0, 4, -1);
            if (sel == 0) return;
            switch (sel) {
                case 1 -> createUserMenu();
                case 2 -> listUsersMenu();
                case 3 -> disableUserMenu();
                case 4 -> deleteUserMenu();
                default -> {}
            }
        }
    }
    private void createUserMenu() {
        io.blank();
        String username = io.readLine("ユーザー名（Enterで戻る）: ");
        if (username == null || username.isBlank()) return;
        String displayName = io.readLine("表示名（任意 / Enterで省略）: ");
        if (displayName != null && displayName.isBlank()) displayName = null;
        io.println("役割を選択してください:");
        io.println("1) 従業員（EMPLOYEE）");
        io.println("2) 管理者（ADMIN）");
        io.println("0) 戻る");
        int r = io.readIntInRange("選択: ", 0, 2, 0);
        if (r == 0) return;
        String role = (r == 2) ? "ADMIN" : "EMPLOYEE";
        io.blank();
        io.println("この内容で職員を追加しますか？");
        io.println("ユーザー名: " + username);
        io.println("表示名: " + (displayName == null ? "なし" : displayName));
        io.println("役割: " + role);
        io.println("1) 実行");
        io.println("0) 戻る");
        int ok = io.readIntInRange("選択: ", 0, 1, 0);
        if (ok == 0) return;
        try {
            Timestamp now = Timestamp.valueOf(LocalDateTime.now());
            String hash = passwordEncoder.encode(username);
            jdbc.update(
                    "INSERT INTO users (username, display_name, password_hash, role, enabled, created_at, updated_at) VALUES (?,?,?,?,?,?,?)",
                    username, displayName, hash, role, true, now, now
            );
            Long newId = jdbc.queryForObject("SELECT id FROM users WHERE username=?", Long.class, username);
            audit("CREATE_USER", "users", String.valueOf(newId), "username=" + username + ", role=" + role);
            io.blank();
            io.println("----- 結果 -----");
            io.println("ステータス: 200");
            io.println("完了: ユーザーを作成しました（初期パスワードはユーザー名と同じです）");
            io.println("ユーザーID: " + newId + " / ユーザー名: " + username + " / 表示名: " + (displayName == null ? "なし" : displayName)
                    + " / 権限: " + (role.equals("ADMIN") ? "管理者" : "従業員") + " / 状態: 有効");
        } catch (Exception e) {
            io.blank();
            io.println("----- 結果 -----");
            io.println("ステータス: 400");
            io.println("失敗: " + e.getMessage());
        }
        io.pause();
    }
    private void listUsersMenu() {
    io.blank();
    io.println("【職員一覧】");
    List<Map<String, Object>> rows =
            jdbc.queryForList("SELECT id, username, role, enabled FROM users ORDER BY id");
    for (Map<String, Object> row : rows) {
        long id = ((Number) row.get("id")).longValue();
        String username = String.valueOf(row.get("username"));
        String role = String.valueOf(row.get("role"));
        Object e = row.get("enabled");
        boolean enabled;
        if (e instanceof Boolean b) {
            enabled = b;
        } else if (e instanceof Number n) {
            enabled = n.intValue() != 0;
        } else {
            enabled = e != null && Boolean.parseBoolean(e.toString());
        }
        io.println(String.format(
                "ID:%d / username:%s / role:%s / %s",
                id, username, role, (enabled ? "有効" : "無効")
        ));
    }
    io.pause();
}
    private void disableUserMenu() {
        io.blank();
        String s = io.readLine("利用停止にするユーザーID: ");
        if (s == null || s.isBlank()) return;
        long id;
        try { id = Long.parseLong(s.trim()); } catch (Exception e) { io.println("数字で入力してください"); io.pause(); return; }
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());
        int updated = jdbc.update("UPDATE users SET enabled=?, updated_at=? WHERE id=?", false, now, id);
        if (updated == 0) {
            io.println("対象ユーザーが見つかりません。");
        } else {
            audit("DISABLE_USER", "users", String.valueOf(id), null);
            io.println("利用停止にしました。");
        }
        io.pause();
    }
    private void deleteUserMenu() {
        io.blank();
        String s = io.readLine("完全削除するユーザーID（履歴ゼロのみ）: ");
        if (s == null || s.isBlank()) return;
        long id;
        try { id = Long.parseLong(s.trim()); } catch (Exception e) { io.println("数字で入力してください"); io.pause(); return; }
        if (id == session.id) {
            io.println("自分自身は削除できません。");
            io.pause();
            return;
        }
        if (!isDeletableUser(id)) {
            io.println("削除不可: 勤怠/申請/メッセージ/メンタル/監査などの履歴が存在します。");
            io.pause();
            return;
        }
        io.println("本当に削除しますか？ 1) はい / 0) いいえ");
        int ok = io.readIntInRange("選択: ", 0, 1, 0);
        if (ok == 0) return;
        int deleted = jdbc.update("DELETE FROM users WHERE id=?", id);
        if (deleted == 0) {
            io.println("対象ユーザーが見つかりません。");
        } else {
            audit("DELETE_USER", "users", String.valueOf(id), null);
            io.println("削除しました。");
        }
        io.pause();
    }
    private boolean isDeletableUser(long userId) {
    String[] sqls = new String[] {
            "SELECT COUNT(*) FROM attendance_records WHERE user_id=?",
            "SELECT COUNT(*) FROM application_requests WHERE requester_id=? OR decided_by_id=?",
            "SELECT COUNT(*) FROM messages WHERE sender_id=? OR receiver_id=?",
            "SELECT COUNT(*) FROM mental_checkins WHERE user_id=?",
            "SELECT COUNT(*) FROM audit_logs WHERE actor_id=?",
            "SELECT COUNT(*) FROM password_reset_request WHERE target_user_id=? OR requested_by_id=? OR decided_by_id=?"
    };
    for (String q : sqls) {
        int placeholders = countPlaceholders(q);
        Object[] args;
        if (placeholders == 1) {
            args = new Object[] { userId };
        } else if (placeholders == 2) {
            args = new Object[] { userId, userId };
        } else if (placeholders == 3) {
            args = new Object[] { userId, userId, userId };
        } else {
            throw new IllegalStateException("想定外のプレースホルダ数(" + placeholders + "): " + q);
        }
        Integer c = jdbc.queryForObject(q, Integer.class, args);
        int count = (c == null) ? 0 : c;
        if (count > 0) return false; 
    }
    return true; 
}
private int countPlaceholders(String sql) {
    int n = 0;
    for (int i = 0; i < sql.length(); i++) {
        if (sql.charAt(i) == '?') n++;
    }
    return n;
}
    private void createRequestMenu(boolean isAdmin) {
    io.blank();
    io.println("=== 【申請（" + (isAdmin ? "管理者" : "従業員") + "）】 ===");
    io.println("1) 休暇申請（期間）");
    io.println("2) 残業申請（分）");
    io.println("3) シフト変更申請（文字）");
    if (isAdmin) {
        io.println("4) 自分の申請状況（一覧）");
    }
    io.println("0) 戻る");
    int max = isAdmin ? 4 : 3;
    int sel = io.readIntInRange("選択: ", 0, max, 0);
    if (sel == 0) return;
    if (isAdmin && sel == 4) {
        showMyRequests();
        return;
    }
    String type;
    Date startDate = null;
    Date endDate = null;
    Date targetDate = null;
    Integer overtime = null;
    String requestedShift = null;
    String reason = null;
    if (sel == 1) {
        type = "LEAVE";
        String leaveLabel = selectLeaveCategoryLabel();
        if (leaveLabel == null) return; 
        startDate = readSqlDate("開始日(yyyy-mm-dd): ");
        endDate = readSqlDate("終了日(yyyy-mm-dd): ");
        String userReason = io.readLine("理由（任意）: ");
        reason = buildLeaveReason(leaveLabel, userReason);
    } else if (sel == 2) {
        type = "OVERTIME";
        targetDate = readSqlDate("対象日(yyyy-mm-dd): ");
        String m = io.readNonEmpty("残業分（数字）: ");
        overtime = Integer.parseInt(m.trim());
        String r = io.readLine("理由（任意）: ");
        reason = normalizeBlankToNull(r);
    } else {
        type = "SHIFT_CHANGE";
        targetDate = readSqlDate("対象日(yyyy-mm-dd): ");
        requestedShift = io.readNonEmpty("希望シフト（例: 9-18）: ");
        String r = io.readLine("理由（任意）: ");
        reason = normalizeBlankToNull(r);
    }
    Timestamp now = Timestamp.valueOf(LocalDateTime.now());
    jdbc.update(
            "INSERT INTO application_requests (type, requester_id, status, start_date, end_date, target_date, overtime_minutes, requested_shift, reason, created_at, updated_at) " +
                    "VALUES (?,?,?,?,?,?,?,?,?,?,?)",
            type, session.id, "PENDING",
            startDate, endDate, targetDate, overtime, requestedShift, reason,
            now, now
    );
    audit("CREATE_REQUEST", "application_requests", null, "type=" + type);
    io.println("申請を作成しました（未処理）。");
    io.pause();
}
private String selectLeaveCategoryLabel() {
    io.blank();
    io.println("休暇種別を選択してください:");
    io.println("1) 有給休暇");
    io.println("2) 特別休暇（忌引き）");
    io.println("3) 特別休暇（結婚）");
    io.println("4) 特別休暇（その他）");
    io.println("0) 戻る");
    int sel = io.readIntInRange("選択: ", 0, 4, 0);
    if (sel == 0) return null;
    return switch (sel) {
        case 1 -> "有給休暇";
        case 2 -> "特別休暇:忌引き";
        case 3 -> "特別休暇:結婚";
        case 4 -> "特別休暇:その他";
        default -> null;
    };
}
private String buildLeaveReason(String leaveLabel, String userReason) {
    String prefix = "[休暇:" + leaveLabel + "]";
    if (userReason == null || userReason.isBlank()) return prefix;
    return prefix + " " + userReason.trim();
}
private String normalizeBlankToNull(String s) {
    if (s == null) return null;
    if (s.isBlank()) return null;
    return s.trim();
}
    private void requestManagementMenu() {
        while (true) {
            io.blank();
            io.println("=== 【申請管理（管理者）】 ===");
            io.println("1) 未処理一覧");
            io.println("2) 詳細（依頼ID指定）");
            io.println("3) 承認（依頼ID指定）");
            io.println("4) 却下（依頼ID指定）");
            io.println("0) 戻る");
            int sel = io.readIntInRange("選択: ", 0, 4, 0);
            if (sel == 0) return;
            switch (sel) {
                case 1 -> listPendingRequests();
                case 2 -> showRequestDetail();
                case 3 -> decideRequest(true);
                case 4 -> decideRequest(false);
                default -> {}
            }
        }
    }
    private void listPendingRequests() {
        io.blank();
        List<String> rows = jdbc.query(
                "SELECT r.id, r.type, u.username, u.display_name, r.created_at FROM application_requests r " +
                        "JOIN users u ON r.requester_id=u.id WHERE r.status='PENDING' ORDER BY r.created_at DESC LIMIT 50",
                (rs, rn) -> String.format("ID:%d / type:%s / requester:%s(%s) / at:%s",
                        rs.getLong("id"),
                        rs.getString("type"),
                        rs.getString("username"),
                        (rs.getString("display_name") == null ? "-" : rs.getString("display_name")),
                        rs.getTimestamp("created_at"))
        );
        if (rows.isEmpty()) io.println("未処理はありません。");
        for (String r : rows) io.println(r);
        io.pause();
    }
    private void showRequestDetail() {
        io.blank();
        String s = io.readLine("依頼ID: ");
        if (s == null || s.isBlank()) return;
        long id = Long.parseLong(s.trim());
        try {
            String detail = jdbc.queryForObject(
                    "SELECT r.id, r.type, r.status, u.username, u.display_name, r.start_date, r.end_date, r.target_date, r.overtime_minutes, r.requested_shift, r.reason, r.created_at " +
                            "FROM application_requests r JOIN users u ON r.requester_id=u.id WHERE r.id=?",
                    (rs, rn) -> {
                        return "ID=" + rs.getLong("id")
                                + "\ntype=" + rs.getString("type")
                                + "\nstatus=" + rs.getString("status")
                                + "\nrequester=" + rs.getString("username") + "(" + (rs.getString("display_name") == null ? "-" : rs.getString("display_name")) + ")"
                                + "\nstart=" + rs.getDate("start_date")
                                + "\nend=" + rs.getDate("end_date")
                                + "\ntarget=" + rs.getDate("target_date")
                                + "\novertime=" + rs.getObject("overtime_minutes")
                                + "\nshift=" + rs.getString("requested_shift")
                                + "\nreason=" + rs.getString("reason")
                                + "\ncreated=" + rs.getTimestamp("created_at");
                    },
                    id
            );
            io.println(detail);
        } catch (EmptyResultDataAccessException e) {
            io.println("見つかりません。");
        }
        io.pause();
    }
    private void decideRequest(boolean approve) {
    io.blank();
    String s = io.readLine("依頼ID: ");
    if (s == null || s.isBlank()) return;
    long id;
    try {
        id = Long.parseLong(s.trim());
    } catch (NumberFormatException e) {
        io.println("IDは数字で入力してください。");
        io.pause();
        return;
    }
    record ReqInfo(long requesterId, String status) {}
    ReqInfo req;
    try {
        req = jdbc.queryForObject(
                "SELECT requester_id, status FROM application_requests WHERE id=?",
                (rs, rowNum) -> new ReqInfo(rs.getLong("requester_id"), rs.getString("status")),
                id
        );
    } catch (org.springframework.dao.EmptyResultDataAccessException e) {
        io.println("更新できません（そのIDの申請が存在しません）。");
        io.pause();
        return;
    }
    if (req.requesterId == session.id) {
        io.println("自分の申請は自分で承認/却下できません。別の管理者で処理してください。");
        io.pause();
        return;
    }
    if (!"PENDING".equals(req.status)) {
        io.println("更新できません（未処理ではありません / 現在: " + req.status + "）。");
        io.pause();
        return;
    }
    String note = io.readLine("コメント（任意）: ");
    if (note != null && note.isBlank()) note = null;
    Timestamp now = Timestamp.valueOf(LocalDateTime.now());
    int updated = jdbc.update(
            "UPDATE application_requests " +
                    "SET status=?, decided_by_id=?, decided_at=?, decision_note=?, updated_at=? " +
                    "WHERE id=? AND status='PENDING' AND requester_id<>?",
            approve ? "APPROVED" : "REJECTED",
            session.id, now, note, now,
            id, session.id
    );
    if (updated == 0) {
        io.println("更新できません（自分の申請 / すでに処理済み / IDが違う可能性）。");
    } else {
        audit(approve ? "APPROVE_REQUEST" : "REJECT_REQUEST", "application_requests", String.valueOf(id), note);
        io.println(approve ? "承認しました。" : "却下しました。");
    }
    io.pause();
}
private void showMyRequests() {
    io.blank();
    io.println("=== 【自分の申請状況】 ===");
    Integer pending = jdbc.queryForObject(
            "SELECT COUNT(*) FROM application_requests WHERE requester_id=? AND status='PENDING'",
            Integer.class, session.id
    );
    Integer approved = jdbc.queryForObject(
            "SELECT COUNT(*) FROM application_requests WHERE requester_id=? AND status='APPROVED'",
            Integer.class, session.id
    );
    Integer rejected = jdbc.queryForObject(
            "SELECT COUNT(*) FROM application_requests WHERE requester_id=? AND status='REJECTED'",
            Integer.class, session.id
    );
    io.println("未処理: " + (pending == null ? 0 : pending)
            + " / 承認: " + (approved == null ? 0 : approved)
            + " / 却下: " + (rejected == null ? 0 : rejected));
    io.println("");
    record MyReqRow(
            long id,
            String type,
            String status,
            java.sql.Date startDate,
            java.sql.Date endDate,
            java.sql.Date targetDate,
            Integer overtimeMinutes,
            String requestedShift,
            String reason,
            java.sql.Timestamp createdAt,
            Long decidedById,
            java.sql.Timestamp decidedAt,
            String decisionNote
    ) {}
    final String sql =
            "SELECT id, type, status, start_date, end_date, target_date, overtime_minutes, requested_shift, reason, created_at, " +
            "       decided_by_id, decided_at, decision_note " +
            "FROM application_requests " +
            "WHERE requester_id=? " +
            "ORDER BY created_at DESC, id DESC";
    java.util.List<MyReqRow> rows = jdbc.query(
            sql,
            (rs, rowNum) -> {
                long tmp = rs.getLong("decided_by_id");
                Long decidedById = rs.wasNull() ? null : tmp;
                Integer overtimeMinutes = (Integer) rs.getObject("overtime_minutes");
                return new MyReqRow(
                        rs.getLong("id"),
                        rs.getString("type"),
                        rs.getString("status"),
                        rs.getDate("start_date"),
                        rs.getDate("end_date"),
                        rs.getDate("target_date"),
                        overtimeMinutes,
                        rs.getString("requested_shift"),
                        rs.getString("reason"),
                        rs.getTimestamp("created_at"),
                        decidedById,
                        rs.getTimestamp("decided_at"),
                        rs.getString("decision_note")
                );
            },
            session.id
    );
    if (rows.isEmpty()) {
        io.println("申請はありません。");
        io.pause();
        return;
    }
    for (MyReqRow r : rows) {
        io.println("----------------------------------------");
        io.println("ID: " + r.id());
        io.println("種別: " + labelRequestType(r.type()));
        io.println("対象/期間: " + formatRequestTarget(
                r.type(), r.startDate(), r.endDate(), r.targetDate(), r.overtimeMinutes(), r.requestedShift()
        ));
        io.println("状況: " + labelRequestStatus(r.status()));
        if (r.reason() != null && !r.reason().isBlank()) {
            io.println("理由: " + r.reason());
        }
        if (r.createdAt() != null) {
            io.println("申請日: " + r.createdAt());
        }
        if (r.status() != null && !"PENDING".equals(r.status())) {
            io.println("処理者ID: " + (r.decidedById() == null ? "-" : r.decidedById()));
            io.println("処理日時: " + (r.decidedAt() == null ? "-" : r.decidedAt()));
            if (r.decisionNote() != null && !r.decisionNote().isBlank()) {
                io.println("コメント: " + r.decisionNote());
            }
        }
    }
    io.println("----------------------------------------");
    io.pause();
}
private String labelRequestType(String type) {
    if (type == null) return "-";
    return switch (type) {
        case "LEAVE" -> "休暇";
        case "OVERTIME" -> "残業";
        case "SHIFT_CHANGE" -> "シフト変更";
        default -> type;
    };
}
private String labelRequestStatus(String status) {
    if (status == null) return "-";
    return switch (status) {
        case "PENDING" -> "未処理";
        case "APPROVED" -> "承認";
        case "REJECTED" -> "却下";
        default -> status;
    };
}
private String formatRequestTarget(
        String type,
        java.sql.Date startDate,
        java.sql.Date endDate,
        java.sql.Date targetDate,
        Integer overtimeMinutes,
        String requestedShift
) {
    if (type == null) return "-";
    if (type.startsWith("LEAVE")) {
        return safeDate(startDate) + " ~ " + safeDate(endDate);
    }
    if ("OVERTIME".equals(type)) {
        String min = (overtimeMinutes == null ? "-" : String.valueOf(overtimeMinutes));
        return safeDate(targetDate) + " (" + min + "分)";
    }
    if ("SHIFT_CHANGE".equals(type)) {
        return safeDate(targetDate) + " (希望: " + safeText(requestedShift) + ")";
    }
    return "-";
}
private String safeDate(java.sql.Date d) {
    return d == null ? "-" : d.toString();
}
private String safeText(String s) {
    return (s == null || s.isBlank()) ? "-" : s;
}
private void messageMenu() {
    while (true) {
        io.blank();
        io.println("=== 【メッセージ】 ===");
        io.println("1) 受信一覧");
        io.println("2) 送信一覧");
        io.println("3) 送信");
        io.println("4) 既読にする（メッセージID指定）");
        io.println("0) 戻る");

        int sel = io.readIntInRange("選択: ", 0, 4, 0);
        if (sel == 0) return;

        if (sel == 1) {
            inbox();
        } else if (sel == 2) {
            listSentMessagesMenu();
        } else if (sel == 3) {
            sendMessage();  
        } else {
            markRead();     
        }
    }
}
private void listSentMessagesMenu() {
    io.blank();
    io.println("【送信一覧】");
    List<String> rows = jdbc.query(
            "SELECT m.id, ru.username AS receiver, ru.display_name AS receiver_display, " +
                    "m.body, m.sent_at, m.read_at, m.receiver_id " +
                    "FROM messages m LEFT JOIN users ru ON m.receiver_id=ru.id " +
                    "WHERE m.sender_id=? " +
                    "ORDER BY m.sent_at DESC LIMIT 50",
            (rs, rn) -> {
                String receiverUsername = rs.getString("receiver");
                String receiverDisplay  = rs.getString("receiver_display");
                long receiverId = rs.getLong("receiver_id");
                String to;
                if (receiverUsername == null) {
                    to = "(id=" + receiverId + ")";
                } else {
                    to = receiverUsername + "(" + (receiverDisplay == null ? "-" : receiverDisplay) + ")";
                }
                Timestamp readAt = rs.getTimestamp("read_at");
                String read = (readAt == null) ? "未読" : ("既読@" + readAt);
                return "ID:" + rs.getLong("id")
                        + " / " + read
                        + " / To:" + to
                        + " / At:" + rs.getTimestamp("sent_at")
                        + "\n" + rs.getString("body");
            },
            session.id
    );
    if (rows.isEmpty()) io.println("送信したメッセージはありません。");
    for (String r : rows) {
        io.println("----------------------------------");
        io.println(r);
    }
    io.pause();
}
    private void inbox() {
        io.blank();
        List<String> rows = jdbc.query(
                "SELECT m.id, su.username AS sender, su.display_name AS sender_display, m.body, m.sent_at, m.read_at " +
                        "FROM messages m JOIN users su ON m.sender_id=su.id WHERE m.receiver_id=? ORDER BY m.sent_at DESC LIMIT 50",
                (rs, rn) -> {
                    String from = rs.getString("sender") + "(" + (rs.getString("sender_display") == null ? "-" : rs.getString("sender_display")) + ")";
                    String read = (rs.getTimestamp("read_at") == null) ? "未読" : "既読";
                    return "ID:" + rs.getLong("id")
                            + " / " + read
                            + " / From:" + from
                            + " / At:" + rs.getTimestamp("sent_at")
                            + "\n" + rs.getString("body");
                },
                session.id
        );
        if (rows.isEmpty()) io.println("受信はありません。");
        for (String r : rows) {
            io.println("----------------------------------");
            io.println(r);
        }
        io.pause();
    }
    private void sendMessage() {
    io.blank();
    String to = io.readNonEmpty("送信先ユーザー名: ");
    DbUser target = findUserByUsername(to);
    if (target == null) {
        io.println("送信先が見つかりません。");
        io.pause();
        return;
    }
    String body = io.readNonEmpty("本文: ");
    if (!requireMyPasswordFor("メッセージ送信")) {
        return;
    }
    Timestamp now = Timestamp.valueOf(LocalDateTime.now());
    jdbc.update(
            "INSERT INTO messages (sender_id, receiver_id, body, sent_at, read_at) VALUES (?,?,?,?,?)",
            session.id, target.id, body, now, null
    );
    audit("SEND_MESSAGE", "messages", null, "to=" + target.username);
    io.println("送信しました。");
    io.pause();
}
private String loadCurrentUserPasswordHash() {
    try {
        String v = jdbc.queryForObject("SELECT password FROM users WHERE id=?", String.class, session.id);
        if (v != null && !v.isBlank()) return v;
    } catch (Exception ignored) {}
    try {
        String v = jdbc.queryForObject("SELECT password_hash FROM users WHERE id=?", String.class, session.id);
        if (v != null && !v.isBlank()) return v;
    } catch (Exception ignored) {}
    try {
        String v = jdbc.queryForObject("SELECT password_digest FROM users WHERE id=?", String.class, session.id);
        if (v != null && !v.isBlank()) return v;
    } catch (Exception ignored) {}
    return null;
}
private boolean requireMyPasswordFor(String purposeLabel) {
    io.blank();
    io.println("【本人確認】" + purposeLabel);
    io.println("※確認のため、あなたのパスワードを入力してください（表示されます）");
    String pw = io.readLine("パスワード: ");
    if (pw == null || pw.isBlank()) {
        io.println("キャンセルしました。");
        io.pause();
        return false;
    }
    String hash = loadCurrentUserPasswordHash();
    if (hash == null) {
        io.println("パスワード情報を取得できません（usersテーブルの列名を確認してください）。");
        io.pause();
        return false;
    }
    boolean ok;
    try {
        ok = passwordEncoder.matches(pw, hash);
    } catch (Exception e) {
        io.println("パスワード照合に失敗しました: " + e.getMessage());
        io.pause();
        return false;
    }
    if (!ok) {
        io.println("パスワードが一致しません。送信を中止しました。");
        io.pause();
        return false;
    }
    return true;
}
    private void markRead() {
        io.blank();
        String s = io.readNonEmpty("既読にするメッセージID: ");
        long id = Long.parseLong(s.trim());
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());
        int updated = jdbc.update(
                "UPDATE messages SET read_at=? WHERE id=? AND receiver_id=?",
                now, id, session.id
        );
        if (updated == 0) {
            io.println("更新できません（IDが違う/受信者ではない）。");
        } else {
            audit("READ_MESSAGE", "messages", String.valueOf(id), null);
            io.println("既読にしました。");
        }
        io.pause();
    }
    private void upsertMyMentalToday() {
        io.blank();
        io.println("【今日のメンタル記録】");
        String sScore = io.readNonEmpty("スコア（例: 1〜10）: ");
        int score = Integer.parseInt(sScore.trim());
        String comment = io.readLine("コメント（任意）: ");
        if (comment != null && comment.isBlank()) comment = null;
        Date today = Date.valueOf(LocalDate.now());
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());
        Integer exists = jdbc.queryForObject(
                "SELECT COUNT(*) FROM mental_checkins WHERE user_id=? AND check_date=?",
                Integer.class, session.id, today
        );
        if (exists != null && exists > 0) {
            jdbc.update(
                    "UPDATE mental_checkins SET score=?, comment=?, updated_at=? WHERE user_id=? AND check_date=?",
                    score, comment, now, session.id, today
            );
            audit("UPDATE_MENTAL", "mental_checkins", session.id + ":" + today, null);
            io.println("更新しました。");
        } else {
            jdbc.update(
                    "INSERT INTO mental_checkins (user_id, check_date, score, comment, created_at, updated_at) VALUES (?,?,?,?,?,?)",
                    session.id, today, score, comment, now, now
            );
            audit("CREATE_MENTAL", "mental_checkins", session.id + ":" + today, null);
            io.println("記録しました。");
        }
        io.pause();
    }
    private void mentalViewByUserMenu() {
        io.blank();
        String u = io.readNonEmpty("閲覧するユーザー名: ");
        DbUser target = findUserByUsername(u);
        if (target == null) {
            io.println("見つかりません。");
            io.pause();
            return;
        }
        List<String> rows = jdbc.query(
                "SELECT check_date, score, comment FROM mental_checkins WHERE user_id=? ORDER BY check_date DESC LIMIT 30",
                (rs, rn) -> rs.getDate("check_date") + " / score=" + rs.getInt("score") + " / " + (rs.getString("comment") == null ? "" : rs.getString("comment")),
                target.id
        );
        io.println("【" + target.username + "(" + (target.displayName == null ? "-" : target.displayName) + ") のメンタル】");
        if (rows.isEmpty()) io.println("記録がありません。");
        for (String r : rows) io.println(r);
        io.pause();
    }
    private void showAuditLogs() {
        io.blank();
        List<String> rows = jdbc.query(
                "SELECT created_at, actor_username, actor_role, action, entity_type, entity_id, detail " +
                        "FROM audit_logs ORDER BY created_at DESC LIMIT 50",
                (rs, rn) -> rs.getTimestamp("created_at")
                        + " / " + rs.getString("actor_username")
                        + " / " + rs.getString("actor_role")
                        + " / " + rs.getString("action")
                        + " / " + rs.getString("entity_type")
                        + " / " + rs.getString("entity_id")
                        + " / " + rs.getString("detail")
        );
        if (rows.isEmpty()) io.println("監査ログがありません。");
        for (String r : rows) io.println(r);
        io.pause();
    }
    private void passwordResetRequestBeforeLogin() {
        io.blank();
        io.println("【パスワード初期化依頼】（Enterで戻る）");
        String username = io.readLine("ユーザー名: ");
        if (username == null || username.isBlank()) return;
        DbUser target = findUserByUsername(username.trim());
        if (target == null) {
            io.println("ユーザーが見つかりません。");
            io.pause();
            return;
        }
        Integer exists = jdbc.queryForObject(
                "SELECT COUNT(*) FROM password_reset_request WHERE target_user_id=? AND status='PENDING'",
                Integer.class, target.id
        );
        if (exists != null && exists > 0) {
            io.println("すでに未処理の依頼があります。管理者の承認をお待ちください。");
            io.pause();
            return;
        }
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());
        jdbc.update(
                "INSERT INTO password_reset_request (target_user_id, requested_by_id, status, requested_at, requested_ip) VALUES (?,?,?,?,?)",
                target.id, null, "PENDING", now, null
        );
        io.println("初期化依頼を受け付けました。管理者の承認をお待ちください。");
        io.pause();
    }
    private void passwordResetAdminMenu() {
        while (true) {
            io.blank();
            io.println("=== 【パスワード初期化依頼（管理者）】 ===");
            io.println("1) 未処理一覧");
            io.println("2) 承認（依頼ID指定）");
            io.println("3) 却下（依頼ID指定）");
            io.println("0) 戻る");
            int sel = io.readIntInRange("選択: ", 0, 3, 0);
            if (sel == 0) return;
            switch (sel) {
                case 1 -> listPendingPasswordReset();
                case 2 -> decidePasswordReset(true);
                case 3 -> decidePasswordReset(false);
                default -> {}
            }
        }
    }
    private void listPendingPasswordReset() {
        io.blank();
        List<String> rows = jdbc.query(
                "SELECT pr.id, tu.username AS target_username, tu.display_name AS target_display, pr.requested_at, ru.username AS requester_username " +
                        "FROM password_reset_request pr " +
                        "JOIN users tu ON pr.target_user_id=tu.id " +
                        "LEFT JOIN users ru ON pr.requested_by_id=ru.id " +
                        "WHERE pr.status='PENDING' ORDER BY pr.requested_at DESC LIMIT 50",
                (rs, rn) -> "依頼ID:" + rs.getLong("id")
                        + " / 対象:" + rs.getString("target_username") + "(" + (rs.getString("target_display") == null ? "-" : rs.getString("target_display")) + ")"
                        + " / 依頼者:" + (rs.getString("requester_username") == null ? "（ログイン前/本人）" : rs.getString("requester_username"))
                        + " / at:" + rs.getTimestamp("requested_at")
        );
        if (rows.isEmpty()) io.println("未処理はありません。");
        for (String r : rows) io.println(r);
        io.pause();
    }
    private void decidePasswordReset(boolean approve) {
        io.blank();
        String s = io.readNonEmpty("依頼ID: ");
        long reqId = Long.parseLong(s.trim());
        String note = io.readLine("コメント（任意）: ");
        if (note != null && note.isBlank()) note = null;
        Long targetUserId;
        try {
            targetUserId = jdbc.queryForObject(
                    "SELECT target_user_id FROM password_reset_request WHERE id=? AND status='PENDING'",
                    Long.class, reqId
            );
        } catch (EmptyResultDataAccessException e) {
            io.println("未処理の依頼が見つかりません。");
            io.pause();
            return;
        }
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());
        if (approve) {
            DbUser target = findUserById(targetUserId);
            if (target == null) {
                io.println("対象ユーザーが見つかりません。");
                io.pause();
                return;
            }
            String newPlain = "pp" + (target.displayName != null && !target.displayName.isBlank() ? target.displayName : target.username);
            String newHash = passwordEncoder.encode(newPlain);
            jdbc.update("UPDATE users SET password_hash=?, updated_at=? WHERE id=?", newHash, now, target.id);
            jdbc.update(
                    "UPDATE password_reset_request SET status=?, decided_by_id=?, decided_at=?, decided_ip=?, decision_note=? WHERE id=?",
                    "APPROVED", session.id, now, null, note, reqId
            );
            audit("APPROVE_PASSWORD_RESET", "password_reset_request", String.valueOf(reqId), "target=" + target.username);
            io.println("承認しました。初期化後パスワード: " + newPlain);
        } else {
            jdbc.update(
                    "UPDATE password_reset_request SET status=?, decided_by_id=?, decided_at=?, decided_ip=?, decision_note=? WHERE id=?",
                    "REJECTED", session.id, now, null, note, reqId
            );
            audit("REJECT_PASSWORD_RESET", "password_reset_request", String.valueOf(reqId), null);
            io.println("却下しました。");
        }
        io.pause();
    }
    private int countPendingPasswordReset() {
        Integer c = jdbc.queryForObject(
                "SELECT COUNT(*) FROM password_reset_request WHERE status='PENDING'",
                Integer.class
        );
        return c == null ? 0 : c;
    }
    private void changePasswordMenu() {
        io.blank();
        io.println("【パスワード変更】");
        String current = io.readNonEmpty("現在のパスワード（表示されます）: ");
        DbUser u = findUserById(session.id);
        if (u == null) {
            io.println("ユーザー情報が取得できません。");
            io.pause();
            return;
        }
        if (!passwordEncoder.matches(current, u.passwordHash)) {
            io.println("現在のパスワードが違います。");
            io.pause();
            return;
        }
        String next = io.readNonEmpty("新しいパスワード（表示されます）: ");
        String next2 = io.readNonEmpty("新しいパスワード（確認）: ");
        if (!next.equals(next2)) {
            io.println("確認が一致しません。");
            io.pause();
            return;
        }
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());
        String hash = passwordEncoder.encode(next);
        jdbc.update("UPDATE users SET password_hash=?, updated_at=? WHERE id=?", hash, now, session.id);
        audit("CHANGE_PASSWORD", "users", String.valueOf(session.id), null);
        io.println("変更しました。");
        io.pause();
    }
    private DbUser findUserByUsername(String username) {
        try {
            return jdbc.queryForObject(
                    "SELECT id, username, display_name, password_hash, role, enabled FROM users WHERE username=?",
                    (rs, rn) -> new DbUser(
                            rs.getLong("id"),
                            rs.getString("username"),
                            rs.getString("display_name"),
                            rs.getString("password_hash"),
                            rs.getString("role"),
                            rs.getBoolean("enabled")
                    ),
                    username
            );
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }
    private DbUser findUserById(long id) {
        try {
            return jdbc.queryForObject(
                    "SELECT id, username, display_name, password_hash, role, enabled FROM users WHERE id=?",
                    (rs, rn) -> new DbUser(
                            rs.getLong("id"),
                            rs.getString("username"),
                            rs.getString("display_name"),
                            rs.getString("password_hash"),
                            rs.getString("role"),
                            rs.getBoolean("enabled")
                    ),
                    id
            );
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }
    private Date readSqlDate(String prompt) {
        while (true) {
            String s = io.readNonEmpty(prompt);
            try {
                LocalDate d = LocalDate.parse(s.trim());
                return Date.valueOf(d);
            } catch (Exception e) {
                io.println("日付形式が不正です。例: 2025-12-28");
            }
        }
    }
    private void audit(String action, String entityType, String entityId, String detail) {
        try {
            Timestamp now = Timestamp.valueOf(LocalDateTime.now());
            jdbc.update(
                    "INSERT INTO audit_logs (actor_id, actor_username, actor_role, action, entity_type, entity_id, detail, ip_address, created_at) " +
                            "VALUES (?,?,?,?,?,?,?,?,?)",
                    session == null ? null : session.id,
                    session == null ? null : session.username,
                    session == null ? null : session.role,
                    action,
                    entityType,
                    entityId,
                    detail,
                    null,
                    now
            );
        } catch (Exception ignored) {
        }
    }
}