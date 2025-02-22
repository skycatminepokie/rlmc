/* Licensed MIT 2025 */
package com.skycatdev.rlmc.environment;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public record BlockHitInfo(Direction side, BlockPos blockPos, BlockState blockState) {
}
