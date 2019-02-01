package io.taucoin.facade;

import javax.annotation.Nullable;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

public interface Repository extends io.taucoin.core.Repository {

    /**
     * @param addr - account to check
     * @return - true if account exist,
     *           false otherwise
     */
    boolean isExist(byte[] addr);


    /**
     * Retrieve balance of an account
     *
     * @param addr of the account
     * @return balance of the account as a <code>BigInteger</code> value
     */
    BigInteger getBalance(byte[] addr);


    /**
     * Get current forgePower of a given account
     *
     * @param addr of the account
     * @return value of the forgePower
     */
    BigInteger getforgePower(byte[] addr);

}
