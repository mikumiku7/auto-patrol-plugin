# AutoPatrol Module for ZenithProxy

## Overview

**AutoPatrol** is a module for ZenithProxy that enables your Minecraft bot to automatically patrol a series of configured waypoints in sequence. The bot will move to each patrol point in order, looping back to the first point after reaching the last, and will continuously patrol the route. The module includes robust stuck detection and automatic recovery actions to ensure smooth, uninterrupted patrols.

## Features

- **Configurable Patrol Path:** Define a list of patrol points (waypoints) with coordinates and names. The bot will visit each point in order and repeat the cycle.
- **Stuck Detection:** If the bot does not move for a configurable number of seconds, it is considered stuck.
- **Unstuck Actions:** When stuck, the bot will attempt to jump and/or move sideways to free itself, then resume patrol.
- **Arrival Threshold:** The bot considers a patrol point reached when within a configurable distance (in blocks).
- **Full Command Control:** Add, remove, list, and clear patrol points, and adjust all settings via commands.
- **Baritone Integration:** Uses Baritone pathfinder for all movement.

## Configuration

Configuration is available in `config.json` under:

```json
{
        "enabled": false,
        "patrolPoints": [],
        "stuckDetectionSeconds": 3,
        "arrivalThreshold": 1.5,
        "enableUnstuckActions": true,
        "enableJumpUnstuck": true,
        "enableMovementUnstuck": true,
        "unstuckActionDurationTicks": 20
}
```

- `enabled`: Enable or disable the AutoPatrol module.
- `patrolPoints`: List of patrol points (each with `x`, `y`, `z`, and `name`).
- `stuckDetectionSeconds`: Seconds of no movement before the bot is considered stuck.
- `arrivalThreshold`: Distance (in blocks) to consider a patrol point reached.
- `enableUnstuckActions`: Whether to attempt to recover when stuck.
- `enableJumpUnstuck`: Whether to jump when stuck.
- `enableMovementUnstuck`: Whether to move sideways when stuck.
- `unstuckActionDurationTicks`: How long (in ticks) to perform unstuck actions before retrying patrol.

## Commands

All commands are available via the terminal, or Discord (if enabled):

```
autoPatrol on|off
autoPatrol add <x> <y> <z>             # Add a patrol point
autoPatrol remove <index>              # Remove patrol point by index (see list)
autoPatrol clear                       # Remove all patrol points
autoPatrol list                        # List all patrol points
autoPatrol goto <index>                # Set current patrol target by index
autoPatrol stuckDetection <seconds>    # Set stuck detection time
autoPatrol arrivalThreshold <blocks>   # Set arrival threshold (blocks)
autoPatrol unstuckActions on|off       # Enable/disable unstuck actions
autoPatrol jumpUnstuck on|off          # Enable/disable jump on stuck
autoPatrol movementUnstuck on|off      # Enable/disable sideways movement on stuck
autoPatrol unstuckDuration <ticks>     # Set duration of unstuck actions
```

## Example Usage

1. **Enable the module:**
   ```
   autoPatrol on
   ```
2. **Add patrol points:**
   ```
   autoPatrol add 100 64 100
   autoPatrol add 120 64 120
   autoPatrol add 140 64 100
   ```
3. **List patrol points:**
   ```
   autoPatrol list
   ```
4. **Start patrolling:**
   The bot will automatically begin patrolling the configured points in order.

## Notes
- The module requires Baritone pathfinder to be available and functional.
- Patrol points can be managed at runtime; changes take effect immediately.
- If the bot is stuck for longer than the configured time, it will attempt to recover and resume patrol.

## License
This module is part of ZenithProxy and is distributed under the same license. 
