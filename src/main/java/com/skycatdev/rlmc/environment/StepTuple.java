/* Licensed MIT 2025 */
package com.skycatdev.rlmc.environment;

import java.util.Map;

public record StepTuple<O>(O observation, double reward, boolean terminated, boolean truncated, Map<String, Object> info) {
}
