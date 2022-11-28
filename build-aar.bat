rustup target add x86_64-linux-android || exit /b 1
gradlew rsdroid:buildAnkiWebAssets || exit /b 1
