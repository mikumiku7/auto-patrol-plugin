package com.github.mikumiku7.command;


import com.github.mikumiku7.AutoPatrolPlugin;
import com.github.mikumiku7.module.AutoPatrol;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.command.api.Command;
import com.zenith.command.api.CommandCategory;
import com.zenith.command.api.CommandContext;
import com.zenith.command.api.CommandUsage;
import com.zenith.discord.Embed;

import java.util.List;

import static com.github.mikumiku7.AutoPatrolConfig.PatrolPoint;
import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.zenith.Globals.MODULE;
import static com.zenith.command.brigadier.ToggleArgumentType.getToggle;
import static com.zenith.command.brigadier.ToggleArgumentType.toggle;

public class AutoPatrolCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.builder()
                .name("autoPatrol")
                .category(CommandCategory.MODULE)
                .description("""
                        Automatically patrols between configured waypoints in sequence.
                        Bot will move to each patrol point in order, then loop back to the first point.
                        Includes stuck detection and unstuck actions.
                        """)
                .usageLines(
                        "on/off",
                        "add <x> <y> <z>",
                        "remove <index>",
                        "clear",
                        "list",
                        "goto <index>",
                        "stuckDetection <seconds>",
                        "arrivalThreshold <blocks>",
                        "unstuckActions on/off",
                        "jumpUnstuck on/off",
                        "movementUnstuck on/off",
                        "unstuckDuration <ticks>"
                )
                .build();
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("autoPatrol")
                .then(argument("toggle", toggle()).executes(c -> {
                    AutoPatrolPlugin.PLUGIN_CONFIG.enabled = getToggle(c, "toggle");
                    MODULE.get(AutoPatrol.class).syncEnabledFromConfig();
                    c.getSource().getEmbed()
                            .title("AutoPatrol " + toggleStrCaps(AutoPatrolPlugin.PLUGIN_CONFIG.enabled));
                    return OK;
                }))
                .then(literal("add")
                        .then(argument("x", integer()).then(argument("y", integer()).then(argument("z", integer()).executes(c -> {
                            int x = getInteger(c, "x");
                            int y = getInteger(c, "y");
                            int z = getInteger(c, "z");

                            MODULE.get(AutoPatrol.class).addPatrolPoint(x, y, z);
                            c.getSource().getEmbed()
                                    .title("Patrol Point Added")
                                    .description("Added patrol point: (" + x + ", " + y + ", " + z + ")");
                            return OK;
                        })))))
                .then(literal("remove").then(argument("index", integer()).executes(c -> {
                    int index = getInteger(c, "index") - 1; // Convert to 0-based index
                    List<PatrolPoint> points = AutoPatrolPlugin.PLUGIN_CONFIG.patrolPoints;

                    if (index >= 0 && index < points.size()) {
                        PatrolPoint removed = points.remove(index);
                        c.getSource().getEmbed()
                                .title("Patrol Point Removed")
                                .description("Removed patrol point:  (" + removed.x + ", " + removed.y + ", " + removed.z + ")");
                        return OK;
                    } else {
                        c.getSource().getEmbed()
                                .title("Invalid Index")
                                .description("Patrol point index " + (index + 1) + " does not exist. Use 'list' to see available points.");
                        return ERROR;
                    }
                })))
                .then(literal("clear").executes(c -> {
                    MODULE.get(AutoPatrol.class).clearPatrolPoints();
                    c.getSource().getEmbed()
                            .title("Patrol Points Cleared")
                            .description("All patrol points have been removed.");
                    return OK;
                }))
                .then(literal("list").executes(c -> {
                    List<PatrolPoint> points = AutoPatrolPlugin.PLUGIN_CONFIG.patrolPoints;
                    AutoPatrol module = MODULE.get(AutoPatrol.class);
                    int currentIndex = module.getCurrentPatrolIndex();

                    if (points.isEmpty()) {
                        c.getSource().getEmbed()
                                .title("No Patrol Points")
                                .description("No patrol points configured. Use 'add' to add points.");
                    } else {
                        StringBuilder sb = new StringBuilder();
                        for (int i = 0; i < points.size(); i++) {
                            PatrolPoint point = points.get(i);
                            String marker = (i == currentIndex) ? "➤ " : "  ";
                            sb.append(marker).append(i + 1).append(". ")
                                    .append(" (").append(point.x).append(", ").append(point.y).append(", ").append(point.z).append(")\n");
                        }

                        c.getSource().getEmbed()
                                .title("Patrol Points")
                                .description(sb.toString());
                    }
                    return OK;
                }))
                .then(literal("goto").then(argument("index", integer()).executes(c -> {
                    int index = getInteger(c, "index") - 1; // Convert to 0-based index
                    MODULE.get(AutoPatrol.class).setCurrentPatrolIndex(index);
                    c.getSource().getEmbed()
                            .title("Patrol Index Set")
                            .description("Current patrol index set to " + (index + 1));
                    return OK;
                })))
                .then(literal("stuckDetection").then(argument("seconds", integer(1, 60)).executes(c -> {
                    int seconds = getInteger(c, "seconds");
                    AutoPatrolPlugin.PLUGIN_CONFIG.stuckDetectionSeconds = seconds;
                    c.getSource().getEmbed()
                            .title("Stuck Detection Updated")
                            .description("Stuck detection set to " + seconds + " seconds");
                    return OK;
                })))
                .then(literal("arrivalThreshold").then(argument("blocks", integer(1, 10)).executes(c -> {
                    int blocks = getInteger(c, "blocks");
                    AutoPatrolPlugin.PLUGIN_CONFIG.arrivalThreshold = blocks;
                    c.getSource().getEmbed()
                            .title("Arrival Threshold Updated")
                            .description("Arrival threshold set to " + blocks + " blocks");
                    return OK;
                })))
                .then(literal("unstuckActions").then(argument("toggle", toggle()).executes(c -> {
                    AutoPatrolPlugin.PLUGIN_CONFIG.enableUnstuckActions = getToggle(c, "toggle");
                    c.getSource().getEmbed()
                            .title("Unstuck Actions " + toggleStrCaps(AutoPatrolPlugin.PLUGIN_CONFIG.enableUnstuckActions));
                    return OK;
                })))
                .then(literal("jumpUnstuck").then(argument("toggle", toggle()).executes(c -> {
                    AutoPatrolPlugin.PLUGIN_CONFIG.enableJumpUnstuck = getToggle(c, "toggle");
                    c.getSource().getEmbed()
                            .title("Jump Unstuck " + toggleStrCaps(AutoPatrolPlugin.PLUGIN_CONFIG.enableJumpUnstuck));
                    return OK;
                })))
                .then(literal("movementUnstuck").then(argument("toggle", toggle()).executes(c -> {
                    AutoPatrolPlugin.PLUGIN_CONFIG.enableMovementUnstuck = getToggle(c, "toggle");
                    c.getSource().getEmbed()
                            .title("Movement Unstuck " + toggleStrCaps(AutoPatrolPlugin.PLUGIN_CONFIG.enableMovementUnstuck));
                    return OK;
                })))
                .then(literal("unstuckDuration").then(argument("ticks", integer(10, 200)).executes(c -> {
                    int ticks = getInteger(c, "ticks");
                    AutoPatrolPlugin.PLUGIN_CONFIG.unstuckActionDurationTicks = ticks;
                    c.getSource().getEmbed()
                            .title("Unstuck Duration Updated")
                            .description("Unstuck action duration set to " + ticks + " ticks");
                    return OK;
                })));
    }

    @Override
    public void defaultEmbed(Embed embed) {
        embed.primaryColor();
        if (!embed.isDescriptionPresent()) {
            AutoPatrol module = MODULE.get(AutoPatrol.class);
            List<PatrolPoint> points = AutoPatrolPlugin.PLUGIN_CONFIG.patrolPoints;

            embed
                    .addField("AutoPatrol", toggleStr(AutoPatrolPlugin.PLUGIN_CONFIG.enabled), false)
                    .addField("Patrol Points", String.valueOf(points.size()), false)
                    .addField("Current Index", module.getCurrentPatrolIndex() + 1 + "/" + points.size(), false)
                    .addField("Stuck Detection", AutoPatrolPlugin.PLUGIN_CONFIG.stuckDetectionSeconds + "s", false)
                    .addField("Arrival Threshold", AutoPatrolPlugin.PLUGIN_CONFIG.arrivalThreshold + " blocks", false)
                    .addField("Unstuck Actions", toggleStr(AutoPatrolPlugin.PLUGIN_CONFIG.enableUnstuckActions), false)
                    .addField("Jump Unstuck", toggleStr(AutoPatrolPlugin.PLUGIN_CONFIG.enableJumpUnstuck), false)
                    .addField("Movement Unstuck", toggleStr(AutoPatrolPlugin.PLUGIN_CONFIG.enableMovementUnstuck), false)
                    .addField("Unstuck Duration", AutoPatrolPlugin.PLUGIN_CONFIG.unstuckActionDurationTicks + " ticks", false);
        }
    }
}
