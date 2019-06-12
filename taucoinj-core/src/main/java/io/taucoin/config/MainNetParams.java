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

import java.math.BigInteger;

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

        id = ID_MAINNET;
        subsidyDecreaseBlockCount = 210000;
        spendableCoinbaseDepth = 6;
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
