package net.ankiweb.rsdroid;

import android.annotation.SuppressLint;

import androidx.annotation.CheckResult;

public class NativeMethods {
    static {
        System.loadLibrary("rsdroid");
    }

    @CheckResult
    static native byte[] command(long backendPointer, final int command, byte[] args);
    @CheckResult
    static native long openBackend(byte[] data);

    @SuppressLint("CheckResult")
    @SuppressWarnings("unused")
    static void execCommand(long backendPointer, final int command, byte[] args) {
        command(backendPointer, command, args);
    }
}
