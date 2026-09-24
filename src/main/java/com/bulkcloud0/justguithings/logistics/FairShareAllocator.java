package com.bulkcloud0.justguithings.logistics;

public final class FairShareAllocator {
    private FairShareAllocator() {
    }

    public static int[] allocate(int budget, int[] demands) {
        int[] allocations = new int[demands == null ? 0 : demands.length];
        if (budget <= 0 || demands == null || demands.length == 0) {
            return allocations;
        }

        int active = 0;
        for (int demand : demands) {
            if (demand > 0) {
                active++;
            }
        }

        int remaining = budget;
        while (remaining > 0 && active > 0) {
            int share = Math.max(1, remaining / active);
            boolean progressed = false;

            for (int index = 0; index < demands.length && remaining > 0; index++) {
                int demand = Math.max(0, demands[index]);
                int needed = demand - allocations[index];
                if (needed <= 0) {
                    continue;
                }

                int grant = Math.min(needed, Math.min(share, remaining));
                if (grant <= 0) {
                    continue;
                }

                allocations[index] += grant;
                remaining -= grant;
                progressed = true;

                if (allocations[index] >= demand) {
                    active--;
                }
            }

            if (!progressed) {
                break;
            }
        }

        return allocations;
    }
}
