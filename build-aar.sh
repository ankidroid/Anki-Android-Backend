#!/bin/bash
#
# Builds the .aar file containing generated Kotlin, and the backend library
# used on Android. By default only the library for the current
# architecture is built (eg X86_64, or Arm64 on Apple M1+).
#
# Define ALL_ARCHS=1 to build a multi-platform bundle, which takes about 4x
# longer to download and build.
#
# Define DEBUG=1 to compile the Rust in debug mode, which builds faster
# but runs slower.
#
# When changing env vars, you must './gradlew stop' before rebuilding.
#
# The robolectric library needs to be built separately with ./build-robo.sh
# 

set -e

if [ "$ALL_ARCHS" = "1" ]; then
    rustup target add armv7-linux-androideabi   # arm
    rustup target add i686-linux-android        # x86
    rustup target add aarch64-linux-android     # arm64
    rustup target add x86_64-linux-android      # x86_64
else
    if [[ "$OSTYPE" == "darwin"* && $(arch) == "arm64" ]]; then
        rustup target add aarch64-linux-android
    else
        rustup target add x86_64-linux-android
    fi
fi

./gradlew assembleRelease
