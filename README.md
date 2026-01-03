# EMS（従業員管理システム / CLI）

Spring Boot（3.2.6）で動作する **コンソール型（CLI）の従業員管理システム** です。  
勤怠・申請・メッセージ・メンタル記録など、社内運用で頻出する機能を **シンプルに一通り** 実装しています。

---

## できること（主な機能）

- ログイン（管理者 / 従業員）
- 勤怠（出勤・退勤・最近の勤怠表示）
- 申請（休暇 / 残業 / シフト変更）
  - 管理者：未処理一覧・詳細確認・承認/却下  
  - ※運用ルール：**管理者は自分の申請を自分で承認/却下できません**
- メッセージ
  - 受信一覧 / 送信一覧（相手が既読にすると「既読@日時」表示）
  - 送信（送信前に本人パスワード確認）
  - 既読にする（メッセージID指定）
- メンタル記録
  - 従業員：自分のメンタル記録（今日）
  - 管理者：ユーザー指定の閲覧 + 自分のメンタル記録（今日）
- 管理者機能
  - 職員管理（追加 / 一覧 / 利用停止 / 履歴ゼロのみ削除）
  - 監査ログ閲覧
  - パスワード初期化依頼（承認 / 却下）
  - パスワード変更

---

## 技術スタック

- Java 17+（※環境によっては 21 でも動作）
- Spring Boot 3.2.6
- Spring Data JPA / Hibernate
- Spring Security（PasswordEncoder）
- Flyway（DBマイグレーション）
- H2 Database（ファイルDB）

---

## 起動方法（Windows / PowerShell）

プロジェクト直下（`pom.xml` がある場所）で実行します。

```powershell
# 1) ビルド
.\mvnw.cmd -DskipTests compile

# 2) CLI起動（推奨）
powershell -NoProfile -ExecutionPolicy Bypass -File .	ools
un-cli.ps1
```

> 文字化けが出る場合は Windows Terminal を推奨します。  
> それでも改善しない場合は `chcp 65001` でUTF-8に切り替えてください。

---

## パッケージ構成（推奨）

```
com.example.ems
├── EmsApplication.java          # Spring Boot エントリポイント
├── cli                          # コンソールUI（EmsConsoleCli, ConsoleIO）
├── config                       # 設定（例: PasswordConfig）
├── domain                       # エンティティ/ドメインモデル（User, Message, Request...）
├── repository                   # DBアクセス（Spring Data JPA 等）
├── service                      # ビジネスロジック
├── security                     # 認証/ハッシュ/権限制御
├── audit                        # 監査ログ
└── util                         # 共通関数（日時・文字処理）
src/main/resources
└── V1__init.sql                 # Flywayマイグレーション（推奨配置）
```

---

## ドキュメント

- **操作説明書（管理者/従業員）**: `UserGuide_v4.docx`

---

## ライセンス

学習用途/ポートフォリオ用途を想定しています。運用に転用する場合は、セキュリティ要件（権限・監査・ログ・入力制御）を見直してください。
