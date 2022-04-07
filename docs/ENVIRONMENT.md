# Environment Setup

## Get the source

Clone the project, including submodules

`git clone --recurse-submodules git@github.com:ankidroid/Anki-Android-Backend`

## Docker (needed for cross compilation)

### Docker Installation

#### macOS and Windows

you will probably want [Docker Desktop](https://www.docker.com/products/docker-desktop/)

#### linux

Your package manager should have Docker, for example `apt install docker` for Ubuntu

### Docker Image installation

We have a script that automates Docker image build, run it in your shell:

`./tools/build-docker-images.sh`

### Android Tools

Make sure you have an Android SDK, NDK and build tools installed

You may examine the file [`rsdriod/build.gradle`](../rsdroid/build.gradle) to determine the versions needed.

For example:

```groovy
    compileSdkVersion 30
    buildToolsVersion "30.0.1"
    ndkVersion "22.0.7026061" // Used by GitHub actions - avoids an install step on some machines

    compileOptions {
        sourceCompatibility JavaVersion.VERSION_1_8
        targetCompatibility JavaVersion.VERSION_1_8
    }

    defaultConfig {
        minSdkVersion 21
        targetSdkVersion 30
```

That indicates you need:

- SDK 30 installed (compileSdkVersion and targetSdkVersion)
- NDK 22.0.7026062 (ndkVersion)
- Build Tools 30.0.1 (buildToolsVersion)

You should open Android Studio and use the Tools --> SDK Manager to download them

### Rust

#### Rust toolchain

Install rust via [rustup](https://rustup.rs/)

Configure rust to use version 1.54.0 since current does not work yet [#168](https://github.com/ankidroid/Anki-Android-Backend/issues/168)

```bash
rustup install 1.54.0
rustup default 1.54.0
```

#### Android targets

Add our 4 supported Android targets

```bash
rustup target add armv7-linux-androideabi
rustup target add i686-linux-android
rustup target add aarch64-linux-android
rustup target add x86_64-linux-android
```

It appears that you need to make a link to 'cc' on linux for the Rust build to work:

```bash
sudo ln -s /usr/bin/cc /usr/local/bin/x86_64-unknown-linux-gnu-gcc
```

#### Cross-compile tools

Install cross crate for cross compiling rust code

```bash
cargo install cross --git https://github.com/rust-embedded/cross --tag v0.2.1
```

(we need a specific version as the current version requires a newer rust to work, see #168)

## Protobuf tools

### Install protobuf compiler (protoc) 3.x

- macOS: `brew install protobuf`
- ubuntu: `sudo apt-get install protobuf-compiler`
- others:
  - [build from source](https://github.com/protocolbuffers/protobuf/blob/master/src/README.md)
  - [pre built binary](https://github.com/protocolbuffers/protobuf/releases)
    - name: `protoc-$VERSION-$PLATFORM.zip`
    - add bin directory to your PATH

### Install Python protobuf support

#### Install Python3

- macOS this should be `brew install python`
- linux you may need to make sure `python` exists instead of just `python3`, for example `sudo apt install python-is-python3`

#### Install protobuf package

```bash
pip install protobuf
```

## Make sure it works

Basic commands to build and test things may be taken from the `./github/workflows` scripts.

Some examples:

> ./gradlew clean assembleRelease -DtestBuildType=release

Or maybe

> ./gradlew :rsdroid:androidJavadocs -DtestBuildType=release

Perhaps

> ./gradlew clean rsdroid-testing:build -DtestBuildType=debug --warning-mode all

Even

> ./gradlew rsdroid:test -x jar -x cargoBuildArm -x cargoBuildX86 -x cargoBuildArm64 -x cargoBuildX86_64
