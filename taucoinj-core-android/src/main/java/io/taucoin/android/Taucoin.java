package io.taucoin.android;


import io.taucoin.android.manager.BlockLoader;
import io.taucoin.core.Block;
import io.taucoin.core.Genesis;
import io.taucoin.core.PendingState;
import io.taucoin.core.Transaction;
import io.taucoin.core.Wallet;
import io.taucoin.crypto.HashUtil;
import io.taucoin.forge.BlockForger;
import io.taucoin.manager.AdminInfo;
import io.taucoin.manager.WorldManager;
import io.taucoin.net.client.PeerClient;
import io.taucoin.net.rlpx.discover.UDPListener;
import io.taucoin.net.server.ChannelManager;
import io.taucoin.net.server.PeerServer;
import org.spongycastle.util.encoders.Hex;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import static io.taucoin.config.SystemProperties.CONFIG;

@Singleton
public class Taucoin extends io.taucoin.facade.TaucoinImpl {

    @Inject
    public Taucoin(WorldManager worldManager, AdminInfo adminInfo,
                    ChannelManager channelManager, io.taucoin.manager.BlockLoader blockLoader, PendingState pendingState,
                    Provider<PeerClient> peerClientProvider, Provider<UDPListener> discoveryServerProvider,
                    PeerServer peerServer, BlockForger blockForger) {

        super(worldManager, adminInfo, channelManager, blockLoader, pendingState, peerClientProvider,
                discoveryServerProvider, peerServer, blockForger);
    }

    public void init(List<String> privateKeys) {

        for (String privateKey: privateKeys) {
            worldManager.getWallet().importKey(Hex.decode(privateKey));
        }

        // By default, import first privkey as block forger private key.
        if (!privateKeys.isEmpty()) {
            CONFIG.importForgerPrikey(Hex.decode(privateKeys.get(0)));
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
