package io.taucoin.http.message;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract message class for all messages on the Taucoin network
 *
 * @author Taucoin Core Developers
 * @since 29.03.19
 */
public abstract class Message {

    protected static final Logger logger = LoggerFactory.getLogger("http");

    public Message() {
    }

    public abstract Class<?> getAnswerMessage();

    /**
     * Returns the message in Json String format
     *
     * @return A json string with all attributes of the message
     */
    public abstract String toJsonString();

    /**
     * Returns the message in String format
     *
     * @return A string with all attributes of the message
     */
    public abstract String toString();
}
