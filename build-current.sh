#!/bin/bash
#
# Builds for the current architecture only. See docs/easy-testing.md
#

set -e

export CURRENT_ONLY=true

# build simulator/device package
./gradlew assembleRelease

# build library for Robolectric tests
CARGO_TARGET_DIR=target NO_CROSS=true ./gradlew rsdroid-testing:build
