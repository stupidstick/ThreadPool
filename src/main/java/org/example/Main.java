package org.example;
import org.example.test.ThreadPoolTest;


public class Main {
    public static void main(String[] args) {
        ThreadPoolTest.test(100, 8);
        ThreadPoolTest.test(10000, 8);
        ThreadPoolTest.test(100000, 8);
    }
}