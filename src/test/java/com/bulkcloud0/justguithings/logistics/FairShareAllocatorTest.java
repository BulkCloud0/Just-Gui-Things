package com.bulkcloud0.justguithings.logistics;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class FairShareAllocatorTest {
    @Test
    public void returnsEmptyAllocationsForNullDemands() {
        assertArrayEquals(new int[0], FairShareAllocator.allocate(10, null));
    }

    @Test
    public void returnsZeroAllocationsForNonPositiveBudget() {
        assertArrayEquals(new int[]{0, 0, 0},
                FairShareAllocator.allocate(0, new int[]{4, 5, 6}));
        assertArrayEquals(new int[]{0, 0, 0},
                FairShareAllocator.allocate(-5, new int[]{4, 5, 6}));
    }

    @Test
    public void ignoresZeroAndNegativeDemands() {
        assertArrayEquals(new int[]{0, 0, 4},
                FairShareAllocator.allocate(10, new int[]{-5, 0, 4}));
    }

    @Test
    public void splitsEqualDemandWithinOneUnit() {
        int[] allocations = FairShareAllocator.allocate(7, new int[]{10, 10, 10});

        assertArrayEquals(new int[]{3, 2, 2}, allocations);
        assertEquals(7, sum(allocations));
        assertEquals(1, max(allocations) - min(allocations));
    }

    @Test
    public void redistributesUnusedShareAfterSmallDemandIsSatisfied() {
        int[] allocations = FairShareAllocator.allocate(12, new int[]{2, 10, 10});

        assertArrayEquals(new int[]{2, 5, 5}, allocations);
        assertEquals(12, sum(allocations));
    }

    @Test
    public void neverAllocatesMoreThanTotalDemand() {
        int[] allocations = FairShareAllocator.allocate(100, new int[]{2, 3, 0});

        assertArrayEquals(new int[]{2, 3, 0}, allocations);
        assertEquals(5, sum(allocations));
    }

    @Test
    public void respectsBudgetAndPerTargetDemandAcrossRepresentativeInputs() {
        assertAllocationInvariant(1, new int[]{10, 10});
        assertAllocationInvariant(9, new int[]{1, 100, 3, 0});
        assertAllocationInvariant(25, new int[]{20, 20, 20});
        assertAllocationInvariant(Integer.MAX_VALUE, new int[]{1, 2, Integer.MAX_VALUE});
    }

    private static void assertAllocationInvariant(int budget, int[] demands) {
        int[] allocations = FairShareAllocator.allocate(budget, demands);

        long allocated = 0L;
        for (int index = 0; index < allocations.length; index++) {
            int demand = Math.max(0, demands[index]);
            if (allocations[index] < 0 || allocations[index] > demand) {
                throw new AssertionError("allocation outside demand at index " + index);
            }
            allocated += allocations[index];
        }

        assertEquals(Math.min((long) budget, positiveDemandTotal(demands)), allocated);
    }

    private static long positiveDemandTotal(int[] demands) {
        long total = 0L;
        for (int demand : demands) {
            total += Math.max(0, demand);
        }
        return total;
    }

    private static int sum(int[] values) {
        int total = 0;
        for (int value : values) {
            total += value;
        }
        return total;
    }

    private static int max(int[] values) {
        int result = Integer.MIN_VALUE;
        for (int value : values) {
            result = Math.max(result, value);
        }
        return result;
    }

    private static int min(int[] values) {
        int result = Integer.MAX_VALUE;
        for (int value : values) {
            result = Math.min(result, value);
        }
        return result;
    }
}
