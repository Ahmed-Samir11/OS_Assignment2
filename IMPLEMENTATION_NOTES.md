# CS241 Assignment 2: Car Wash Simulation - Implementation Notes

## Overview
This implementation fulfills all requirements of the Car Wash Simulation assignment, including the bonus GUI requirement.

## Implementation Summary

### 1. ServiceStation (Main Class) ✓
- Initializes shared resources: `waitingQueue`, `mutex`, `empty`, `full`, `pumps` semaphores
- Creates and starts `numPumps` Pump (consumer) threads as daemon threads
- Generates exactly 20 Car (producer) threads with random arrival times (0.5-1.5s)
- Provides synchronized `log()` method to prevent interleaved console output
- Integrates GUI through bridge methods (`updatePumpState`, `syncQueueToGUI`)

### 2. Semaphore Class ✓
- Custom counting semaphore implementation
- `waitSemaphore()` (P operation): decrements value, blocks if value ≤ 0
- `signalSemaphore()` (V operation): increments value, wakes waiting threads
- Uses `synchronized`, `wait()`, and `notifyAll()` for thread coordination
- Follows blocking (not busy-waiting) implementation as per lab requirements

### 3. Car (Producer) Thread ✓
- Implements `Runnable`
- **Synchronization Protocol**:
  1. `empty.waitSemaphore()` — wait for free slot in waiting area
  2. `mutex.waitSemaphore()` — acquire lock on queue
  3. Add self to `waitingQueue`
  4. `mutex.signalSemaphore()` — release queue lock
  5. `full.signalSemaphore()` — signal car availability
  6. Wait on private `serviced` semaphore until pump signals completion
- Tracks arrival, service start, and departure times for statistics
- Updates GUI queue state after entering waiting area

### 4. Pump (Consumer) Thread ✓
- Implements `Runnable`
- **Synchronization Protocol**:
  1. `full.waitSemaphore()` — wait for car in queue
  2. `mutex.waitSemaphore()` — acquire lock on queue
  3. Remove car from `waitingQueue`
  4. `mutex.signalSemaphore()` — release queue lock
  5. `empty.signalSemaphore()` — signal free slot available
  6. `pumps.waitSemaphore()` — acquire service bay
  7. Service car (1-3 seconds) with progress updates
  8. Signal car's private semaphore (service complete)
  9. `pumps.signalSemaphore()` — release service bay
- Reports all state changes to console and GUI
- Updates GUI pump state throughout service (progress 0-100%)

## Synchronization Correctness

### Producer-Consumer Pattern
- **Bounded Buffer**: `waitingQueue` with capacity 1-10 (user input)
- **Mutual Exclusion**: `mutex` semaphore (binary, initial=1) protects queue access
- **Resource Counting**:
  - `empty` (initial=capacity): counts free slots
  - `full` (initial=0): counts occupied slots
  - `pumps` (initial=numPumps): counts available service bays

### Critical Sections
All queue modifications are protected by `mutex`:
```java
// Car entering queue
mutex.waitSemaphore();
try {
    waitingQueue.offer(this);
    log(...);
    syncQueueToGUI();
} finally {
    mutex.signalSemaphore();
}

// Pump removing from queue
mutex.waitSemaphore();
try {
    car = waitingQueue.poll();
} finally {
    mutex.signalSemaphore();
}
```

## GUI Implementation (Bonus) ✓

### Architecture
- **CarWashModel**: Thread-safe model with property-change notifications
- **CarWashGUI**: Swing view with vertical progress bars, queue list, activity log
- **Bridge Integration**: ServiceStation calls GUI update methods:
  - `updatePumpState(pumpId, carName, progress)` — update pump visualization
  - `syncQueueToGUI()` — refresh waiting queue display
  - `log(message)` — append to activity log

### Visual Features
- Vertical progress bars with dynamic color coding (red→orange→green)
- Car icons and labels showing current service progress
- Real-time queue display (JList)
- Activity log with timestamps and filtering
- Split-pane responsive layout

## Output Compliance

The console output matches the required format:
```
Car-1 arrived.
Car-1 entered waiting area (Queue: 1)
Car-1 is waiting for service.
Pump 1: Car-1 begins service.
Pump 1: Car-1 finishes service.
Pump 1: Bay is now free.
Car-1 service completed, leaving. (Wait: Xms, Service: Yms, Total: Zms)
```

## Testing
- ✓ Compiles without errors: `javac ServiceStation.java`
- ✓ Runs with GUI: `java ServiceStation`
- ✓ Accepts user input for capacity (1-10) and pumps (≥1)
- ✓ Generates exactly 20 cars then terminates car generation
- ✓ No race conditions (queue operations are properly synchronized)
- ✓ No deadlocks (semaphore order is consistent)

## Files Submitted
- `ServiceStation.java` — contains all required classes:
  - `ServiceStation` (main class)
  - `Semaphore` (custom semaphore)
  - `Car` (producer)
  - `Pump` (consumer)
- `CarWashModel.java` — GUI model (bonus)
- `CarWashGUI.java` — GUI view (bonus)
- `CarWashController.java` — GUI controller (bonus, not used in ServiceStation)
- `README.md` — minimal course identification

## Academic Integrity
All code written by team members. Synchronization patterns follow CS241 lab materials and the provided producer-consumer semaphore example.

---
Faculty of Computers and Artificial Intelligence, Cairo University  
Academic Year: 2025-2026
