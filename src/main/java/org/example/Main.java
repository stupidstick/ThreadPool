package org.example;
import org.example.test.AllDivisors;
import org.example.threadpool.PoolManager;
import org.example.threadpool.PoolRequest;

import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

public class Main {
    public static void main(String[] args) {
        int size = 50;
        AtomicInteger count = new AtomicInteger(0);
        Random random = new Random();
        int[] numbers = new int[size];
        for (int i = 0; i < size; i++) {
            numbers[i] = random.nextInt(5000000, 10000000);
        }

        PoolRequest[] requests = new PoolRequest[size];
        int[][] result = new int[size][];
        for (int i = 0; i < size; i++) {
            final int n = i;
            requests[n] = new PoolRequest() {
                @Override
                public void execute() {
                    result[n] = AllDivisors.findAllDivisors(numbers[n]);
                }
                @Override
                public void onFinish() {
                    count.incrementAndGet();
                }

                @Override
                public String toString() {
                    return "n = " + n;
                }
            };
        }

        long t1 = System.currentTimeMillis();
        for (PoolRequest request : requests) {
            request.execute();
            request.onFinish();
        }
        long t2 = System.currentTimeMillis();
        System.out.println("sequental: " + (t2 - t1));
        count.set(0);
        PoolManager manager = new PoolManager(5);
        t1 = System.currentTimeMillis();
        for (PoolRequest request : requests) {
            manager.addRequest(request);
        }
        while (count.get() != size) {
        }
        t2 = System.currentTimeMillis();
        System.out.println("parallel: " + (t2 - t1));
        manager.shutdown();
    }
}