rustup target add x86_64-linux-android || exit /b 1
set PATH=anki\out\extracted\python;%PATH%
gradlew assembleRelease || exit /b 1
