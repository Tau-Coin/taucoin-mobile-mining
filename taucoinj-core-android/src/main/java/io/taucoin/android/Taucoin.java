package io.taucoin.android;

import io.taucoin.config.MainNetParams;
import io.taucoin.core.*;
import io.taucoin.crypto.ECKey;
import io.taucoin.crypto.HashUtil;
import io.taucoin.debug.RefWatcher;
import io.taucoin.forge.BlockForger;
import io.taucoin.http.RequestManager;
import io.taucoin.manager.WorldManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.*;

import javax.inject.Inject;
import javax.inject.Singleton;

import static io.taucoin.config.SystemProperties.CONFIG;

@Singleton
public class Taucoin extends io.taucoin.facade.TaucoinImpl {
    private static final Logger log = LoggerFactory.getLogger("tauAndroid");
    @Inject
    public Taucoin(WorldManager worldManager,
                    io.taucoin.manager.BlockLoader blockLoader, PendingState pendingState,
                    BlockForger blockForger, RequestManager requestManager, RefWatcher refWatcher) {

        super(worldManager, blockLoader, pendingState, blockForger,
                requestManager, refWatcher);
    }

    public void init(List<String> privateKeys) {
        List<ECKey> ecprivateKey = new ArrayList<>();
        for (String prikey : privateKeys) {
            ECKey key;
            //String prikey = privateKeys.get(0);

            if (prikey.length() == 51 || prikey.length() == 52) {
                DumpedPrivateKey dumpedPrivateKey = DumpedPrivateKey.fromBase58(MainNetParams.get(), prikey);
                key = dumpedPrivateKey.getKey();
            } else {
                BigInteger privKey = new BigInteger(prikey, 16);
                key = ECKey.fromPrivate(privKey);
                log.info("taucoin init prikey wif:{}", key.getPrivateKeyAsWiF(MainNetParams.get()));
            }
            ecprivateKey.add(key);
        }

        // By default, import first privkey as block forger private key.
        if (!ecprivateKey.isEmpty()) {
            //CONFIG.importForgerPrikey(Hex.decode(privateKeys.get(0)));
            CONFIG.importForgerPrikey(ecprivateKey.get(0).getPrivKeyBytes());
        }

        worldManager.init();
        init();
    }

    public void initSync() {
        worldManager.initSync();
    }

}
