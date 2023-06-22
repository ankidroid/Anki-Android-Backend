#!/bin/bash

set -e

test -f rsdroid/build/outputs/aar/rsdroid-release.aar || (
    echo "Run ./build-aar.sh first"
    exit 1
)
# See comment in build-aar.sh
export RUSTFLAGS=" -C link-arg=$(clang -print-libgcc-file-name)"

./gradlew rsdroid:lint rsdroid-instrumented:connectedCheck
