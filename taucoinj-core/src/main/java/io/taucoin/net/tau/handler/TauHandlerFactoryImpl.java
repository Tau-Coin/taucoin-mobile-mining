package io.taucoin.net.tau.handler;

import io.taucoin.net.tau.TauVersion;
import javax.inject.Inject;
//import org.springframework.context.ApplicationContext;


/**
 * Default factory implementation
 *
 * @author Mikhail Kalinin
 * @since 20.08.2015
 */
public class TauHandlerFactoryImpl implements TauHandlerFactory {

    //@Inject
    //private ApplicationContext ctx;

    @Override
    public TauHandler create(TauVersion version) {
        switch (version) {
            //case V60:   return ctx.getBean(Tau60.class);
            //case V61:   return ctx.getBean(Tau61.class);
            //case V62:   return ctx.getBean(Tau62.class);
            default:    throw new IllegalArgumentException("Eth " + version + " is not supported");
        }
    }
}
