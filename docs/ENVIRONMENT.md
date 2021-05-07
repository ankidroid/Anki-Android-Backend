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
- [ ] Install protobuf compiler (protoc) 3.x
    - macOS: `brew install protobuf`
    - ubuntu: `sudo apt-get install protobuf-compiler`
    - others:
        - [build from source](https://github.com/protocolbuffers/protobuf/blob/master/src/README.md)
        - [pre built binary](https://github.com/protocolbuffers/protobuf/releases)
            - name: `protoc-$VERSION-$PLATFORM.zip`
            - add bin directory to your PATH
- [ ] Install Python3
    - [ ] Install protobuf package
```bash
$ pip3 install protobuf
```