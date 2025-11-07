import java.util.InputMismatchException;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Scanner;
import javax.swing.SwingUtilities;

/**
 * Main class for the Service Station simulation.
 * This class contains the main method to start the simulation,
 * holds the shared resources (Queues and Semaphores),
 * and provides a synchronized logging method.
 */
public class ServiceStation {

    // --- Shared Resources ---
    public static Queue<Car> waitingQueue;
    public static Semaphore mutex; // Controls access to the waitingQueue
    public static Semaphore empty; // Counts empty slots in the waitingQueue
    public static Semaphore full;  // Counts occupied slots in the waitingQueue
    public static Semaphore pumps; // Controls access to the service bays (pumps)
    
    // GUI bridge (optional)
    private static CarWashModel guiModel = null;
    private static CarWashGUI gui = null;

    /**
     * A synchronized logging method to prevent interleaved console output.
     * All threads should use this to print their status.
     * @param message The message to log.
     */
    public static synchronized void log(String message) {
        System.out.println(message);
        if (guiModel != null) {
            guiModel.fireLog(message);
        }
    }
    
    /**
     * Update GUI pump state (call from Pump thread)
     */
    public static void updatePumpState(int pumpId, String carName, int progress) {
        if (guiModel != null) {
            if (progress == 0 && carName == null) {
                guiModel.releasePump(pumpId - 1); // pumpId is 1-indexed, model is 0-indexed
            } else if (carName != null && progress == 0) {
                guiModel.claimPump(pumpId - 1, carName);
            } else {
                guiModel.setPumpProgress(pumpId - 1, progress, 0);
            }
        }
    }
    
    /**
     * Update GUI queue display by syncing local queue to GUI model
     */
    public static void syncQueueToGUI() {
        if (guiModel != null) {
            // Build a snapshot and fire property change via a public method
            java.util.List<String> snapshot = new java.util.ArrayList<>();
            for (Car c : waitingQueue) {
                snapshot.add(c.getCarName());
            }
            // Use the model's property-change to notify GUI
            guiModel.fireLogWithLevel("DEBUG", "Queue size: " + snapshot.size());
        }
    }

    /**
     * Main entry point for the simulation.
     */
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        int waitingCapacity;
        int numPumps;

        // 1. User input for waiting area capacity
        while (true) {
            try {
                System.out.print("Enter Waiting area capacity (1-10): ");
                waitingCapacity = scanner.nextInt();

                if (waitingCapacity >= 1 && waitingCapacity <= 10) {
                    break;
                } else {
                    log("Invalid capacity. Value must be between 1 and 10.");
                }
            } catch (InputMismatchException e) {
                log("Invalid input. Please enter a whole number.");
                scanner.next(); // Clear invalid input
            }
        }

        // 2. User input for number of pumps
        while (true) {
            try {
                System.out.print("Enter Number of service bays (pumps): ");
                numPumps = scanner.nextInt();

                if (numPumps > 0) {
                    break;
                } else {
                    log("Invalid pump count. Value must be positive (greater than 0).");
                }
            } catch (InputMismatchException e) {
                log("Invalid input. Please enter a whole number.");
                scanner.next(); // Clear invalid input
            }
        }

        log("\n--- Car Wash Simulation Starting ---");
        log("Waiting Area: " + waitingCapacity);
        log("Service Bays: " + numPumps);
        log("------------------------------------\n");

        // 3. Initialize shared resources
        waitingQueue = new LinkedList<>();
        mutex = new Semaphore(1);                   // Mutex starts at 1 (unlocked)
        empty = new Semaphore(waitingCapacity);     // 'empty' starts at capacity
        full = new Semaphore(0);                    // 'full' starts at 0
        pumps = new Semaphore(numPumps);            // 'pumps' starts at number of pumps

        // 3b. Initialize GUI (optional but recommended for bonus)
        final int finalNumPumps = numPumps;
        final int finalWaitingCapacity = waitingCapacity;
        SwingUtilities.invokeLater(() -> {
            guiModel = new CarWashModel(finalNumPumps);
            gui = new CarWashGUI(guiModel, null, finalWaitingCapacity);
            log("GUI initialized.");
        });
        
        // Small delay to let GUI appear before simulation starts
        try { Thread.sleep(500); } catch (InterruptedException e) {}

        // 4. Start pump threads
        for (int i = 0; i < numPumps; i++) {
            Pump pump = new Pump(i + 1);
            Thread pumpThread = new Thread(pump, "Pump-" + (i + 1));
            pumpThread.setDaemon(true); // Make pumps daemon threads so app can exit
            pumpThread.start();
        }

        // 5. Start generating car threads
        int carId = 1;
        int n = 0;
        try {
            while (n < 20) { // Limit to 20 cars for demonstration
                n++;
                String carName = "Car-" + carId++;
                Car car = new Car(carName);
                Thread carThread = new Thread(car, carName);
                carThread.start();
                
                // Simulate random car arrivals (0.5 to 1.5 seconds)
                Thread.sleep((long) (Math.random() * 1000) + 500);
            }
        } catch (InterruptedException e) {
            log("Car generation interrupted. Shutting down.");
            Thread.currentThread().interrupt();
        }
        
        scanner.close();
    }
}

/**
 * A custom Semaphore implementation.
 * Provides waitSemaphore() (P operation) and signalSemaphore() (V operation).
 */
class Semaphore {
    private int value;

    // Constructor
    public Semaphore(int value) {
        if (value < 0) throw new IllegalArgumentException("Semaphore value must be non-negative.");
        this.value = value;
    }

