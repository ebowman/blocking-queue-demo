package demo;

import java.util.concurrent.*;

/**
 * Demonstrates how to supply a custom blocking queue to an executor, so that you have more control
 * over what happens when the production side starts to overwhelm the consumption side.
 */
public class Demo {
    final int nThreads = 10;

    // if this is false, then once the ABQ is full, executor.submit
    // will fail. If this is true, then it will spin the thread until
    // there is room in the queue (a bad idea, see below).
    boolean nevRfail = true;

    BlockingQueue<Runnable> queue = new ArrayBlockingQueue<Runnable>(1024) {
        @Override
        public boolean offer(Runnable r) {
            if (nevRfail) {
                while (!super.offer(r)) {
                    try {
                        // probably a terrible idea to do this in a production system;
                        // this is just to demonstrate how it works
                        Thread.sleep(100);
                    } catch (InterruptedException ignore) {
                    }
                }
            } else {
                add(r);
            }
            return true;
        }

        @Override
        public boolean add(Runnable r) {
            // add calls offer, so we need to re-implement how the superclass does it
            if (super.offer(r)) {
                return true;
            } else {
                throw new IllegalStateException("Queue full");
            }
        }
    };

    ExecutorService executor = new ThreadPoolExecutor(nThreads, nThreads, 0L, TimeUnit.MILLISECONDS, queue);

    Runnable run(String msg) {
        return () -> System.out.println(msg);
    }

    void go() throws InterruptedException {
        try {
            for (int i = 0; i < 100000; i++) {
                Runnable next = run("test " + i);
                executor.submit(next);
            }
        } finally {
            executor.shutdown();
        }
    }

    public static void main(String[] args) throws Exception {
        try {
            new Demo().go();
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}
