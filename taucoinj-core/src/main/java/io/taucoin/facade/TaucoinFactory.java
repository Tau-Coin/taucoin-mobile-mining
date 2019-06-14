package io.taucoin.facade;

import io.taucoin.config.SystemProperties;
import io.taucoin.net.tau.TauVersion;
import io.taucoin.util.BuildInfo;
import io.taucoin.util.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;

/**
 * @author Roman Mandeleil
 * @since 13.11.2014
 */
@Singleton
public class TaucoinFactory {

    private static final Logger logger = LoggerFactory.getLogger("general");
    //public static ApplicationContext context = null;

    public static Taucoin createTaucoin() {
        return createTaucoin((Class) null);
    }

    public static Taucoin createTaucoin(Class userSpringConfig) {
        //return new Taucoin();//createTaucoin(SystemProperties.CONFIG, userSpringConfig);
        return null;
    }

    public static Taucoin createTaucoin(SystemProperties config, Class userSpringConfig) {

        logger.info("Running {},  core version: {}-{}", config.genesisInfo(), config.projectVersion(), config.projectVersionModifier());
        BuildInfo.printInfo();

        if (config.databaseReset()){
            FileUtil.recursiveDelete(config.databaseDir());
            logger.info("Database reset done");
        }

        //return userSpringConfig == null ? createTaucoin(new Class[] {DefaultConfig.class}) :
        //        createTaucoin(DefaultConfig.class, userSpringConfig);
        return null;
    }

    public static Taucoin createTaucoin(Class ... springConfigs) {

        if (logger.isInfoEnabled()) {
            StringBuilder versions = new StringBuilder();
            for (TauVersion v : TauVersion.supported()) {
                versions.append(v.getCode()).append(", ");
            }
            versions.delete(versions.length() - 2, versions.length());
            logger.info("capability eth version: [{}]", versions);
        }

       // context = new AnnotationConfigApplicationContext(springConfigs);
       // return new Taucoin();// context.getBean(Taucoin.class);
       return null;
    }
}
