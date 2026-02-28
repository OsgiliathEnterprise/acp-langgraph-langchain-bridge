package com.example.concurrent;

/**
 * A simple Thread implementation for testing purposes.
 * This class demonstrates the Observer pattern in a concurrent context.
 *
 * Key characteristics:
 * - Implements Runnable interface
 * - Uses Observer pattern for state notifications
 * - Thread-safe operation tracking
 */
public class Thread implements Runnable {

    private final String name;
    private final ObserverList observers;
    private volatile boolean running;

    /**
     * Creates a new Thread with the specified name.
     * Design pattern: Observer pattern for state management
     *
     * @param name the thread name
     */
    public Thread(String name) {
        this.name = name;
        this.running = false;
        this.observers = new ObserverList();
    }

    @Override
    public void run() {
        this.running = true;
        notifyObservers("Thread " + name + " started");

        // Simulate work
        performWork();

        this.running = false;
        notifyObservers("Thread " + name + " completed");
    }

    private void performWork() {
        // Placeholder for actual work
        System.out.println("Thread " + name + " is working");
    }

    private void notifyObservers(String message) {
        observers.notifyAll(message);
    }

    public boolean isRunning() {
        return running;
    }

    public String getName() {
        return name;
    }

    /**
     * Observer list for managing thread state observers.
     */
    private static class ObserverList {
        public void notifyAll(String message) {
            // Notify all registered observers
        }
    }
}

