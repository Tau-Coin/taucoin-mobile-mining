/*
 * Copyright 2013 Google Inc.
 * Copyright 2015 Andreas Schildbach
 * Copyright 2018 taucoin

 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.taucoin.config;

import io.taucoin.core.*;
//import org.ethereum.net.peerdiscovery.*;

import java.math.BigInteger;
import java.net.*;

import static com.google.common.base.Preconditions.*;

/**
 * Parameters for the main production network on which people trade goods and services.
 */
public class MainNetParams extends AbstractTaucoinNetParams {
    public static final int MAINNET_MAJORITY_WINDOW = 1000;
    public static final int MAINNET_MAJORITY_REJECT_BLOCK_OUTDATED = 950;
    public static final int MAINNET_MAJORITY_ENFORCE_BLOCK_UPGRADE = 750;

    public MainNetParams() {
        super();
        //305166438 equal to 0xffffffffffffffff
        maxTarget = new BigInteger("ffffffffffffffff",16);
        dumpedPrivateKeyHeader = 128;
        addressHeader = 65;
        port = 8686;//8333
        packetMagic = 0x696d636dL;//0xf9beb4d9L

        majorityEnforceBlockUpgrade = MAINNET_MAJORITY_ENFORCE_BLOCK_UPGRADE;
        majorityRejectBlockOutdated = MAINNET_MAJORITY_REJECT_BLOCK_OUTDATED;
        majorityWindow = MAINNET_MAJORITY_WINDOW;

        //genesisBlock.setDifficultyTarget(new BigInteger("369D0369D036978",16));
        //time is utc 2018-11-16
        //genesisBlock.setTime(1542336793L);
        id = ID_MAINNET;
        subsidyDecreaseBlockCount = 210000;
        spendableCoinbaseDepth = 6;
        //String genesisHash = genesisBlock.getHashAsString();
        //reason is same test
        //checkState(genesisHash.equals("000000000019d6689c085ae165831e934ff763ae46a2a6c172b3f1b60a8ce26f"),
         //       genesisHash);

        // This contains (at a minimum) the blocks which are not BIP30 compliant. BIP30 changed how duplicate
        // transactions are handled. Duplicated transactions could occur in the case where a coinbase had the same
        // extraNonce and the same outputs but appeared at different heights, and greatly complicated re-org handling.
        // Having these here simplifies block connection logic considerably.
//        checkpoints.put(91722, Sha256Hash.wrap());
//        checkpoints.put(91812, Sha256Hash.wrap());
//        checkpoints.put(91842, Sha256Hash.wrap());
//        checkpoints.put(91880, Sha256Hash.wrap());
//        checkpoints.put(200000, Sha256Hash.wrap("hashcode"));
    }

    private static MainNetParams instance;
    public static synchronized MainNetParams get() {
        if (instance == null) {
            instance = new MainNetParams();
        }
        return instance;
    }

    @Override
    public String getPaymentProtocolId() {
        return PAYMENT_PROTOCOL_ID_MAINNET;
    }
}
