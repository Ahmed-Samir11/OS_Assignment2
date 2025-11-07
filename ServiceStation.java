import java.util.InputMismatchException;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicLong;
import javax.swing.SwingUtilities;

public class ServiceStation {

    public static Queue<Car> waitingQueue;
    public static Semaphore mutex;
    public static Semaphore empty;
    public static Semaphore full;
    public static Semaphore pumps;
    
    private static CarWashModel guiModel = null;
    private static CarWashGUI gui = null;
    
    public static final AtomicLong totalWaitTime = new AtomicLong(0);
    public static final AtomicLong totalWorkTime = new AtomicLong(0);
    public static final AtomicLong semaphoreWaitCount = new AtomicLong(0);

    public static synchronized void log(String message) {
        System.out.println(message);
        if (guiModel != null) {
            guiModel.fireLog(message);
        }
    }
    
    public static void updatePumpState(int pumpId, String carName, int progress) {
        if (guiModel != null) {
            if (progress == 0 && carName == null) {
                guiModel.releasePump(pumpId - 1);
            } else if (carName != null && progress == 0) {
                guiModel.claimPump(pumpId - 1, carName);
            } else {
                guiModel.setPumpProgress(pumpId - 1, progress, 0);
            }
        }
    }
    
    public static void syncQueueToGUI() {
        if (guiModel != null) {
            java.util.List<String> snapshot = new java.util.ArrayList<>();
            for (Car c : waitingQueue) {
                snapshot.add(c.getCarName());
            }
            guiModel.fireLogWithLevel("DEBUG", "Queue size: " + snapshot.size());
        }
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        int waitingCapacity;
        int numPumps;

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
                scanner.next();
            }
        }

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
                scanner.next();
            }
        }

        log("\n--- Car Wash Simulation Starting ---");
        log("Waiting Area: " + waitingCapacity);
        log("Service Bays: " + numPumps);
        log("------------------------------------\n");

        waitingQueue = new LinkedList<>();
        mutex = new Semaphore(1);
        empty = new Semaphore(waitingCapacity);
        full = new Semaphore(0);
        pumps = new Semaphore(numPumps);

        final int finalNumPumps = numPumps;
        final int finalWaitingCapacity = waitingCapacity;
        SwingUtilities.invokeLater(() -> {
            guiModel = new CarWashModel(finalNumPumps);
            gui = new CarWashGUI(guiModel, null, finalWaitingCapacity);
            log("GUI initialized.");
        });
        
        try { Thread.sleep(500); } catch (InterruptedException e) {}

        for (int i = 0; i < numPumps; i++) {
            Pump pump = new Pump(i + 1);
            Thread pumpThread = new Thread(pump, "Pump-" + (i + 1));
            pumpThread.setDaemon(true);
            pumpThread.start();
        }

        int carId = 1;
        int n = 0;
        try {
            while (n < 20) {
                n++;
                String carName = "Car-" + carId++;
                Car car = new Car(carName);
                Thread carThread = new Thread(car, carName);
                carThread.start();
                
                Thread.sleep((long) (Math.random() * 1000) + 500);
            }
        } catch (InterruptedException e) {
            log("Car generation interrupted. Shutting down.");
            Thread.currentThread().interrupt();
        }
        
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        printEfficiencyAnalysis();
        
        scanner.close();
    }
    
    private static void printEfficiencyAnalysis() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("COMPUTATIONAL EFFICIENCY ANALYSIS");
        System.out.println("=".repeat(80));
        
        long totalWaitNs = totalWaitTime.get();
        long totalWorkNs = totalWorkTime.get();
        long waitCount = semaphoreWaitCount.get();
        
        double totalWaitMs = totalWaitNs / 1_000_000.0;
        double totalWorkMs = totalWorkNs / 1_000_000.0;
        double totalTimeMs = totalWaitMs + totalWorkMs;
        
        System.out.println("Total time spent waiting on semaphores: " + String.format("%.2f", totalWaitMs) + " ms");
        System.out.println("Total time spent doing actual work: " + String.format("%.2f", totalWorkMs) + " ms");
        System.out.println("Total execution time: " + String.format("%.2f", totalTimeMs) + " ms");
        System.out.println("Number of semaphore wait operations: " + waitCount);
        
        if (waitCount > 0) {
            double avgWaitMs = totalWaitMs / waitCount;
            System.out.println("Average wait time per semaphore operation: " + String.format("%.3f", avgWaitMs) + " ms");
        }
        
        if (totalTimeMs > 0) {
            double workPercentage = (totalWorkMs / totalTimeMs) * 100;
            double waitPercentage = (totalWaitMs / totalTimeMs) * 100;
            double efficiency = totalWorkMs / totalTimeMs;
            
            System.out.println("\nTime Distribution:");
            System.out.println("  Work time: " + String.format("%.2f%%", workPercentage));
            System.out.println("  Wait time: " + String.format("%.2f%%", waitPercentage));
            System.out.println("  Efficiency ratio: " + String.format("%.4f", efficiency));
        }
        
        System.out.println("\nSYNCHRONIZATION EFFECTS:");
        System.out.println("- Synchronization introduces overhead through context switching and blocking");
        System.out.println("- Wait time represents threads blocked on semaphores (mutex, empty, full, pumps)");
        System.out.println("- Work time includes queue operations, service simulation, and logging");
        System.out.println("- The efficiency ratio shows productive work vs synchronization overhead");
        System.out.println("- Higher wait percentages indicate more resource contention");
        System.out.println("- Trade-off: synchronization ensures correctness but reduces throughput");
        System.out.println("=".repeat(80) + "\n");
    }
}

