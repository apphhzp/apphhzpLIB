package apphhzp.lib.hotspot.oops;


import apphhzp.lib.hotspot.oops.oop.OopDesc;

public interface HeapVisitor {
    // This is called at the beginning of the iteration to provide the
    // HeapVisitor with information about the amount of memory which
    // will be traversed (for example, for displaying a progress bar)
    void prologue(long usedSize);

    // Callback method for each object
    // Return true if the iteration should be stopped.
    boolean doObj(OopDesc obj);

    // This is called after the traversal is complete
    void epilogue();
}
