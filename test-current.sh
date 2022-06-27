#!/bin/bash

set -e

NO_CROSS=true CURRENT_ONLY=true ./gradlew rsdroid:test rsdroid-instrumented:connectedAndroidTest

