param([switch]$Install)
$ErrorActionPreference = 'Stop'
Push-Location $PSScriptRoot
try {
    $jdkDirectory = Get-ChildItem '.tooling\jdk-portable' -Directory | Select-Object -First 1
    if (!$jdkDirectory) { throw 'Local JDK missing. See README.md for Android Studio setup.' }
    $env:JAVA_HOME = $jdkDirectory.FullName
    $env:GRADLE_USER_HOME = Join-Path $PSScriptRoot '.tooling\gradle-cache'
    $env:ANDROID_HOME = Join-Path $PSScriptRoot '.tooling\android-sdk'
    $env:ANDROID_USER_HOME = Join-Path $PSScriptRoot '.tooling\android-user'
    & '.\.tooling\gradle-portable\gradle-8.9\bin\gradle.bat' --console=plain :app:assembleDebug :app:testDebugUnitTest
    if ($LASTEXITCODE -ne 0) { throw 'Build or tests failed.' }
    if ($Install) {
        & '.\.tooling\platform-tools\adb.exe' install -r '.\app\build\outputs\apk\debug\app-debug.apk'
        if ($LASTEXITCODE -ne 0) { throw 'ADB install failed. Check USB authorization and connected devices.' }
    }
} finally { Pop-Location }
