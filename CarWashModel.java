import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

public class CarWashModel {
    public static class PumpState {
        public volatile String carName;
        public final AtomicInteger progress = new AtomicInteger(0); // 0..100
        public volatile boolean occupied = false;
        public volatile long remainingMs = 0L; // estimated remaining milliseconds
    }

    private final ConcurrentLinkedQueue<String> queue = new ConcurrentLinkedQueue<>();
    private final PumpState[] pumps;
    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);
    private final ReentrantLock[] pumpLocks;

    public CarWashModel(int numPumps) {
        pumps = new PumpState[numPumps];
        pumpLocks = new ReentrantLock[numPumps];
        for (int i = 0; i < numPumps; i++) {
            pumps[i] = new PumpState();
            pumpLocks[i] = new ReentrantLock();
        }
    }

    // Queue operations
    public void addToQueue(String car) {
        queue.add(car);
        fireQueueChanged();
    }

    public String pollQueue() {
        String s = queue.poll();
        if (s != null) fireQueueChanged();
        return s;
    }

    public List<String> getQueueSnapshot() {
        List<String> list = new ArrayList<>(queue);
        return Collections.unmodifiableList(list);
    }

    // Pump operations
    public boolean claimPump(int pumpId, String carName) {
        if (pumpId < 0 || pumpId >= pumps.length) return false;
        ReentrantLock lock = pumpLocks[pumpId];
        lock.lock();
        try {
            PumpState p = pumps[pumpId];
            if (p.occupied) return false;
            p.occupied = true;
            p.carName = carName;
            p.progress.set(0);
        } finally {
            lock.unlock();
        }
        // notify outside lock
        firePumpChanged(pumpId);
        return true;
    }

    // set progress only
    public void setPumpProgress(int pumpId, int percent) {
        setPumpProgress(pumpId, percent, 0L);
    }

    // set progress and remaining time (ms)
    public void setPumpProgress(int pumpId, int percent, long remainingMs) {
        if (pumpId < 0 || pumpId >= pumps.length) return;
        PumpState p = pumps[pumpId];
        p.progress.set(percent);
        p.remainingMs = Math.max(0L, remainingMs);
        firePumpChanged(pumpId);
    }

    public void releasePump(int pumpId) {
        if (pumpId < 0 || pumpId >= pumps.length) return;
        ReentrantLock lock = pumpLocks[pumpId];
        lock.lock();
        try {
            PumpState p = pumps[pumpId];
            p.occupied = false;
            p.carName = null;
            p.progress.set(0);
        } finally {
            lock.unlock();
        }
        firePumpChanged(pumpId);
    }

    public PumpState getPumpState(int pumpId) {
        return pumps[pumpId];
    }

    public int getNumPumps() { return pumps.length; }

    // Property change support
    public void addPropertyChangeListener(PropertyChangeListener l) { pcs.addPropertyChangeListener(l); }
    public void removePropertyChangeListener(PropertyChangeListener l) { pcs.removePropertyChangeListener(l); }

    private void fireQueueChanged() {
        pcs.firePropertyChange("queue", null, getQueueSnapshot());
    }

    private void firePumpChanged(int pumpId) {
        pcs.firePropertyChange("pump" + pumpId, null, pumps[pumpId]);
    }

    // For logging convenience
    public void fireLog(String message) { fireLogWithLevel("INFO", message); }

    public void fireLogWithLevel(String level, String message) {
        String full = level + ": " + message;
        pcs.firePropertyChange("log", null, full);
    }
}
