#!/bin/bash

set -e

NO_CROSS=true CURRENT_ONLY=true ./gradlew rsdroid:test rsdroid-instrumented:assembleDebugAndroidTest
adb shell am instrument -w -e debug false net.ankiweb.rsdroid.instrumented.test/androidx.test.runner.AndroidJUnitRunner
