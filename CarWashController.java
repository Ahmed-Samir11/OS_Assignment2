import java.util.List;
import java.util.Random;
import javax.swing.*;

public class CarWashController {
    private final CarWashModel model;
    private SimulatorWorker worker;
    private volatile int delayMs = 800; // base delay, adjustable by GUI slider

    public CarWashController(CarWashModel model) {
        this.model = model;
    }

    public void setDelayMs(int ms) { this.delayMs = Math.max(10, ms); }

    public void start() {
        if (worker != null && !worker.isDone()) return;
        worker = new SimulatorWorker();
        worker.execute();
        model.fireLog("Simulation started");
    }

    public void pause() {
        if (worker != null) worker.pauseWorker();
        model.fireLog("Simulation paused");
    }

    public void resume() {
        if (worker != null) worker.resumeWorker();
        model.fireLog("Simulation resumed");
    }

    public void stop() {
        if (worker != null) worker.cancel(true);
        model.fireLog("Simulation stopped");
    }

    // Simple simulation worker
    private class SimulatorWorker extends SwingWorker<Void, Void> {
        private volatile boolean paused = false;
        private final Random rnd = new Random();
        private int carCounter = 1;

        void pauseWorker() { paused = true; }
        void resumeWorker() { paused = false; }

        @Override
        protected Void doInBackground() throws Exception {
            int numPumps = model.getNumPumps();
            while (!isCancelled()) {
                // respect pause
                while (paused && !isCancelled()) Thread.sleep(100);

                // 1) Possibly create new car arrival
                if (rnd.nextDouble() < 0.6) {
                    String car = "C" + (carCounter++);
                    model.addToQueue(car);
                    model.fireLog("Arrived: " + car);
                }

                // 2) Try to assign queued cars to free pumps
                List<String> q = model.getQueueSnapshot();
                for (int i = 0; i < numPumps && !isCancelled(); i++) {
                    CarWashModel.PumpState ps = model.getPumpState(i);
                    if (!ps.occupied) {
                        String next = model.pollQueue();
                        if (next != null) {
                            boolean claimed = model.claimPump(i, next);
                            if (claimed) {
                                model.fireLog("Pump " + (i+1) + ": " + next + " started");
                                // simulate service in a dedicated thread so we can update progress
                                final int pumpId = i;
                                new Thread(() -> simulateService(pumpId)).start();
                            }
                        }
                    }
                }

                Thread.sleep(Math.max(50, delayMs));
            }
            return null;
        }

        private void simulateService(int pumpId) {
            try {
                int total = 4000; // simulated service time base (ms)
                int steps = 40; // more steps for smoother progress
                int stepMs = Math.max(10, total / steps);
                long start = System.currentTimeMillis();
                for (int p = 1; p <= steps; p++) {
                    if (isCancelled()) break;
                    long elapsed = System.currentTimeMillis() - start;
                    long remaining = Math.max(0L, total - elapsed);
                    int percent = Math.min(100, (p * 100) / steps);
                    model.setPumpProgress(pumpId, percent, remaining);
                    Thread.sleep(Math.max(10, stepMs * delayMs / 800));
                }
                model.fireLog("Pump " + (pumpId+1) + ": finished");
                model.releasePump(pumpId);
            } catch (InterruptedException ignored) {}
        }
    }
}
