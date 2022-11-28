REM On Windows, linking fails with "library limit of 65535 objects exceeded"
REM when building in debug mode, so build is locked to release.

cargo build -p rsdroid --release || exit /b 1
cp target\release\rsdroid.dll rsdroid-testing\assets || exit /b 1
gradlew rsdroid-testing:build || exit /b 1
