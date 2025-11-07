// TODO: Uncomment this when using Java's standard Semaphore
// import java.util.concurrent.Semaphore;

public class Car implements Runnable {
    private final String carName;

    // TODO: Uncomment when custom Semaphore class is ready
    // private final Semaphore serviced = new Semaphore(0); // For service
    // completion notification

    // Statistics tracking
    private long arrivalTime;
    private long serviceStartTime;
    private long departureTime;

    public Car(String name) {
        this.carName = name;
    }

    public String getCarName() {
        return carName;
    }

    // TODO: Uncomment when custom Semaphore class is ready
    // Method for Pump to call when service is done
    /*
     * public void serviceCompleted() {
     * serviced.signalSemaphore(); // Will use custom Semaphore's signalSemaphore()
     * }
     */

    // Statistics methods
    public void recordArrival() {
        this.arrivalTime = System.currentTimeMillis();
    }

    public void recordServiceStart() {
        this.serviceStartTime = System.currentTimeMillis();
    }

    public void recordDeparture() {
        this.departureTime = System.currentTimeMillis();
    }

    public long getWaitingTime() {
        if (serviceStartTime == 0 || arrivalTime == 0)
            return 0;
        return serviceStartTime - arrivalTime;
    }

    public long getServiceTime() {
        if (departureTime == 0 || serviceStartTime == 0)
            return 0;
        return departureTime - serviceStartTime;
    }

    public long getTotalTime() {
        if (departureTime == 0 || arrivalTime == 0)
            return 0;
        return departureTime - arrivalTime;
    }

    @Override
    public void run() {
        try {
            recordArrival(); // Record arrival time

            // TODO: Uncomment when custom Semaphore class is ready
            /*
             * // Wait for an empty slot in the waiting area
             * ServiceStation.empty.waitSemaphore(); // Using custom Semaphore
             * 
             * // Acquire mutex with try-finally for safety
             * ServiceStation.mutex.waitSemaphore(); // Using custom Semaphore
             * try {
             * boolean added = ServiceStation.waitingQueue.offer(this);
             * if (added) {
             * ServiceStation.log(carName + " entered waiting area");
             * } else {
             * ServiceStation.log(carName + " failed to enter waiting area");
             * ServiceStation.empty.signalSemaphore(); // Using custom Semaphore
             * return;
             * }
             * } finally {
             * ServiceStation.mutex.signalSemaphore(); // Always release mutex - using
             * custom Semaphore
             * }
             * 
             * // Signal that a car is available
             * ServiceStation.full.signalSemaphore(); // Using custom Semaphore
             * 
             * // Wait for service to complete
             * ServiceStation.log(carName + " waiting for service");
             * serviced.waitSemaphore(); // Using custom Semaphore's waitSemaphore()
             * recordDeparture(); // Record departure time
             * ServiceStation.log(carName + " service completed, leaving (waited: " +
             * getWaitingTime() + "ms, total: " + getTotalTime() + "ms)");
             */

            // TEMPORARY: Placeholder until custom Semaphore class is implemented
            ServiceStation.log(carName + " created - waiting for custom Semaphore class implementation");

        } catch (Exception e) {
            ServiceStation.log(carName + " encountered an error: " + e.getMessage());
        }
    }
}
