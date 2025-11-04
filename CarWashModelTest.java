public class CarWashModelTest {
    public static void main(String[] args) throws Exception {
        CarWashModel model = new CarWashModel(3);

        // simple concurrency test: multiple producers
        Thread producer1 = new Thread(() -> {
            for (int i = 0; i < 50; i++) model.addToQueue("P1-" + i);
        });
        Thread producer2 = new Thread(() -> {
            for (int i = 0; i < 50; i++) model.addToQueue("P2-" + i);
        });

        producer1.start();
        producer2.start();
        producer1.join();
        producer2.join();

        if (model.getQueueSnapshot().size() != 100) {
            System.err.println("Queue size mismatch: " + model.getQueueSnapshot().size());
            System.exit(2);
        }

        // claim and release pumps concurrently
        Thread[] workers = new Thread[3];
        for (int i = 0; i < 3; i++) {
            final int id = i;
            workers[i] = new Thread(() -> {
                String car;
                while ((car = model.pollQueue()) != null) {
                    boolean ok = model.claimPump(id, car);
                    if (ok) {
                        try {
                            // simulate quick work
                            for (int p = 0; p <= 100; p += 25) {
                                model.setPumpProgress(id, p);
                                Thread.sleep(5);
                            }
                        } catch (InterruptedException ignored) {}
                        model.releasePump(id);
                    }
                }
            });
            workers[i].start();
        }
        for (Thread w : workers) w.join();

        if (!model.getQueueSnapshot().isEmpty()) {
            System.err.println("Queue not empty after processing: " + model.getQueueSnapshot().size());
            System.exit(3);
        }

        System.out.println("CarWashModelTest: PASS");
    }
}
