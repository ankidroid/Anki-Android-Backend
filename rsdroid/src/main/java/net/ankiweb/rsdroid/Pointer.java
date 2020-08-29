package net.ankiweb.rsdroid;

public class Pointer {
    private long mPointer;


    public Pointer(long backendPointer) {
        mPointer = backendPointer;
    }


    public long toJni() {
        return mPointer;
    }
}
