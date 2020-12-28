echo "installing Rust targets for Android"
rustup target add armv7-linux-androideabi   # arm
rustup target add i686-linux-android        # x86
rustup target add aarch64-linux-android     # arm64
rustup target add x86_64-linux-android      # x86_64
echo "installing Rust targets for Robolectric .jar"
rustup target add x86_64-unknown-linux-gnu
rustup target add x86_64-apple-darwin
rustup target add x86_64-pc-windows-gnu