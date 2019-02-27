package io.taucoin.trie;

import io.taucoin.util.Value;

/**
 * @author Roman Mandeleil
 * @since 29.08.2014
 */
public class CountAllNodes implements TrieImpl.ScanAction {

    int counted = 0;

    @Override
    public void doOnNode(byte[] hash, Value node) {
        ++counted;
    }

    public int getCounted() {
        return counted;
    }
}
