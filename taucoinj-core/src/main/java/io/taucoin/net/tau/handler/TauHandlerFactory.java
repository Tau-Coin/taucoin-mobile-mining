package io.taucoin.net.tau.handler;

import io.taucoin.net.tau.TauVersion;

/**
 * @author Mikhail Kalinin
 * @since 20.08.2015
 */
public interface TauHandlerFactory {

    /**
     * Creates TauHandler by requested Tau version
     *
     * @param version Tau version
     * @return created handler
     *
     * @throws IllegalArgumentException if provided Tau version is not supported
     */
    TauHandler create(TauVersion version);

}
