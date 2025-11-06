public class Pump implements Runnable {
    private int pumpId;

    public Pump(int id) {
        this.pumpId = id;
    }

    @Override
    public void run() {
        while (true) {
            try {
                // Wait until there is at least one car in the waiting queue
                ServiceStation.full.waitSemaphore();

                // Lock the queue using mutex before accessing it
                ServiceStation.mutex.waitSemaphore();

                // Remove the first car from the waiting queue
                Car car = ServiceStation.waitingQueue.poll();

                // Signal that one more space is available in the waiting area
                ServiceStation.empty.signalSemaphore();

                // Release the mutex after updating the queue
                ServiceStation.mutex.signalSemaphore();

                if (car != null) {
                    ServiceStation.log("Pump " + pumpId + ": " + car.getCarName() + " login");

                    // Request permission to use a service bay (pump)
                    ServiceStation.pumps.waitSemaphore();

                    // Start servicing the car
                    ServiceStation.log("Pump " + pumpId + ": " + car.getCarName() +
                            " begins service at Bay " + pumpId);

                    // Simulate service time between 1 and 3 seconds
                    Thread.sleep((long) (Math.random() * 2000) + 1000);

                    // Service completed
                    ServiceStation.log("Pump " + pumpId + ": " + car.getCarName() + " finishes service");
                    ServiceStation.log("Pump " + pumpId + ": Bay " + pumpId + " is now free");

                    // Release the service bay for another car
                    ServiceStation.pumps.signalSemaphore();
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}
