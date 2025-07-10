package com.github.mikumiku7.module;


import com.github.mikumiku7.AutoPatrolConfig;
import com.github.mikumiku7.AutoPatrolConfig.PatrolPoint;
import com.github.mikumiku7.AutoPatrolPlugin;
import com.zenith.Proxy;
import com.zenith.event.client.ClientBotTick;
import com.zenith.feature.pathfinder.goals.Goal;
import com.zenith.feature.pathfinder.goals.GoalBlock;
import com.zenith.feature.player.Input;
import com.zenith.feature.player.InputRequest;
import com.zenith.module.api.Module;
import com.zenith.util.math.MathHelper;
import com.zenith.util.timer.Timer;
import com.zenith.util.timer.Timers;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import static com.github.rfresh2.EventConsumer.of;
import static com.zenith.Globals.*;

public class AutoPatrol extends Module {
    private final Timer stuckTimer = Timers.tickTimer();
    private final Timer unstuckActionTimer = Timers.tickTimer();
    public static final int MOVEMENT_PRIORITY = 160;

    private final Timer tickTimer = Timers.tickTimer();


    // 巡逻状态
    private int currentPatrolIndex = 0;
    private double lastX = 0;
    private double lastY = 0;
    private double lastZ = 0;
    private long lastPositionChangeTime = 0;
    private boolean isUnstucking = false;
    private ScheduledFuture<?> unstuckActionFuture = null;

    private long tickCounter = 0;

    @Override
    public void subscribeEvents() {
        EVENT_BUS.subscribe(
                this,
                of(ClientBotTick.class, this::handleBotTick),
                of(ClientBotTick.Starting.class, this::handleBotTickStarting)
        );
    }

    @Override
    public boolean enabledSetting() {
        return AutoPatrolPlugin.PLUGIN_CONFIG.enabled;
    }

    @Override
    public void onDisable() {
        BARITONE.stop();
        currentPatrolIndex = 0;
        isUnstucking = false;
        if (unstuckActionFuture != null) {
            unstuckActionFuture.cancel(true);
            unstuckActionFuture = null;
        }
    }

    private void handleBotTickStarting(ClientBotTick.Starting event) {
        stuckTimer.reset();
        unstuckActionTimer.reset();
        lastPositionChangeTime = System.currentTimeMillis();
        tickCounter = 0;

        lastX = 0;
        lastY = 0;
        lastZ = 0;

        //是否允许破坏方块（如树木、墙壁）
        CONFIG.client.extra.pathfinder.allowBreak = false;
        //是否允许放置方块（如搭桥、垫脚）
        CONFIG.client.extra.pathfinder.allowPlace = false;
    }

    private void handleBotTick(ClientBotTick event) {
        if (!AutoPatrolPlugin.PLUGIN_CONFIG.enabled) return;

        tickCounter++;

        // 每10 tick执行一次（假设onTick每秒被调用20次）
        if (tickCounter % 10 != 0) {
            return;
        }

        List<AutoPatrolConfig.PatrolPoint> patrolPoints = AutoPatrolPlugin.PLUGIN_CONFIG.patrolPoints;
        if (patrolPoints.isEmpty()) {
            debug("No patrol points configured");
            return;
        }

        // 检查位置变化
        checkPositionChange();

        // 检查是否卡住
        if (checkIfStuck()) {
            handleStuck();
            return;
        }

        // 如果正在解锁操作，不进行巡逻
        if (isUnstucking) {
            return;
        }

        // 执行巡逻逻辑
        performPatrol(patrolPoints);
    }

    private void checkPositionChange() {
        double currentX = CACHE.getPlayerCache().getX();
        double currentY = CACHE.getPlayerCache().getY();
        double currentZ = CACHE.getPlayerCache().getZ();

        double distance = MathHelper.manhattanDistance3d(lastX, lastY, lastZ, currentX, currentY, currentZ);

        if (distance > 1) { // 有显著位移
            lastPositionChangeTime = System.currentTimeMillis();
            lastX = currentX;
            lastY = currentY;
            lastZ = currentZ;
        }
    }

    private boolean checkIfStuck() {
        long stuckTime = System.currentTimeMillis() - lastPositionChangeTime;
        int stuckDetectionMs = AutoPatrolPlugin.PLUGIN_CONFIG.stuckDetectionSeconds * 1000;

        return stuckTime > stuckDetectionMs && !isUnstucking;
    }

    private void handleStuck() {
        if (!AutoPatrolPlugin.PLUGIN_CONFIG.enableUnstuckActions) {
            warn("Bot appears to be stuck for {} seconds", AutoPatrolPlugin.PLUGIN_CONFIG.stuckDetectionSeconds);
            return;
        }

        info("Bot appears to be stuck, attempting unstuck actions");
        isUnstucking = true;

        // 停止当前pathfinder
        BARITONE.stop();

        // 执行解锁操作
        performUnstuckActions();
    }

