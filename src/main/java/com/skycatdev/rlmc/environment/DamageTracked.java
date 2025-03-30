/* Licensed MIT 2025 */
package com.skycatdev.rlmc.environment;

public interface DamageTracked {
    default void rlmc$trackIfTracking(float damage) {
        if (rlmc$isTrackingDamage()) {
            rlmc$setDamageTaken(rlmc$getDamageTaken() + damage);
        }
    }

    boolean rlmc$isTrackingDamage();

    float rlmc$getDamageTaken();

    default void rlmc$resetDamageTaken() {
        rlmc$setDamageTaken(0);
    }

    void rlmc$setDamageTaken(float damage);

    void rlmc$setDamageTracking(boolean tracking);

    default void rlmc$startTrackingDamage() {
        rlmc$setDamageTracking(true);
    }

    default void rlmc$stopTracking() {
        rlmc$setDamageTracking(false);
    }
}
