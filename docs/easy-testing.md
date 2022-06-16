# Testing changes with AnkiDroid on an X86_64 sim (Linux)

## Setup

Make sure you can build AnkiDroid first.

Install NDK:

- Download https://developer.android.com/studio#command-tools
- Rename cmdline-tools to $ANDROID_SDK_ROOT/cmdline-tools/latest
- Get ndk version from rslib/build.grade
- .github/scripts/install_ndk.sh 22.0.7026061

Install Rust:

- rustup install 1.58.1
- rustup target add x86_64-linux-android
- sudo ln -sf /usr/bin/gcc /usr/bin/x86_64--unknown-linux-gnu-gcc

Install protobuf:

- Install protobuf with your package manager

Install Python packages

- pip install protobuf stringcase, or see the venv section below

## Limit build to x86_64

So you don't need to install cross compilers, patch the sources to
only build the x86_64 image for the aar file and jar:

```diff
diff --git a/rsdroid/build.gradle b/rsdroid/build.gradle
index bc3a401..7c69a5b 100644
--- a/rsdroid/build.gradle
+++ b/rsdroid/build.gradle
@@ -72,7 +72,7 @@ dependencies {

 }

-preBuild.dependsOn "cargoBuild"
+preBuild.dependsOn "cargoBuildX86_64"

 signing {
     def hasPrivate = project.hasProperty('SIGNING_PRIVATE_KEY')
diff --git a/rsdroid-testing/build.gradle b/rsdroid-testing/build.gradle
index 8641b8f..694212e 100644
--- a/rsdroid-testing/build.gradle
+++ b/rsdroid-testing/build.gradle
@@ -181,8 +181,6 @@ task copyWindowsOutput(type: Copy) {
 // TODO: check for cargo
 // check for targets: x86_64-apple-darwin, x86_64-pc-windows-gnu, TODO: Linux

-processResources.dependsOn preBuildWindows
-processResources.dependsOn copyWindowsOutput
 // To fix: "toolchain 'nightly-x86_64-unknown-linux-gnu' is not installed"
 // execute in bash: rustup toolchain install nightly-x86_64-unknown-linux-gnu
 // "linker `x86_64-unknown-linux-gnu-gcc` not found"
```

## Using a custom python venv

If you don't want to `pip install protobuf` globally, you can
switch to a venv:

```diff
diff --git a/tools/protoc-gen/protoc-gen.sh b/tools/protoc-gen/protoc-gen.sh
index d4039ec..3ac5d29 100755
--- a/tools/protoc-gen/protoc-gen.sh
+++ b/tools/protoc-gen/protoc-gen.sh
@@ -1,2 +1,3 @@
 #!/bin/bash
-./tools/protoc-gen/protoc-gen.py
+
+$HOME/Local/python/misc/bin/python3 ./tools/protoc-gen/protoc-gen.py
```

## Build

Two files need to be built:

- A .aar file for emulator/device testing
- A .jar for running unit tests

```
export ANDROID_SDK_ROOT=$HOME/Android/Sdk
export PATH=$HOME/Android/Sdk/cmdline-tools/latest/bin/:$PATH
./gradlew assembleRelease
NO_CROSS=true ./gradlew rsdroid-testing:build
```

If your environment is set up to override the default
Rust output location, you must also set unset CARGO_TARGET_DIR.

## Modify AnkiDroid to use built library

Tell gradle to load the compiled .aar and .jar files from disk:

```diff
diff --git a/AnkiDroid/build.gradle b/AnkiDroid/build.gradle
index 2a2d94034..b21c7caff 100644
--- a/AnkiDroid/build.gradle
+++ b/AnkiDroid/build.gradle
@@ -271,10 +271,10 @@ dependencies {
     // - switch the commented and uncommented lines below
     // - run a gradle sync

-    implementation "io.github.david-allison-1:anki-android-backend:$ankidroid_backend_version"
-    testImplementation "io.github.david-allison-1:anki-android-backend-testing:$ankidroid_backend_version"
-    // implementation files("../../Anki-Android-Backend/rsdroid/build/outputs/aar/rsdroid-release.aar")
-    // testImplementation files("../../Anki-Android-Backend/rsdroid-testing/build/libs/rsdroid-testing-0.1.12.jar")
+    // implementation "io.github.david-allison-1:anki-android-backend:$ankidroid_backend_version"
+    // testImplementation "io.github.david-allison-1:anki-android-backend-testing:$ankidroid_backend_version"
+    implementation files("../../Anki-Android-Backend/rsdroid/build/outputs/aar/rsdroid-release.aar")
+    testImplementation files("../../Anki-Android-Backend/rsdroid-testing/build/libs/rsdroid-testing-0.1.12.jar")

     // On Windows, you can use something like
     // implementation files("C:\\GitHub\\Rust-Test\\rsdroid\\build\\outputs\\aar\\rsdroid-release.aar")
```

After making the change, force a gradle sync, and then you should be able to build
and run the project on an x86_64 emulator/device, and run unit tests.
