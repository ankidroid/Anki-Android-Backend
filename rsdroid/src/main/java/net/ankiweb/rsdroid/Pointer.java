package net.ankiweb.rsdroid;

public class Pointer {
    private final long pointer;


    public Pointer(long backendPointer) {
        pointer = backendPointer;
    }


    public long toJni() {
        return pointer;
    }
}
