public class VolatileDemo {

    /**
     * Volatile variables are not cached into threads' working memory, but has
     * only one main copy in the shared memory (L3 cache or main memory).
     * Any update to a volatile variable by any thread is written directly to
     * the shared memory. And every time another thread is accessing its value,
     * it is read directly from the main memory.
     * In other words, any update to a volatile variable becomes visible to all
     * other threads.
     * In this way, the same variable is kept synchronized among different
     * threads.
     */
    private static volatile int volatileVar = 0;

    /**
     * Main driver.
     * @param args arguments from command line
     */
    public static void main(String[] args) {
        Thread printThread = new Thread() {
            @Override
            public void run() {
                int x = volatileVar;
                while (true) {
                    if (x != volatileVar) {
                        System.out.println("Print volatile variable: " + volatileVar);
                        x = volatileVar;
                    }
                }
            }
        };

        Thread incThread = new Thread() {
            @Override
            public void run() {
                while (true) {
                    ++volatileVar;
                    System.out.println("Increment volatile variable: " + volatileVar);
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        };

        printThread.start();
        incThread.start();

        /*
         * Without the volatile keyword, we can only see "Increment ..." statements, because volatileVar is cached into
         * the two threads' working memories, respectively. Therefore, any update to volatileVar in the incrementing
         * thread is not visible to the printing thread.
         *
         * With the volatile keyword, we can see both the "Print ..." and the "Increment ..." statements, because now
         * there is only one main copy of volatileVar, which is in the shared memory. The updates by the incrementing
         * thread is written directly to the shared memory, and whenever the printing memory is accessing its value, it
         * is read directly from the shared memory.
         * In other words, any updates to volatileVar by the incrementing thread is not visible to the printing thread.
         */
    }

}
