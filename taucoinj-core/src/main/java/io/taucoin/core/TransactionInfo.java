package io.taucoin.core;

import io.taucoin.util.RLP;
import io.taucoin.util.RLPList;
import io.taucoin.util.ByteUtil;

import org.spongycastle.util.encoders.Hex;

import java.io.Serializable;
import java.math.BigInteger;

import static io.taucoin.crypto.HashUtil.*;

public class TransactionInfo implements Serializable {

    private byte[] trHash;
    private long trTime;
    private byte[] rlpEncoded;
    private boolean parsed;
        
    public TransactionInfo(long trTime,byte[] trHash) {
         this.trHash = trHash;
         this.trTime = trTime;
         parsed = true;
    }
    public TransactionInfo(byte[] rlpEncodedData) {
        this.rlpEncoded = rlpEncodedData;
        parsed = false;
    }

    public byte[] gettrHashcode() {
        return trHash;
    }
         
    public long gettrTime() {
         return trTime;
    }
         
    public void settrHashcode(byte[] trHashcode){
        this.trHash = trHashcode;
    }

    public void settrTime(long trTime){
         this.trTime = trTime;
    }
     public void rlpParse() { 
         RLPList decodedTxList = RLP.decode2(rlpEncoded);
         RLPList transaction = (RLPList) decodedTxList.get(0);

         byte[] trHashcode = transaction.get(0).getRLPData();
         byte[] trTime = transaction.get(1).getRLPData();
         this.trHash = trHashcode;
         this.trTime = ByteUtil.byteArrayToLong(trTime);
         this.parsed = true;
     }

    public byte[] getEncoded() {
         if (!parsed) rlpParse();
         if (rlpEncoded != null) return rlpEncoded;

         byte[] trHashcode = RLP.encodeElement(this.trHash);
         byte[] trTime = RLP.encodeBigInteger(BigInteger.valueOf(this.trTime));
 
         this.rlpEncoded = RLP.encodeList(trHashcode, trTime);
 
         return rlpEncoded;
     }

}
