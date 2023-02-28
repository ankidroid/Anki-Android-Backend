# AnkiDroid-Backend

An interface for accessing Anki Desktop's Rust backend inside AnkiDroid. This
allows AnkiDroid to re-use the computer version's business logic and webpages,
instead of having to reimplement them.

This is a separate repo that gets published to a library that AnkiDroid consumes,
so that AnkiDroid development is possible without a Rust toolchain installed.

## Prerequisites

We assume you already have Android Studio, and are able to build the AnkiDroid
project already.

### Download Anki submodule

git submodule update --init

### C toolchain

Install Xcode/Visual Studio if on macOS/Windows.

### Rust

Install rustup from <https://rustup.rs/>

### Ninja

Debian/Ubuntu:

  sudo apt install ninja-build

macOS:

  brew upgrade
  brew install ninja

Windows if using choco:

  choco install ninja

You can alternatively download a binary from <https://github.com/ninja-build/ninja/releases>
and put it on your path.

### NDK

In Android Studio, choose the Tools>SDK menu option.

- In SDK tools, enable "show package details"
- Choose the NDK version that matches the number used in .github/workflows, eg 22.0.7026061
- After downloading, you may need to restart Android Studio to get it to
synchronize gradle.

### Windows: msys2

Install [msys2](https://www.msys2.org/) into the default folder location.

After installation completes, run msys2, and run the following command:

```
pacman -S git rsync
```

When following the build steps below, make sure msys is on the path:

```
set PATH=%PATH%;c:\msys64\usr\bin
```

## Building

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

or Windows:

```
set JAVA_HOME=C:\Program Files\Android\Android Studio\jre
```

Now build the .aar:

```
./build-aar.sh
```

If you have 'python3' on your system but not 'python', you can specify
the name:

```
RUST_ANDROID_GRADLE_PYTHON_COMMAND=python3 ./build-aar.sh
```

Assuming success, then build the .jar file:

```
./build-robo.sh
```

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

## Architecture

See [ARCHITECTURE.md](./docs/ARCHITECTURE.md)

## License

[GPL-3.0 License](https://github.com/ankidroid/Anki-Android/blob/master/COPYING)  
[AGPL-3.0 Licence](https://github.com/AnkiDroid/anki/blob/main/LICENSE) (anki submodule)
