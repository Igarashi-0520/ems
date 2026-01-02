# EMS（従業員管理システム）— Java / Spring Boot Console CLI

ローカル環境で動作する **従業員管理システム（EMS）** です。  
Spring Boot（WebApplicationType.NONE）で起動し、**コンソール（CLI）** から操作します。

---

## 1. 主な機能

- ログイン / パスワード変更
- 勤怠：出勤・退勤（打刻）/ 最近の勤怠表示
- 申請：
  - 休暇申請（期間）  
    - **有給休暇**
    - **特別休暇（忌引き / 結婚 / その他）**
  - 残業申請（分）
  - シフト変更申請（文字）
- 申請管理（管理者）：
  - 未処理一覧 / 詳細表示 / 承認 / 却下
  - **管理者が自分の申請を承認・却下できない**（別管理者の承認が必要）
  - 管理者は **自分の申請状況（一覧）** を確認可能
- メッセージ：
  - 受信一覧 / 送信一覧 / 既読（メッセージID指定）
  - **送信者も送信済みメッセージを一覧表示**でき、**既読時刻**も確認可能
  - 送信時に **本人確認（パスワード入力）** を必須化（従業員/管理者共通）
- メンタル記録（今日）/ 管理者による閲覧
- 監査ログ（最新）
- パスワード初期化依頼（未処理/承認/却下）

---

## 2. 動作環境（推奨）

- Windows 10/11
- Java 17+（手元で Java 21 でも動作確認）
- PowerShell 5.1+（または PowerShell 7+）
- Maven Wrapper 同梱（`mvnw.cmd`）

---

## 3. 起動方法（Windows / PowerShell）

### 3.1 推奨：run-cli.ps1 で起動

リポジトリ直下で実行します。

```powershell
cd "C:\path\to\ems"
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\run-cli.ps1
```

> うまく起動しない場合は、まず `cd` が **リポジトリ直下**（`pom.xml` がある場所）になっているか確認してください。

### 3.2 文字化け対策（おすすめ）

Windows Terminal を使う場合は UTF-8 が安定です。

```powershell
chcp 65001 | Out-Null
[Console]::InputEncoding  = [Text.Encoding]::UTF8
[Console]::OutputEncoding = [Text.Encoding]::UTF8
```

古いコンソールで CP932（Shift-JIS）を使う場合は次の通りです。

```powershell
chcp 932 | Out-Null
[Console]::InputEncoding  = [Text.Encoding]::GetEncoding(932)
[Console]::OutputEncoding = [Text.Encoding]::GetEncoding(932)
```

---

## 4. パッケージ構成

```
src/main/java/com/example/ems
├── EmsApplication.java          # Spring Boot エントリポイント
├── cli                          # コンソールUI（入出力・メニュー）
│   ├── EmsConsoleCli.java
│   └── ConsoleIO.java
├── config                       # 設定（例: PasswordConfig）
│   └── PasswordConfig.java
├── domain                       # エンティティ/ドメインモデル
│   ├── AuditLog.java
│   ├── Role.java
│   └── UserAccount.java
├── repository                   # DBアクセス（Spring Data JPA 等）
│   ├── UserRepository.java
│   └── AuditLogRepository.java
└── service                      # ビジネスロジック（ユースケース）
    └── AuditLogService.java

src/main/resources
└── V1__init.sql                 # DB初期化（Flyway マイグレーション）
```

- **cli**：画面（メニュー）とユーザー入出力。ビジネス判断は service に寄せるのが推奨。  
- **config**：Bean 定義やパスワードエンコーダ設定等。  
- **domain**：DBと対応するエンティティ、列挙型（Role 等）。  
- **repository**：DBアクセス。JPA Repository やカスタムクエリ。  
- **service**：ユースケース（承認、送信、監査など）を集約。  
- **resources**：SQL、設定ファイル等。

---

## 5. データベース

- H2（file）を使用します。
- 例：`./data/ems-db`（実際の設定は `application.properties` 等を参照）

---

## 6. Git / GitHub への公開(

### 6.1 Git の初期設定（最初に1回）

まず **自分の名前とメールアドレス** を設定します。

```powershell
git config --global user.name  "あなたの名前"
git config --global user.email "you@example.com"
git config --global --list
```

### 6.2 ローカルでコミットする

```powershell
cd "C:\path\to\ems"
git init
git add .
git commit -m "Initial commit"
```

### 6.3 GitHub にリポジトリを作成して push

GitHub 側で空のリポジトリを作成したら（例: `ems`）、表示される URL を remote に登録します。

```powershell
git branch -M main
git remote add origin https://github.com/Igarashi-0520/ems.git
git push -u origin main
```

> 2021年以降、GitHub はパスワード認証ではなく **Personal Access Token（PAT）** を使うのが一般的です。  
> push 時に求められる認証情報は「GitHub のユーザー名 + PAT」です。

---

## 7. よくあるトラブル

- **何も表示されない / すぐ閉じる**  
  - `cd` が間違っている（`pom.xml` の場所で実行する）
  - PowerShell の実行ポリシー（`-ExecutionPolicy Bypass` を付ける）
- **文字化けする**  
  - `chcp 65001`（UTF-8）または `chcp 932`（CP932）を試す
- **クラスパスが長すぎる**  
  - `tools/run-cli.ps1` を使う（コマンドを短くできる）

---

## License

学習・検証用途のサンプルとして提供しています（必要に応じて追記してください）。
