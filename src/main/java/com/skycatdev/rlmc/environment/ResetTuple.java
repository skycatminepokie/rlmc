package com.skycatdev.rlmc.environment;

import java.util.Map;

public record ResetTuple<O>(O observation, Map<String, Object> info) {
}
