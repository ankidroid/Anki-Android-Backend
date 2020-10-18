Library which allows a consumer to load `rsdroid` under Robolectric.

Robolectric requires a native library, rather than a library compiled for Android. 

This project extracts precompiled native libraries into a temp folder, loads them into the JVM at runtime.

We use a separare library as I wanted to avoid bloating the release `.aar` file.

Note: using an old version of the rust toolchain to avoid cross-compilation bug for MacOS