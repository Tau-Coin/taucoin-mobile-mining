package io.taucoin.net.submit;

import io.taucoin.core.BlockHeader;
import io.taucoin.net.server.Channel;
import io.taucoin.net.server.ChannelManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

import static java.lang.Thread.sleep;

/**
 * @author Taucoin Core Developers
 * @since 01.28.2015
 */
public class NewBlockHeaderTask implements Callable<BlockHeader> {

    private static final Logger logger = LoggerFactory.getLogger("net");

    private final BlockHeader header;
    private final ChannelManager channelManager;
    private final Channel receivedFrom;

    public NewBlockHeaderTask(BlockHeader header, ChannelManager channelManager, Channel receivedFrom) {
        this.header = header;
        this.channelManager = channelManager;
        this.receivedFrom = receivedFrom;
    }

    public NewBlockHeaderTask(BlockHeader header, ChannelManager channelManager) {
        this(header, channelManager, null);
    }

    @Override
    public BlockHeader call() throws Exception {

        try {
            logger.info("submit new block header: {}", header.toString());
            channelManager.sendNewBlockHeader(header, receivedFrom);
            return header;

        } catch (Throwable th) {
            logger.warn("Exception caught: {}", th);
        }
        return null;
    }
}
