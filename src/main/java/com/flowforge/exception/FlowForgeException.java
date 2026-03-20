package com.flowforge.exception;

/**
 * Base exception for all FlowForge errors.
 *
 * WHY: The naive implementation used LOGGER.info("ERROR: ...")
 * and returned null on failure. A proper exception hierarchy lets callers
 * handle specific failure modes and avoids silent null propagation.
 */
public class FlowForgeException extends RuntimeException {

    public FlowForgeException(String message) {
        super(message);
    }

    public FlowForgeException(String message, Throwable cause) {
        super(message, cause);
    }
}