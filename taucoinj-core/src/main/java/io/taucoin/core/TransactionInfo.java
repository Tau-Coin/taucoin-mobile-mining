package io.taucoin.core;

import io.taucoin.util.RLP;
import io.taucoin.util.RLPList;
import io.taucoin.util.ByteUtil;

import org.spongycastle.util.encoders.Hex;

import java.io.Serializable;
import java.math.BigInteger;

import static io.taucoin.crypto.HashUtil.*;

public class TransactionInfo implements Serializable {

    private long trTime;
    private byte[] trHash;
    private byte[] rlpEncoded;
    private boolean parsed;

    //memory economical because of some simple devices.
    public TransactionInfo(long trTime,byte[] trHash) {
         this.trTime = trTime;
         //this.trHash = trHash;
         parsed = true;

    }
    public TransactionInfo(byte[] rlpEncodedData) {
        this.rlpEncoded = rlpEncodedData;
        parsed = false;
    }

    @Deprecated
    public byte[] gettrHashcode() {
        if(!parsed) rlpParse();
        if (trHash == null) {
            trHash = ByteUtil.longToBytes(0);
        }
        return trHash;
    }
         
    public long gettrTime() {
        if(!parsed) rlpParse();
        return trTime;
    }
         
    public void settrHashcode(byte[] trHashcode){
        this.trHash = trHashcode;
    }

    public void settrTime(long trTime){
         this.trTime = trTime;
    }

    public void rlpParse() {
        if(rlpEncoded != null) {
            RLPList decodedTxList = RLP.decode2(rlpEncoded);
            RLPList transaction = (RLPList) decodedTxList.get(0);
            byte[] trTime = transaction.get(0).getRLPData();
            //considering concurrency situation ,a list may be need.
            if (transaction.size() > 1) {
                byte[] trHashcode = transaction.get(1).getRLPData();
                this.trHash = trHashcode;
            }
            this.trTime = ByteUtil.byteArrayToLong(trTime);
            this.parsed = true;
        }
        rlpEncoded = null;
     }

    public byte[] getEncoded() {
         if (!parsed) rlpParse();
         if (rlpEncoded != null) return rlpEncoded;

         byte[] trTime = RLP.encodeBigInteger(BigInteger.valueOf(this.trTime));
         //byte[] trHashcode = RLP.encodeElement(this.trHash);

 
         this.rlpEncoded = RLP.encodeList(trTime/*trHashcode*/);
 
         return rlpEncoded;
     }

}
