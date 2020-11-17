package net.ankiweb.rsdroid;

public class Pointer {
    private final long mPointer;


    public Pointer(long backendPointer) {
        mPointer = backendPointer;
    }


    public long toJni() {
        return mPointer;
    }
}
