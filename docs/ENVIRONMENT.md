# Environment

## Requirements

- [ ] Clone the project
    - [ ] Get all the submodules recursively 
```bash
$ git submodule update --init --recursive
```
- [ ] Install Docker (needed for cross compilation)
    - [ ] Build all docker images
```bash
$ ./tools/build-docker-images.sh
```
- [ ] Android SDK
- [ ] Android NDK
    - You can find the exact needed version from [`rsdriod/build.gradle`](./rsdriod/build.gradle)
```groovy
android {
    compileSdkVersion 30
    buildToolsVersion "30.0.1"
    ndkVersion "22.0.7026061" <------------- Here is the version

    compileOptions {
        sourceCompatibility JavaVersion.VERSION_1_8
        targetCompatibility JavaVersion.VERSION_1_8
    }
```
- [ ] Rust
    - You can easily install rust via [rustup](https://rustup.rs/)
```
- [ ] Add Android targets
```bash
$ rustup target add armv7-linux-androideabi   # arm
$ rustup target add i686-linux-android        # x86
$ rustup target add aarch64-linux-android     # arm64
$ rustup target add x86_64-linux-android      # x86_64
```
- [ ] Install cross crate for cross compiling rust code
```bash
$ cargo install cross --git https://github.com/rust-embedded/cross/
```
- [ ] Install Python
    - [ ] Install protobuf pakages
```bash
$ pip3 install protobuf
$ pip3 install protobuf-compiler
```

## Note for macOS users
You may face issues while trying to install protobuf packages. you can solve these issues by installing pythton 3.7.
### Using pyenv
https://github.com/pyenv/pyenv#installation
### Using homebrew
```bash
$ brew install python@3.7
$ brew link python@3.7 # if you do already a have a version installed unlink it first
```