    private void performUnstuckActions() {
        if (unstuckActionFuture != null && !unstuckActionFuture.isDone()) {
            unstuckActionFuture.cancel(true);
        }

        unstuckActionFuture = Proxy.getInstance().getClient().getClientEventLoop().scheduleAtFixedRate(() -> {
            if (!isUnstucking) {
                unstuckActionFuture.cancel(true);
                return;
            }

            // 跳跃解锁
            if (AutoPatrolPlugin.PLUGIN_CONFIG.enableJumpUnstuck) {
                var jumpInput = Input.builder()
                        .jumping(true)
                        .build();
                var jumpRequest = InputRequest.builder()
                        .owner(this)
                        .input(jumpInput)
                        .priority(MOVEMENT_PRIORITY)
                        .build();
                INPUTS.submit(jumpRequest);
            }

            // 轻微移动解锁
            if (AutoPatrolPlugin.PLUGIN_CONFIG.enableMovementUnstuck) {
                // 轻微移动解锁
                // 随机选择前/后/左/右方向
                int dir = ThreadLocalRandom.current().nextInt(4);
                double dx = 0, dz = 0;
                switch (dir) {
                    case 0 -> dx = 0.5;   // forward (positive X)
                    case 1 -> dx = -0.5;  // backward (negative X)
                    case 2 -> dz = 0.5;   // right (positive Z)
                    case 3 -> dz = -0.5;  // left (negative Z)
                }
                double px = CACHE.getPlayerCache().getX() + dx;
                double py = CACHE.getPlayerCache().getY();
                double pz = CACHE.getPlayerCache().getZ() + dz;
                Goal moveGoal = new GoalBlock((int) Math.round(px), (int) Math.round(py), (int) Math.round(pz));
                BARITONE.pathTo(moveGoal);
            }

        }, 0, 50, TimeUnit.MILLISECONDS);

        // 设置解锁操作持续时间
        Proxy.getInstance().getClient().getClientEventLoop().schedule(() -> {
            if (unstuckActionFuture != null) {
                unstuckActionFuture.cancel(true);
                unstuckActionFuture = null;
            }
            isUnstucking = false;
            lastPositionChangeTime = System.currentTimeMillis(); // 重置卡住检测
            info("Unstuck actions completed");
        }, AutoPatrolPlugin.PLUGIN_CONFIG.unstuckActionDurationTicks * 50L, TimeUnit.MILLISECONDS);
    }

    private void performPatrol(List<PatrolPoint> patrolPoints) {
        if (patrolPoints.isEmpty()) return;

        // 获取当前目标点
        PatrolPoint currentPoint = patrolPoints.get(currentPatrolIndex);

        // 检查是否到达当前目标点
        double distanceToTarget = MathHelper.manhattanDistance3d(
                CACHE.getPlayerCache().getX(),
                CACHE.getPlayerCache().getY(),
                CACHE.getPlayerCache().getZ(),
                currentPoint.x,
                currentPoint.y,
                currentPoint.z
        );

        if (distanceToTarget <= AutoPatrolPlugin.PLUGIN_CONFIG.arrivalThreshold) {
            // 到达目标点，切换到下一个
            info("Reached patrol point {}: ({}, {}, {})",
                    currentPatrolIndex + 1, currentPoint.x, currentPoint.y, currentPoint.z);

            currentPatrolIndex = (currentPatrolIndex + 1) % patrolPoints.size();

            // 获取下一个目标点
            PatrolPoint nextPoint = patrolPoints.get(currentPatrolIndex);
            info("Moving to next patrol point {}: ({}, {}, {})",
                    currentPatrolIndex + 1, nextPoint.x, nextPoint.y, nextPoint.z);
        }

        // 如果pathfinder不活跃，开始移动到当前目标点
        if (!BARITONE.isActive()) {
            PatrolPoint targetPoint = patrolPoints.get(currentPatrolIndex);
            Goal goal = new GoalBlock(targetPoint.x, targetPoint.y, targetPoint.z);

            debug("Pathing to patrol point {}: ({}, {}, {})",
                    currentPatrolIndex + 1, targetPoint.x, targetPoint.y, targetPoint.z);

            BARITONE.pathTo(goal);
        }
    }

    public void addPatrolPoint(int x, int y, int z) {
        AutoPatrolPlugin.PLUGIN_CONFIG.patrolPoints.add(
                new PatrolPoint(x, y, z)
        );
        info("Added patrol point: ({}, {}, {})", x, y, z);
    }

    public void clearPatrolPoints() {
        AutoPatrolPlugin.PLUGIN_CONFIG.patrolPoints.clear();
        currentPatrolIndex = 0;
        info("Cleared all patrol points");
    }

    public void setCurrentPatrolIndex(int index) {
        if (index >= 0 && index < AutoPatrolPlugin.PLUGIN_CONFIG.patrolPoints.size()) {
            currentPatrolIndex = index;
            info("Set current patrol index to {}", index + 1);
        } else {
            warn("Invalid patrol index: {}", index + 1);
        }
    }

    public List<PatrolPoint> getPatrolPoints() {
        return new ArrayList<>(AutoPatrolPlugin.PLUGIN_CONFIG.patrolPoints);
    }

    public int getCurrentPatrolIndex() {
        return currentPatrolIndex;
    }

    public boolean isPatrolling() {
        return AutoPatrolPlugin.PLUGIN_CONFIG.enabled && !AutoPatrolPlugin.PLUGIN_CONFIG.patrolPoints.isEmpty();
    }
}
