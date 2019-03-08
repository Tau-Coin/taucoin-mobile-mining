package io.taucoin.forge;

import io.taucoin.core.Block;

/**
 * Created by Anton Nashatyrev on 10.12.2015.
 */
public interface ForgerListener {
    void forgingStarted();
    void forgingStopped();
    void blockForgingStarted(Block block);
    void nextBlockForgedInternal(long internal);
    void blockForged(Block block);
    void blockForgingCanceled(Block block);
}
