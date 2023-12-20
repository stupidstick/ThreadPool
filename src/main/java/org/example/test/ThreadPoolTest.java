package org.example.test;

import org.example.threadpool.PoolManager;
import org.example.threadpool.PoolRequest;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

public class ThreadPoolTest {
    private static class TestRequest extends PoolRequest {
        private final int n;
        private final AtomicInteger counter;
        public TestRequest(int n, AtomicInteger counter) {
            this.n = n;
            this.counter = counter;
        }
        @Override
        public void execute() {
            AllDivisors.findAllDivisors(n);
        }

        @Override
        public void onFinish() {
            counter.incrementAndGet();
        }
    }

    public static void test(int size, int poolThreadsCount) {
        Random random = new Random();
        int[] numbers = new int[size];
        for (int i = 0; i < size; i++) {
            numbers[i] = random.nextInt(10, 50);
        }

        System.out.println("--------------");
        System.out.println("number of numbers: " + size + "; number of pool threads: " + poolThreadsCount);
        //sequential test
        AtomicInteger count = new AtomicInteger(0);
        long t1 = System.currentTimeMillis();
        for (int number : numbers) {
            var request = new TestRequest(number, count);
            request.execute();
            request.onFinish();
        }
        long t2 = System.currentTimeMillis();
        System.out.println("sequental: " + (t2 - t1));

        //parallel test
        count.set(0);
        t1 = System.currentTimeMillis();
        for (int number : numbers) {
            new Thread(() -> {
                var request = new TestRequest(number, count);
                request.execute();
                request.onFinish();
            }).start();

        }
        while (count.get() != size);
        System.out.println("parallel: " + (System.currentTimeMillis() - t1));

        //thread pool
        count.set(0);
        t1 = System.currentTimeMillis();
        PoolManager manager = new PoolManager(poolThreadsCount);
        for (int number : numbers) {
            manager.addRequest(new TestRequest(number, count));
        }
        while (count.get() != size);
        t2 = System.currentTimeMillis();
        System.out.println("thread pool: " + (t2 - t1));
        manager.shutdown();
        System.out.println("--------------");
    }
}
