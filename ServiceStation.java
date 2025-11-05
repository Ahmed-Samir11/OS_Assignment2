import java.util.InputMismatchException;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Scanner;
public class ServiceStation {

    // --- Shared Resources ---
    public static Queue<Car> waitingQueue;
    public static Semaphore mutex; 
    public static Semaphore empty;
    public static Semaphore full;
    public static Semaphore pumps;

    /**
     * A synchronized logging method to prevent interleaved console output.
     * All threads should use this to print their status.
     * @param message The message to log.
     */
    public static synchronized void log(String message) {
        System.out.println(message);
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        // 1. User input for capacities
        System.out.print("Enter Waiting area capacity (1-10): ");
        int waitingCapacity = scanner.nextInt();

        // Validate queue size
        while (true) {
            try {
                System.out.print("Enter Waiting area capacity (1-10): ");
                waitingCapacity = scanner.nextInt();

                if (waitingCapacity >= 1 && waitingCapacity <= 10) {
                    break;
                } else {
                    log("Invalid capacity. Value must be between 1 and 10.");
                }
            } 
            catch (InputMismatchException e) {
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

        for (int i = 0; i < numPumps; i++) {
            Pump pump = new Pump(i + 1);
            Thread pumpThread = new Thread(pump, "Pump-" + (i + 1));
            pumpThread.start();
        }
        int carId = 1;
        while (true) {
            String carName = "C" + carId++;
            Car car = new Car(carName);
            Thread carThread = new Thread(car, carName);
            carThread.start();
            try {
                Thread.sleep((long) (Math.random() * 1000) + 500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
       /*   
        String[] carArrivals = {"C1", "C2", "C3", "C4", "C5"};
        
        for (String carName : carArrivals) {
            Car car = new Car(carName);
            Thread carThread = new Thread(car, carName);
            carThread.start();
            
            // Short Delay between cars
            try {
                Thread.sleep((long) (Math.random() * 1000) + 500);
            } catch (InterruptedException e) {
                
            }
        }        
        */
        
        scanner.close();
    }
}