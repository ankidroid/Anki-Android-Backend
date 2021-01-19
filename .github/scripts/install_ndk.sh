: ${1?"Usage: $0 NDK Version (ex 21.3.6528147)"}

echo "installing NDK $1"
echo "y" | $(sudo $ANDROID_SDK_ROOT/tools/bin/sdkmanager --install "ndk;$1" --sdk_root=${ANDROID_SDK_ROOT}) | grep -v = || true
