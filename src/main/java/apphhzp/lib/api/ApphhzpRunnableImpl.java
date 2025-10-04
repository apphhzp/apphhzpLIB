package apphhzp.lib.api;

class ApphhzpRunnableImpl implements Runnable {
    private final ApphhzpRunnable runnable;
    public ApphhzpRunnableImpl(final ApphhzpRunnable runnable) {
        this.runnable = runnable;
    }
    @Override
    public void run() {
        runnable.runTask();
    }
}
