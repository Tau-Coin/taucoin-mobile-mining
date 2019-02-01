package io.taucoin.net.client;

import io.taucoin.config.SystemProperties;
import io.taucoin.net.tau.TauVersion;
import javax.inject.Inject;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import static io.taucoin.net.tau.TauVersion.fromCode;
import static io.taucoin.net.client.Capability.*;

/**
 * Created by Anton Nashatyrev on 13.10.2015.
 */
public class ConfigCapabilities {
    @Inject
    SystemProperties config;

    private SortedSet<Capability> AllCaps = new TreeSet<>();

    @PostConstruct
    private void init() {
        if (config.syncVersion() != null) {
            TauVersion eth = fromCode(config.syncVersion());
            if (eth != null) AllCaps.add(new Capability(TAU, eth.getCode()));
        } else {
            for (TauVersion v : TauVersion.supported())
                AllCaps.add(new Capability(TAU, v.getCode()));
        }
    }

    /**
     * Gets the capabilities listed in 'peer.capabilities' config property
     * sorted by their names.
     */
    public List<Capability> getConfigCapabilities() {
        List<Capability> ret = new ArrayList<>();
        List<String> caps = config.peerCapabilities();
        for (Capability capability : AllCaps) {
            if (caps.contains(capability.getName())) {
                ret.add(capability);
            }
        }
        return ret;
    }

}
