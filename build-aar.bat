rustup target add x86_64-linux-android || exit /b 1
gradlew assembleRelease || exit /b 1
