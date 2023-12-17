package org.example.threadpool;

import java.util.Vector;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.IntStream;

public class PoolManager {
    private boolean isShutdown = false;
    private final Thread managerThread;
    private final Vector<PoolThread> threads = new Vector<>();
    private final BlockingQueue<PoolThread> freeThreads = new LinkedBlockingQueue<>();
    private final BlockingQueue<Request> requests = new LinkedBlockingQueue<>();

    public PoolManager(int size) {
        IntStream.range(0, size).forEach(i -> threads.add(new PoolThread()));
        freeThreads.addAll(threads);
        managerThread.start();
    }


    {
        managerThread = new Thread(() -> {
            while (!isShutdown) {
                try {
                    freeThreads.take().executeRequest(requests.take());
                } catch (Exception ignored) {
                }
            }
        });
    }

    public void addRequest(Request request) {
        if (isShutdown) throw new RuntimeException("ThreadPool is shutdown.");
        try {
            requests.put(request);
        } catch (Exception exception) {
            throw new RuntimeException("Failed to add request to queue.", exception);
        }
    }

    public void shutdown() {
        isShutdown = true;
        managerThread.interrupt();
        notifyThreads();
        cancelRequests();
    }

    private void cancelRequests() {
        try {
            while (!requests.isEmpty()) requests.take().onCancel();
        } catch (Exception ignored) {
        }
    }

    private void freeThread(PoolThread thread) throws InterruptedException {
        freeThreads.put(thread);
    }

    private void notifyThreads() {
        threads.forEach(t -> {
            synchronized (t.lockObject) {
                t.lockObject.notify();
            }
        });
    }

    private class PoolThread {
        private final Runnable runnable;
        private final Object lockObject = new Object();
        private Request request;

        public PoolThread() {
            new Thread(runnable).start();
        }

        {
            runnable = () -> {
                while (!isShutdown) {
                    try {
                        waitForRequest();
                        if (request == null) continue;
                        processRequest(request);
                        request = null;
                        freeThread(this);

                    } catch (InterruptedException ignored) {
                    }
                }
            };
        }

        public void executeRequest(Request request) {
            synchronized (lockObject) {
                this.request = request;
                lockObject.notify();
            }
        }

        private void processRequest(Request request) {
            try {
                request.execute();
                request.onFinish();
            } catch (Exception exception) {
                request.onException();
            }
        }

        private void waitForRequest() throws InterruptedException {
            synchronized (lockObject) {
                if (request == null)
                    lockObject.wait();
            }
        }
    }
}
