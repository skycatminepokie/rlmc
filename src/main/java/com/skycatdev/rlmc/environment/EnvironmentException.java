/* Licensed MIT 2025 */
package com.skycatdev.rlmc.environment;

public class EnvironmentException extends RuntimeException {
    public EnvironmentException(String message) {
        super(message);
    }

    public EnvironmentException(String message, Throwable cause) {
        super(message, cause);
    }

    public EnvironmentException(Throwable cause) {
        super(cause);
    }

    public EnvironmentException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public EnvironmentException() {
        super();
    }
}
