# disabled: Incompatible with https://www.apple.com/legal/sla/docs/xcode.pdf
sudo ./rsdroid-testing/tools/setup_macos.sh
sudo ./rsdroid-testing/tools/osxcross.sh
echo "installed OSXCROSS"
cd osxcross/target/bin
export ANKIDROID_MACOS_CC="$PWD/x86_64-apple-darwin14-cc"
echo "ANKIDROID_MACOS_CC set to ${ANKIDROID_MACOS_CC}"
