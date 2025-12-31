# EMS Console — 従業員管理システム（Java / Spring Boot）

コンソール（CLI）で動作する 社員管理システムです。  
勤怠打刻、申請（休暇/残業/シフト変更）、メッセージ（送信/受信/既読）、メンタル記録、監査ログ、パスワード初期化依頼などを、**Spring Boot + H2（ファイルDB）**で一体運用できます。

> ⚠️ 注意: 本プロジェクトのCLIは入力したパスワードが表示される仕様です（学習/演習用途想定）。周囲に注意して利用してください。

---

## 主な機能

- **ログイン / パスワード変更**
- **勤怠**：出勤・退勤の打刻、最近の勤怠表示
- **申請**：休暇（有給/特別）、残業、シフト変更
  - 管理者は **自分の申請状況**（未処理/承認/却下の確認）も表示可能
  - **管理者自身の申請は、別の管理者が承認/却下**する前提（自己承認不可）
- **申請管理（管理者）**：未処理一覧、詳細、承認/却下
- **メッセージ**：受信一覧 / 送信一覧 / 送信 / 既読
  - **送信時は本人確認（パスワード再入力）**を要求
  - 送信者は送信一覧で **既読状況（既読時刻）**を確認可能
- **メンタル記録**：日次スコア + コメント
- **監査ログ**：操作履歴の確認
- **パスワード初期化依頼**：ログイン前依頼 → 管理者が承認/却下

---

## 動作環境

- Windows 10/11
- PowerShell（推奨: Windows Terminal）
- Java **17+**（推奨: 21）
- Maven Wrapper同梱（`mvnw.cmd`）

---

## 起動（CLI）

プロジェクトルートで実行します。

### UTF-8（推奨）
```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\run-cli.ps1 -ConsoleEncoding UTF8
```

### CP932（従来のWindowsコンソール互換）
```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\run-cli.ps1 -ConsoleEncoding CP932
```

停止は `Ctrl + C`。

---

## データベース（H2）

- DBはファイルとして保存されます（例：`./data/ems-db`）
- **初期化したい場合**：アプリ停止後に `./data` を削除

---

## よくあるトラブル（重要）

### 「メイン・クラス.encoding=...」が見つからない
`java -Dfile.encoding=UTF-8 ...` の引数が壊れている典型例です。  
**`tools/run-cli.ps1` から起動**してください（引数を安全に渡す実装にしています）。

### 初回だけ時間がかかる
初回は `mvn compile` と依存解決が走るため時間がかかります。2回目以降は速くなります。

### OneDrive配下での運用
同期の競合やロックで不安定になることがあります。  
安定運用したい場合は、OneDrive外（例：`C:\dev\ems`）へ移動して開発するのがおすすめです。

---

## Gitに入れる（ローカルリポジトリ作成）

```powershell
cd "C:\Users\check\OneDrive\デスクトップ\社員管理システム\ems"

git init
git config core.longpaths true

# 推奨: 改行変換
git config core.autocrlf true

# 追加
git add .
git commit -m "Initial commit"
```

GitHubへPushする場合（例）：
```powershell
git branch -M main
git remote add origin <YOUR_REPOSITORY_URL>
git push -u origin main
```

---

## 免責・セキュリティ

- 本プロジェクトは学習/演習用途を想定しています。
- パスワードが表示される設計のため、業務利用・公開運用には適しません。

---

## ドキュメント

- `UserGuide.docx`：利用者向け操作説明書
- `TechnicalGuide.docx`：技術説明書（構成/DB/運用/注意点）

---

## License

必要に応じてMIT等のライセンスを設定してください（未設定の場合は、社内規定に従ってください）。
