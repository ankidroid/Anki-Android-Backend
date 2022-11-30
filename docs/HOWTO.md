# How to build, test and use this library

## Prerequisites

Make sure you have Android Studio can build AnkiDroid first.

### Init submodule

git submodule update --init

### C toolchain

Install Xcode/Visual Studio if on macOS/Windows.

### Rust

Install rustup from https://rustup.rs/

### Ninja

Debian/Ubuntu:

  sudo apt install ninja-build

macOS:

  brew upgrade
  brew install ninja

Windows if using choco:

  choco install ninja

You can alternatively download a binary from https://github.com/ninja-build/ninja/releases
and put it on your path.

### NDK

In Android Studio, choose the Tools>SDK menu option.

- In SDK tools, enable "show package details"
- Choose the NDK version that matches the number used in .github/workflows, eg 22.0.7026061
- After downloading, you may need to restart Android Studio to get it to
synchronize gradle.

## Build

Two main files need to be built:

- The main .aar file, which contains the backend Kotlin code, web assets, and
Anki backend code compiled for Android.
- A .jar that contains the backend code compiled for the host platform, for use
with Robolectric unit tests.

You should do the first build with the provided shell .sh/.bat file, as it will
take care of downloading the target architecture library as well. You'll need
to tell the script to use the Java libraries and NDK downloaded by Android Studio,
eg on Linux:

```
export ANDROID_SDK_ROOT=$HOME/Android/Sdk
```

Or macOS:

```
export ANDROID_SDK_ROOT=$HOME/Library/Android/sdk
```

If you don't have Java installed, you may be able to use the version bundled
with Android Studio. Eg on macOS:

```
export JAVA_HOME="/Applications/Android Studio.app/Contents/jre/Contents/Home"
```

Now build the .aar:

./build-aar.sh

Assuming success, build the .jar file:

./build-robo.sh


## Modify AnkiDroid to use built library

Now open the AnkiDroid project in AndroidStudio. To tell gradle to load the
compiled .aar and .jar files from disk, edit local.properties
in the AnkiDroid repo, and add the following line:

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
emulator/device (arm64 on M1 Macs), and run unit tests.

## Release builds

Only the current platform is built by default. In CI, the .aar and .jar files
are built for multiple platforms, so one release library can be used on a variety
of devices. See .github/workflows for how this is done.