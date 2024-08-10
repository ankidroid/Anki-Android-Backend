@echo off
if [%1]==[] goto usage

echo Installing NDK %1
echo %PATH%
sdkmanager --list_installed | findstr ndk
sdkmanager --install "ndk;%1" --sdk_root=%ANDROID_SDK_ROOT% | findstr /v =
sdkmanager --list_installed | findstr ndk
exit /b 0

:usage
echo Usage: %0 NDK Version (ex 21.3.6528147)