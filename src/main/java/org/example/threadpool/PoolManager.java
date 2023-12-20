package org.example.threadpool;

import java.util.Vector;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.IntStream;

public class PoolManager {
    private volatile boolean isShutdown = false;
    private final Object managerLock = new Object();  //object for synchronizing public methods
    private final Thread managerThread; //manager thread that distributes requests among pool threads
    private final Vector<PoolThread> threads = new Vector<>(); //pool threads
    private final BlockingQueue<PoolThread> freeThreads = new LinkedBlockingQueue<>(); //free thread pool queue
    private final BlockingQueue<Request> requests = new LinkedBlockingQueue<>(); //queue of requests for processing

    public PoolManager(int size) {
        IntStream.range(0, size).forEach(i -> threads.add(new PoolThread())); //creating <size> pool threads
        freeThreads.addAll(threads);
        managerThread.start();
    }


    {
        managerThread = new Thread(() -> {
            while (!isShutdown) {
                try {
                    freeThreads.take() //wait and take free thread
                            .executeRequest(requests.take()); //take request and pass it to a thread
                } catch (Exception ignored) {
                }
            }
        });
    }

    public synchronized void addRequest(Request request) {
        synchronized (managerLock) {
            if (isShutdown) throw new RuntimeException("ThreadPool is shutdown.");
            try {
                requests.put(request);
            } catch (Exception exception) {
                throw new RuntimeException("Failed to add request to queue.", exception);
            }
        }
    }

    public synchronized void shutdown() {
        synchronized (managerLock) {
            isShutdown = true;
            managerThread.interrupt(); //interrupt manager thread to remove the block from freeThreads and requests
            notifyThreads();
            cancelRequests();
        }
    }

    //cancel all requests in the requests queue
    private void cancelRequests() {
        try {
            while (!requests.isEmpty()) requests.take().onCancel(); //
        } catch (Exception ignored) {
        }
    }

    //free thread when execution is complete
    private void freeThread(PoolThread thread) throws InterruptedException {
        freeThreads.put(thread);
    }

    //notify all threads in the pool (used to stop them running)
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
                        waitForRequest(); //block the thread until the executeRequest() method is called from outside
                        if (request == null) continue;
                        processRequest(request);
                        request = null;
                        freeThread(this); //free the pool thread after request completes
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
