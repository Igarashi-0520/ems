param(
    [string]$BaseUrl = "http://localhost:8080",
    # UTF8: 文字化けを根本的に避けたい（推奨）
    # CP932: 従来のWindowsコンソール互換（環境によってはこちらが安定）
    [ValidateSet("UTF8", "CP932")]
    [string]$ConsoleEncoding = "UTF8",
    # 既に target\classes がある場合に高速起動したいなら付ける
    [switch]$NoRebuild
)
Set-StrictMode -Version 3.0
$ErrorActionPreference = "Stop"
function Fail([string]$msg) {
    throw $msg
}
function Set-ConsoleEncoding([string]$mode) {
    if ($mode -eq "CP932") {
        chcp 932 | Out-Null
        [Console]::InputEncoding  = [Text.Encoding]::GetEncoding(932)
        [Console]::OutputEncoding = [Text.Encoding]::GetEncoding(932)
    } else {
        # UTF-8
        chcp 65001 | Out-Null
        [Console]::InputEncoding  = [Text.Encoding]::UTF8
        [Console]::OutputEncoding = [Text.Encoding]::UTF8
    }
}
# tools フォルダの親 = プロジェクトルート
$root = Split-Path -Parent $PSScriptRoot
Push-Location $root
try {
    # 1) コンソール文字コードを合わせる
    Set-ConsoleEncoding $ConsoleEncoding
    # 2) まず本体を compile（target\classes を作る）
    if (-not $NoRebuild) {
        & .\mvnw.cmd -q -DskipTests compile
        if ($LASTEXITCODE -ne 0) { Fail "mvn compile failed" }
    }
    # 3) 依存Jarのクラスパスを書き出す（tools\classpath.txt）
    $cpFile = Join-Path $root "tools\classpath.txt"
    & .\mvnw.cmd -q -DskipTests dependency:build-classpath ("-Dmdep.outputFile=$cpFile")
    if ($LASTEXITCODE -ne 0) { Fail "mvn dependency:build-classpath failed" }
    if (-not (Test-Path $cpFile)) {
        Fail "classpath.txt が作成されていません: $cpFile"
    }
    $deps = (Get-Content $cpFile -Raw).Trim()
    $targetClasses = Join-Path $root "target\classes"
    if (-not (Test-Path $targetClasses)) {
        Fail "target\classes が見つかりません。mvn compile が通っているか確認してください。"
    }
    # Windowsのクラスパス区切りは ;（セミコロン）
    $cp = "$targetClasses;$deps"
    # 4) Java起動（重要：引数は配列で渡して分割事故を防ぐ）
    $fileEnc = if ($ConsoleEncoding -eq "CP932") { "MS932" } else { "UTF-8" }
    $javaArgs = @(
        "-Dfile.encoding=$fileEnc",
        "-cp", $cp,
        "com.example.ems.cli.EmsConsoleCli",
        $BaseUrl
    )
    & java @javaArgs
    exit $LASTEXITCODE
}
finally {
    Pop-Location
}