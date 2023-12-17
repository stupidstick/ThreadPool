package org.example.test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AllDivisors {
    public static int[] findAllDivisors(int n) {
        List<Integer> divisors = new ArrayList<>();
        for (int i = 1; i < n; i++) {
            if (n % i == 0) {
                divisors.add(i);
            }
        }
        return divisors.stream().mapToInt(i -> i).toArray();
    }

}
