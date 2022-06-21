# Testing changes with AnkiDroid on an X86_64 sim (Linux)

A similar approach may work on Mac and Windows, but this has only been tested on Linux
so far.

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
- sudo ln -sf /usr/bin/gcc /usr/bin/x86_64-unknown-linux-gnu-gcc

Install protobuf:

- Install protobuf with your package manager

Install Python packages

- pip install protobuf stringcase, or see the venv section below

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
./build-current.sh
```

## Modify AnkiDroid to use built library

Tell gradle to load the compiled .aar and .jar files from disk by editing local.properties
in the AnkiDroid repo, and adding the following line:

```
local_backend=true
```

If you also want to test out the new schema code paths that make greater use of the backend,
add the following line (be warned, do not use this on a collection you care about yet):

```
legacy_schema=false
```

Also make sure ext.ankidroid_backend_version in AnkiDroid/build.gradle matches the version
of the backend you're testing.

After making the change, you should be able to build and run the project on an x86_64
emulator/device, and run unit tests.
