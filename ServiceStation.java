import java.util.InputMismatchException;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Scanner;
import javax.swing.SwingUtilities;

public class ServiceStation {

    public static Queue<Car> waitingQueue;
    public static Semaphore mutex;
    public static Semaphore empty;
    public static Semaphore full;
    public static Semaphore pumps;
    
    private static CarWashModel guiModel = null;
    private static CarWashGUI gui = null;

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
        
        scanner.close();
    }
}

class Semaphore {
    private int value;

    public Semaphore(int value) {
        if (value < 0) throw new IllegalArgumentException("Semaphore value must be non-negative.");
        this.value = value;
    }

    public synchronized void waitSemaphore() {
        while (value <= 0) {
            try {
                wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("Thread interrupted during waitSemaphore.");
            }
        }
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
                }

            } catch (InterruptedException e) {
                ServiceStation.log("Pump " + pumpId + " interrupted.");
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}