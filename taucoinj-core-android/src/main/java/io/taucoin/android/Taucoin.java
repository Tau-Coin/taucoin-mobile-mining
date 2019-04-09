package io.taucoin.android;


import io.taucoin.android.manager.BlockLoader;
import io.taucoin.config.MainNetParams;
import io.taucoin.core.*;
import io.taucoin.crypto.ECKey;
import io.taucoin.crypto.HashUtil;
import io.taucoin.forge.BlockForger;
import io.taucoin.http.RequestManager;
import io.taucoin.manager.AdminInfo;
import io.taucoin.manager.WorldManager;
import io.taucoin.net.client.PeerClient;
import io.taucoin.net.rlpx.discover.UDPListener;
import io.taucoin.net.server.ChannelManager;
import io.taucoin.net.server.PeerServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.util.*;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import static io.taucoin.config.SystemProperties.CONFIG;

@Singleton
public class Taucoin extends io.taucoin.facade.TaucoinImpl {
    private static final Logger log = LoggerFactory.getLogger("tauAndroid");
    @Inject
    public Taucoin(WorldManager worldManager, AdminInfo adminInfo,
                    io.taucoin.manager.BlockLoader blockLoader, PendingState pendingState,
                    BlockForger blockForger, RequestManager requestManager) {

        super(worldManager, adminInfo, blockLoader, pendingState, blockForger,
                requestManager);
    }

    public void init(List<String> privateKeys) {
        List<ECKey> ecprivateKey = new ArrayList<>();
        for (String prikey : privateKeys) {
            ECKey key;
            //String prikey = privateKeys.get(0);
            log.info("privkey is {}", prikey);

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

        for (ECKey key: ecprivateKey) {
           //worldManager.getWallet().importKey(Hex.decode(privateKey));
           worldManager.getWallet().importKey(key.getPrivKeyBytes());
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

    public byte[] createRandomAccount() {

        byte[] randomPrivateKey = HashUtil.sha3(HashUtil.randomPeerId());
        worldManager.getWallet().importKey(randomPrivateKey);
        return randomPrivateKey;
    }
}
