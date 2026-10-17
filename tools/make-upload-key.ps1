# Creates PocketBill's Google Play UPLOAD key, once (P8-T06). Run from the project folder:
#   powershell -ExecutionPolicy Bypass -File tools\make-upload-key.ps1
#
# The key and its passwords are written OUTSIDE the Git repository (the repo is public, D-006):
#   C:\dev\keys\pocketbill-upload.jks   - the key
#   keystore.properties                 - where Gradle finds it (git-ignored)
# BACK UP C:\dev\keys to a second place (USB drive or password manager). With Play App Signing,
# Google keeps the real app signing key, so a lost upload key can be reset, but it takes days.

$ErrorActionPreference = 'Stop'
$keyDir = 'C:\dev\keys'
$keyFile = Join-Path $keyDir 'pocketbill-upload.jks'
if (Test-Path $keyFile) { Write-Host "Key already exists: $keyFile (nothing changed)"; exit 0 }

$keytool = Join-Path $env:JAVA_HOME 'bin\keytool.exe'
if (-not (Test-Path $keytool)) { $keytool = 'C:\java\bin\keytool.exe' }

New-Item -ItemType Directory -Force $keyDir | Out-Null
# A long random password; it is saved in keystore.properties and must be backed up with the key.
$password = -join ((48..57) + (65..90) + (97..122) | Get-Random -Count 32 | ForEach-Object { [char]$_ })
$name = Read-Host 'Your name or business name for the certificate (e.g. Talha Ather)'

& $keytool -genkeypair -v -keystore $keyFile -alias upload -keyalg RSA -keysize 4096 -validity 10000 `
    -storepass $password -keypass $password -dname "CN=$name"

$props = @"
storeFile=$($keyFile -replace '\', '/')
storePassword=$password
keyAlias=upload
keyPassword=$password
"@
Set-Content -Path (Join-Path $PSScriptRoot '..\keystore.properties') -Value $props -Encoding ascii
Write-Host ''
Write-Host "Done. Key: $keyFile"
Write-Host 'NOW back up C:\dev\keys and keystore.properties somewhere safe (not GitHub).'
