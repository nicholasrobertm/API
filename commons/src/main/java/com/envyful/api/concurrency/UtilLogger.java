package com.envyful.api.concurrency;

import org.apache.logging.log4j.Logger;

import java.util.Optional;

/**
 *
 * Static utility class used by mods, and plugins, to prove a logging
 * interface for the generic API classes found in this repository.
 * <br>
 * Best practice is to use the {@link UtilLogger#logger()} method
 * as it is not guaranteed that every mod will provide a logger to the API
 * and not using that could result in a {@link NullPointerException}
 * in the code you're attempting to run
 *
 */
public class UtilLogger {

    private static Logger logger;

    public static void setLogger(Logger logger) {
        UtilLogger.logger = logger;
    }

    /**
     *
     * Gets the instance of the provided logger
     *
     * @return The logger
     * @deprecated Use {@link UtilLogger#logger()} to safely get the logger instance
     */
    @Deprecated
    public static Logger getLogger() {
        return logger;
    }

    public static Optional<Logger> logger() {
        return Optional.ofNullable(logger);
    }
}
