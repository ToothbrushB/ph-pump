package com.calebli.phpump;

import java.util.concurrent.TimeUnit;

public class Watchdog {
    private long lastFed;
    private long threshold;
    private Runnable action;
    private Status status = Status.READY;
    private Thread check;

    public Watchdog(long threshold, Runnable action) {
        this.threshold = threshold;
        this.action = action;
    }

    public void start() {
        status = Status.RUNNING;
        feed();
        check = new Thread(() -> {
            Thread t = null;
            while (true) {
                if (System.currentTimeMillis() - lastFed > threshold && status == Status.RUNNING) { // uh oh
                    System.out.println(System.currentTimeMillis() - lastFed);
                    t = new Thread(action);
                    t.start();
                    status = Status.TRIGGERED;
                }
                if (t != null && System.currentTimeMillis() - lastFed < threshold && t.isAlive()) {
                    t.interrupt();
                }
                try {
                    TimeUnit.MILLISECONDS.sleep(10);
                } catch (InterruptedException e) {
                    if (t != null)
                        t.interrupt();
                    Thread.currentThread().interrupt();
                    break;
                }
                if (status == Status.STOPPED || Thread.currentThread().isInterrupted()) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        check.start();
    }

    public void stop() {
        check.interrupt();
        status = Status.STOPPED;
        System.out.println("Dog stopped");
    }

    public void feed() {
//        System.out.println("FED");
        if (status == Status.RUNNING || status == Status.TRIGGERED) {
            lastFed = System.currentTimeMillis();
            status = Status.RUNNING;
        }
    }

    public Status getStatus() {
        return status;
    }

    public enum Status {RUNNING, TRIGGERED, READY, STOPPED}
}
