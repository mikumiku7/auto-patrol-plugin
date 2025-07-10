package com.github.mikumiku7;

import java.util.ArrayList;

/**
 * Example configuration POJO.
 * <p>
 * Configurations are saved and loaded to JSON files
 * <p>
 * All fields should be public and mutable.
 * <p>
 * Fields to static inner classes generate nested JSON objects.
 */
public class AutoPatrolConfig {

    public boolean enabled = true;
    public final ArrayList<PatrolPoint> patrolPoints = new ArrayList<>();
    public int stuckDetectionSeconds = 3;
    public double arrivalThreshold = 2;
    public boolean enableUnstuckActions = true;
    public boolean enableJumpUnstuck = true;
    public boolean enableMovementUnstuck = true;
    public int unstuckActionDurationTicks = 10;

    public static class PatrolPoint {
        public int x;
        public int y;
        public int z;

        public PatrolPoint() {
            this.x = 0;
            this.y = 120;
            this.z = 0;
        }

        public PatrolPoint(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }
}
