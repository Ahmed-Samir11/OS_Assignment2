public class Semaphore {
    private int value;

    // Constructor
    public Semaphore(int value) {
        this.value = value;
    }

    // acquire() → P() → wait
    public synchronized void acquire() {
        while (value <= 0) {
            try {
                wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("Thread interrupted during acquire.");
            }
        }
        value--;
    }

    // release() → V() → signal
    public synchronized void release() {
        value++;
        notify(); // Wake up one waiting thread
    }

    // Optional: for debugging
    public synchronized int getValue() {
        return value;
    }
}