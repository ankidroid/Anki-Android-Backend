#!/bin/bash

set -e

test -f rsdroid-android/build/outputs/aar/rsdroid-android-release.aar || (
    echo "Run ./build.sh first"
    exit 1
)

. ./set-android-ndk-home.sh

./gradlew rsdroid-android:lint rsdroid-instrumented:connectedCheck