    // waitSemaphore() → P() → wait (was acquire)
    public synchronized void waitSemaphore() {
        while (value <= 0) {
            try {
                wait(); // Wait until value is positive
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("Thread interrupted during waitSemaphore.");
            }
        }
        value--;
    }

    // signalSemaphore() → V() → signal (was release)
    public synchronized void signalSemaphore() {
        value++;
        notifyAll(); // Wake up all waiting threads
    }

    // Optional: for debugging
    public synchronized int getValue() {
        return value;
    }
}

/**
 * Represents a Car thread.
 * A car arrives, waits for a spot, gets serviced, and departs.
 */
class Car implements Runnable {
    private final String carName;

    // This car's private semaphore. The car waits on this.
    // The pump signals it when service is complete.
    private final Semaphore serviced = new Semaphore(0);

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

    /**
     * Method for a Pump thread to call when this car's service is done.
     */
    public void serviceCompleted() {
        serviced.signalSemaphore(); // Signal this car's private semaphore
    }

    // --- Statistics methods ---
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
        if (serviceStartTime == 0 || arrivalTime == 0) return 0;
        return serviceStartTime - arrivalTime;
    }

    public long getServiceTime() {
        if (departureTime == 0 || serviceStartTime == 0) return 0;
        return departureTime - serviceStartTime;
    }

    public long getTotalTime() {
        if (departureTime == 0 || arrivalTime == 0) return 0;
        return departureTime - arrivalTime;
    }

    @Override
    public void run() {
        try {
            recordArrival(); // 1. Record arrival time
            ServiceStation.log(carName + " arrived.");

            // 2. Wait for an empty slot in the waiting area
            ServiceStation.empty.waitSemaphore();

            // 3. Acquired a slot, now lock the queue to add self
            ServiceStation.mutex.waitSemaphore();
            try {
                ServiceStation.waitingQueue.offer(this);
                ServiceStation.log(carName + " entered waiting area (Queue: " + ServiceStation.waitingQueue.size() + ")");
                ServiceStation.syncQueueToGUI(); // Update GUI with new queue state
            } finally {
                ServiceStation.mutex.signalSemaphore(); // 4. Always release mutex
            }

            // 5. Signal that a car is available (increment 'full' count)
            ServiceStation.full.signalSemaphore();

            // 6. Wait for service to complete (waits on its private semaphore)
            ServiceStation.log(carName + " is waiting for service.");
            serviced.waitSemaphore();

            // 7. Service is complete
            recordDeparture(); // Record departure time
            ServiceStation.log(carName + " service completed, leaving. (Wait: " +
                    getWaitingTime() + "ms, Service: " + getServiceTime() + "ms, Total: " + getTotalTime() + "ms)");

        } catch (Exception e) {
            ServiceStation.log(carName + " encountered an error: " + e.getMessage());
        }
    }
}

/**
 * Represents a Pump (service bay) thread.
 * A pump waits for a car, services it, and signals the car when done.
 */
class Pump implements Runnable {
    private final int pumpId;

    public Pump(int id) {
        this.pumpId = id;
    }

    @Override
    public void run() {
        ServiceStation.log("Pump " + pumpId + " is operational.");
        while (true) {
            try {
                // 1. Wait until there is at least one car in the queue
                ServiceStation.full.waitSemaphore();

                // 2. Lock the queue to remove a car
                Car car;
                ServiceStation.mutex.waitSemaphore();
                try {
                    // 3. Remove the first car from the waiting queue
                    car = ServiceStation.waitingQueue.poll();
                } finally {
                    ServiceStation.mutex.signalSemaphore(); // 4. Release queue lock
                }

                // 5. Signal that one more space is available in the waiting area
                ServiceStation.empty.signalSemaphore();

                if (car != null) {
                    // 6. Request permission to use a service bay (this pump)
                    ServiceStation.pumps.waitSemaphore();

                    // 7. Start servicing the car
                    car.recordServiceStart(); // Tell car to record service start time
                    ServiceStation.log("Pump " + pumpId + ": " + car.getCarName() +
                            " begins service.");
                    
                    // Update GUI: pump is now occupied
                    ServiceStation.updatePumpState(pumpId, car.getCarName(), 0);
                    ServiceStation.syncQueueToGUI(); // Queue changed (car removed)

                    // 8. Simulate service time (1 to 3 seconds) with progress updates
                    long serviceDuration = (long) (Math.random() * 2000) + 1000;
                    long startTime = System.currentTimeMillis();
                    long endTime = startTime + serviceDuration;
                    
                    while (System.currentTimeMillis() < endTime) {
                        long elapsed = System.currentTimeMillis() - startTime;
                        int progress = (int) ((elapsed * 100) / serviceDuration);
                        if (progress > 100) progress = 100;
                        ServiceStation.updatePumpState(pumpId, car.getCarName(), progress);
                        Thread.sleep(100); // Update GUI every 100ms
                    }
                    
                    // Ensure final progress is 100%
                    ServiceStation.updatePumpState(pumpId, car.getCarName(), 100);

                    // 9. Service completed
                    ServiceStation.log("Pump " + pumpId + ": " + car.getCarName() + " finishes service.");
                    car.serviceCompleted(); // Tell car it is done

                    // 10. Release the service bay
                    ServiceStation.log("Pump " + pumpId + ": Bay is now free.");
                    ServiceStation.pumps.signalSemaphore();
                    
                    // Update GUI: pump is now free
                    ServiceStation.updatePumpState(pumpId, null, 0);
                }

            } catch (InterruptedException e) {
                ServiceStation.log("Pump " + pumpId + " interrupted.");
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}