class Semaphore {
    private int value;

    public Semaphore(int value) {
        if (value < 0) throw new IllegalArgumentException("Semaphore value must be non-negative.");
        this.value = value;
    }

    public synchronized void waitSemaphore() {
        long startWait = System.nanoTime();
        while (value <= 0) {
            try {
                wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("Thread interrupted during waitSemaphore.");
            }
        }
        long waitDuration = System.nanoTime() - startWait;
        ServiceStation.totalWaitTime.addAndGet(waitDuration);
        ServiceStation.semaphoreWaitCount.incrementAndGet();
        value--;
    }

    public synchronized void signalSemaphore() {
        value++;
        notifyAll();
    }

    public synchronized int getValue() {
        return value;
    }
}

class Car implements Runnable {
    private final String carName;
    private final Semaphore serviced = new Semaphore(0);
    private long arrivalTime;
    private long serviceStartTime;
    private long departureTime;

    public Car(String name) {
        this.carName = name;
    }

    public String getCarName() {
        return carName;
    }

    public void serviceCompleted() {
        serviced.signalSemaphore();
    }

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
            long workStart = System.nanoTime();
            recordArrival();
            ServiceStation.log(carName + " arrived.");

            ServiceStation.empty.waitSemaphore();

            ServiceStation.mutex.waitSemaphore();
            try {
                ServiceStation.waitingQueue.offer(this);
                ServiceStation.log(carName + " entered waiting area (Queue: " + ServiceStation.waitingQueue.size() + ")");
                ServiceStation.syncQueueToGUI();
            } finally {
                ServiceStation.mutex.signalSemaphore();
            }

            ServiceStation.full.signalSemaphore();

            ServiceStation.log(carName + " is waiting for service.");
            serviced.waitSemaphore();

            recordDeparture();
            ServiceStation.log(carName + " service completed, leaving. (Wait: " +
                    getWaitingTime() + "ms, Service: " + getServiceTime() + "ms, Total: " + getTotalTime() + "ms)");
            
            long workDuration = System.nanoTime() - workStart;
            ServiceStation.totalWorkTime.addAndGet(workDuration);

        } catch (Exception e) {
            ServiceStation.log(carName + " encountered an error: " + e.getMessage());
        }
    }
}

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
                ServiceStation.full.waitSemaphore();

                Car car;
                ServiceStation.mutex.waitSemaphore();
                try {
                    car = ServiceStation.waitingQueue.poll();
                } finally {
                    ServiceStation.mutex.signalSemaphore();
                }

                ServiceStation.empty.signalSemaphore();

                if (car != null) {
                    ServiceStation.pumps.waitSemaphore();

                    car.recordServiceStart();
                    ServiceStation.log("Pump " + pumpId + ": " + car.getCarName() + " begins service.");
                    
                    ServiceStation.updatePumpState(pumpId, car.getCarName(), 0);
                    ServiceStation.syncQueueToGUI();

                    long serviceDuration = (long) (Math.random() * 2000) + 1000;
                    long workStart = System.nanoTime();
                    long startTime = System.currentTimeMillis();
                    long endTime = startTime + serviceDuration;
                    
                    while (System.currentTimeMillis() < endTime) {
                        long elapsed = System.currentTimeMillis() - startTime;
                        int progress = (int) ((elapsed * 100) / serviceDuration);
                        if (progress > 100) progress = 100;
                        ServiceStation.updatePumpState(pumpId, car.getCarName(), progress);
                        Thread.sleep(100);
                    }
                    
                    ServiceStation.updatePumpState(pumpId, car.getCarName(), 100);

                    ServiceStation.log("Pump " + pumpId + ": " + car.getCarName() + " finishes service.");
                    car.serviceCompleted();

                    ServiceStation.log("Pump " + pumpId + ": Bay is now free.");
                    ServiceStation.pumps.signalSemaphore();
                    
                    ServiceStation.updatePumpState(pumpId, null, 0);
                    
                    long workDuration = System.nanoTime() - workStart;
                    ServiceStation.totalWorkTime.addAndGet(workDuration);
                }

            } catch (InterruptedException e) {
                ServiceStation.log("Pump " + pumpId + " interrupted.");
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}