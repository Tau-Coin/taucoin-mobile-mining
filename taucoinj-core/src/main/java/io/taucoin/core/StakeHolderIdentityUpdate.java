package io.taucoin.core;

import io.taucoin.crypto.ECKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class StakeHolderIdentityUpdate {
    private static final Logger logger = LoggerFactory.getLogger("stakeholder");
    private Repository track;
    private Transaction tx;
    private byte[] forgeAddress;

    public StakeHolderIdentityUpdate(Transaction tx, Repository track,byte[] forgeAddress){
       this.tx = tx;
       this.track = track;
       this.forgeAddress = forgeAddress;
    }

    public AssociatedAccount updateStakeHolderIdentity(){
        byte[] senderAddress = tx.getSender();
        byte[] receiveAddress = tx.getReceiveAddress();

        AccountState senderAccount = track.getAccountState(senderAddress);
        AccountState receiveAccount = track.getAccountState(receiveAddress);

        senderAccount.setAssociatedAddress(receiveAddress);
        senderAccount.setWitnessAddress(this.forgeAddress);
        receiveAccount.setAssociatedAddress(senderAddress);
        receiveAccount.setWitnessAddress(this.forgeAddress);
        return new AssociatedAccount(senderAddress,senderAccount,receiveAddress,receiveAccount);
    }
}
