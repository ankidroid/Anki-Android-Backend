if not exist rsdroid-android\build\outputs\aar\rsdroid-android-release.aar (
    echo "Run ./build.bat first"
    exit 1
)

./gradlew rsdroid:lint rsdroid-android:lint rsdroid-instrumented:connectedCheck
