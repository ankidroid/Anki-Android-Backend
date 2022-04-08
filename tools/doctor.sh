#!/usr/bin/env bash

set -e # Error out if there were any problems

ANDROID_NDK_VERSION="22.0.7026061"

red=31
green=32
yellow=33
blue=34
purple=35
cyan=36
lgray=37
gray=90
normal=0
bold=1

cecho() {
  echo -e "\033[$bold;$1m$2\033[0m"
}

error_echo() {
  cecho $red "✗ $1"
  exit 1
}

ok_echo() {
  cecho $green "✓ $1"
}

cecho $lgray"##################################"
cecho $lgray "# AnkiDroidBackend Docker Script #"
cecho $lgray "##################################\n"

cecho $lgray "Getting all submodules recursively"
git submodule update --init --recursive


if [[ $(which docker) && $(docker --version) ]]; then
  ok_echo "Docker is installed"
else
  error_echo "Docker is not installed. please install it first"
fi


cecho $lgray "Building docker images"
./tools/build-docker-images.sh


if [[ -n "$ANDROID_SDK_ROOT" ]]; then
  ok_echo "ANDROID_SDK_ROOT is set"
else
  error_echo "ANDROID_SDK_ROOT should point to your SDK installation"
fi


if [[ -d "$ANDROID_SDK_ROOT/ndk/$ANDROID_NDK_VERSION" ]]; then
  ok_echo "NDK $ANDROID_NDK_VERSION directory found"
else
  echo -e "Expected to find: $ANDROID_SDK_ROOT/ndk/$ANDROID_NDK_VERSION"
  error_echo "NDK $ANDROID_NDK_VERSION directory found"
fi


cecho $lgray "Installing rust 1.54.0" # nightly" - temporarily using 1.54.0 - see #168
rustup install 1.54.0 #nightly
rustup default 1.54.0


cecho $lgray "Adding rust android targets"
rustup target add armv7-linux-androideabi   # arm
rustup target add i686-linux-android        # x86
rustup target add aarch64-linux-android     # arm64
rustup target add x86_64-linux-android      # x86_64

cecho $lgray "Installing cross"
cargo install cross --git https://github.com/rust-embedded/cross/ --tag v0.2.1 # current requires rust-current


cecho $lgray "Installing protobuf python libraries"

if [[ $(protoc --version) ]]; then
  ok_echo "Protobuf compiler is installed"
else
  error_echo "Protobuf compiler (protoc) is not found"
fi

if [[ $(pip3 install protobuf) ]]; then
  ok_echo "Protobuf python library is installed"
else
    echo -e "Try installing with python 3.7"
    error_echo "Failed installing Protobuf python libraries"
fi