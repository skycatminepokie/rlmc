/* Licensed MIT 2025 */
package com.skycatdev.rlmc.command;

public class EnvironmentSettings {
    protected boolean useMonitor = false;
    protected int timeLimit = 0;
    protected int frameStack = 0;

    public EnvironmentSettings(boolean useMonitor, int timeLimit, int frameStack) {
        this.useMonitor = useMonitor;
        this.timeLimit = timeLimit;
        this.frameStack = frameStack;
    }

    @SuppressWarnings("unused") // Used by entrypoint.py
    public boolean shouldUseMonitor() {
        return useMonitor;
    }

    @SuppressWarnings("unused") // Used by entrypoint.py
    public int getTimeLimit() {
        return timeLimit;
    }

    @SuppressWarnings("unused") // Used by entrypoint.py
    public int getFrameStack() {
        return frameStack;
    }
}
