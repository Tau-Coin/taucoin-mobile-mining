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
import java.util.concurrent.TimeUnit;

//import io.taucoin.core.Block;
import io.taucoin.core.NetworkParameters;
//import io.taucoin.core.StoredBlock;
//import io.taucoin.core.Transaction;
//import io.taucoin.core.Util;
//import io.taucoin.util.MonetaryFormat;
//import io.taucoin.core.VerificationException;
//import io.taucoin.store.BlockStore;
//import io.taucoin.store.BlockStoreException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Stopwatch;

/**
 * Parameters for taucoin-like networks.
 */
public abstract class AbstractTaucoinNetParams extends NetworkParameters {
    /**
     * Scheme part for Taucoin URIs.
     */
    public static final String TAUCOIN_SCHEME = "taucoin";

    private static final Logger log = LoggerFactory.getLogger(AbstractTaucoinNetParams.class);

    public AbstractTaucoinNetParams() {
        super();
    }

    /*
    @Override
    public Coin getMaxMoney() {
        return MAX_MONEY;
    }

    @Override
    public Coin getMinNonDustOutput() {
        return Transaction.MIN_NONDUST_OUTPUT;
    }

    @Override
    public MonetaryFormat getMonetaryFormat() {
        return new MonetaryFormat();
    }

    @Override
    public String getUriScheme() {
        return TAUCOIN_SCHEME;
    }

    @Override
    public boolean hasMaxMoney() {
        return true;
    }
    */
}
