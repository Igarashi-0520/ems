# EMS（従業員管理システム / CLI）

Spring Boot（**3.2.6**）で動作する **コンソール型（CLI）** の従業員管理システムです。  
勤怠・申請・メッセージ・メンタル記録など、社内運用で頻出する機能を **一通り・シンプルに** 実装しています。

> **注意**: このCLIは、入力したパスワードを確認のために表示する設計になっています（学習/デモ用途）。実運用に転用する場合は「マスク入力」「監査・権限制御」「ログの取り扱い」を必ず見直してください。

---

## できること（主な機能）

### 共通（管理者 / 従業員）
- ログイン（管理者 / 従業員）
- 勤怠
  - 出勤（打刻） / 退勤（打刻） / 最近の勤怠表示
- 申請（作成）
  - 休暇申請（有給 / 特別休暇：忌引き・結婚・その他）
  - 残業申請（分）
  - シフト変更申請（文字）
- メッセージ
  - 受信一覧 / 送信一覧
  - 既読ボタン（メッセージID指定）
  - **送信前に本人パスワード確認**（なりすまし防止）
  - 相手が既読にすると送信一覧で **既読@日時** を表示
- メンタル記録
  - 自分のメンタル記録（今日）

### 管理者のみ
- 職員管理（追加 / 一覧 / 利用停止 / 履歴ゼロのみ削除）
- 申請管理（未処理一覧 / 詳細 / 承認 / 却下）  
  - **運用ルール**: 管理者は **自分の申請を自分で承認/却下できません**（別管理者が処理）
- メンタル閲覧（ユーザー指定） + 自分のメンタル記録（今日）
- 監査ログ閲覧
- パスワード初期化依頼（未処理一覧 / 承認 / 却下）
- パスワード変更

---

## 技術スタック
- Java **17+**（環境によっては 21 でも動作）
- Spring Boot **3.2.6**
- Spring Data JPA / Hibernate
- Spring Security（PasswordEncoder）
- Flyway（DBマイグレーション）
- H2 Database（ファイルDB）

---

## 起動方法（Windows / PowerShell）

> ✅ **プロジェクト直下（`pom.xml` がある場所）** で実行します。

### 1) ビルド（初回 or 変更後）
```powershell
.\mvnw.cmd -DskipTests compile
```

### 2) CLI起動（推奨：スクリプト）
```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\run-cli.ps1
```

### 文字化け対策（おすすめ）
- **Windows Terminal** 推奨（UTF-8が安定）
- それでも崩れる場合は、UTF-8を明示します（スクリプトのオプション）

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\run-cli.ps1 -ConsoleEncoding UTF8 -FileEncoding UTF8
```

> **重要**: コマンドをコピペした際、`-`（ハイフン）が全角/別記号に置き換わると、  
> `メイン・クラス.encoding=... を検出できません` のようなエラーになります。**半角の `-`** で入力してください。

---

## DBについて（H2 / ファイルDB）
- DBはローカルファイルとして保存されます（例：`./data/ems-db`）。
- 初期状態からやり直したい場合は、アプリ停止後にDBファイルを削除して再起動してください。

### ✅ GitにDBファイルを上げない（重要）
H2のDBファイル（例：`*.mv.db`）は **成果物ではなく実行時データ** なので、Git管理しないのがおすすめです。

`.gitignore` に以下を含める例：
```gitignore
# build
/target/

# runtime DB (H2)
*.mv.db
*.trace.db
/data/
```

---

## パッケージ構成（推奨）

> 既存コードはCLI寄りですが、運用/拡張を考えると **役割で分割** しておくと読みやすくなります。

```text
com.example.ems
├── EmsApplication.java          # Spring Boot エントリポイント
├── cli                          # コンソールUI（EmsConsoleCli, ConsoleIO）
├── config                       # 設定（例: PasswordConfig）
├── domain                       # エンティティ/ドメインモデル（User, Message, Request...）
├── repository                   # DBアクセス（Spring Data JPA 等）
└── service                      # ビジネスロジック

src/main/resources
└── db/migration
    └── V1__init.sql             # Flywayマイグレーション（推奨配置）
```

---

## 初期データについて
初期ユーザー/初期データは **FlywayのマイグレーションSQL** に置く想定です。  
（例：`src/main/resources/db/migration/V1__init.sql`）  
※もし既にDBファイルが生成されている場合は、SQLを変えても反映されないことがあるため、必要に応じてDBファイルを削除して作り直してください。

---

## よくある質問（突っ込まれやすい点）
- **Q. CLIなのにSpring Boot？**  
  A. DI・設定・DB接続・PasswordEncoder等を一体で扱えて、学習と保守に向くためです（`WebApplicationType.NONE` でCLI起動）。
- **Q. 休暇の「有給/特別」はDB的にどう表現？**  
  A. 本実装では `type=LEAVE` + `reason` 先頭に種別を付与する等、移行コストの低い表現を採用しています。将来は `leave_category` 列追加も検討可能です。
- **Q. パスワードが見えるのは危険では？**  
  A. その通りです。学習/デモ用途のため、実運用では「非表示入力」「ログ抑制」「監査」等を必須にします。

---

---

## ライセンス
学習用途/ポートフォリオ用途を想定しています。  
運用に転用する場合は、セキュリティ要件（権限・監査・ログ・入力制御）を必ず見直してください。
