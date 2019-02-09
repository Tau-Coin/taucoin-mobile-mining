package io.taucoin.net.tau.handler;

import io.taucoin.net.tau.TauVersion;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

/**
 * Default factory implementation
 *
 * @author Mikhail Kalinin
 * @since 20.08.2015
 */
@Singleton
public class TauHandlerFactoryImpl implements TauHandlerFactory {

    Provider<Tau60> tau60Provider;
    Provider<Tau61> tau61Provider;
    Provider<Tau62> tau62Provider;

    @Inject
    public TauHandlerFactoryImpl(Provider<Tau60> tau60Provider, Provider<Tau61> tau61Provider, Provider<Tau62> tau62Provider) {
        this.tau60Provider = tau60Provider;
        this.tau61Provider = tau61Provider;
        this.tau62Provider = tau62Provider;
    }

    @Override
    public TauHandler create(TauVersion version) {
        switch (version) {
            case V60:   return tau60Provider.get();
            case V61:   return tau61Provider.get();
            case V62:   return tau62Provider.get();
            default:    throw new IllegalArgumentException("Eth " + version + " is not supported");
        }
    }
}
