package io.taucoin.core;

import org.ethereum.util.RLP;
import org.ethereum.util.RLPList;
import org.ethereum.util.ByteUtil;

import org.spongycastle.util.encoders.Hex;

import java.io.Serializable;
import java.math.BigInteger;

import static org.ethereum.crypto.HashUtil.*;

public class TransactionInfo implements Serializable {
   
    private int trHashcode;
    private long trTime;
    private byte[] rlpEncoded;
    private boolean parsed;
        
    public TransactionInfo(int trHashcode,long trTime) {
         this.trHashcode = trHashcode; 
         this.trTime = trTime;
         parsed = true;
    }
    public TransactionInfo(byte[] rlpEncodedData) {
         rlpParse();
    }

    public int gettrHashcode() {
         return trHashcode;
    }
         
    public long gettrTime() {
         return trTime;
    }
         
    public void settrHashcode(int trHashcode){
         this.trHashcode = trHashcode;
    }

    public void settrTime(long trTime){
         this.trTime = trTime;
    }
     public void rlpParse() { 
         RLPList decodedTxList = RLP.decode2(rlpEncoded);
         RLPList transaction = (RLPList) decodedTxList.get(0);

         byte[] trHashcode = transaction.get(0).getRLPData();
         byte[] trTime = transaction.get(1).getRLPData();
         this.trHashcode = ByteUtil.byteArrayToInt(trHashcode);
         this.trTime = ByteUtil.byteArrayToLong(trTime);
         this.parsed = true;
     }

    public byte[] getEncoded() {
         if (!parsed) rlpParse();
         if (rlpEncoded != null) return rlpEncoded;

         byte[] trHashcode = RLP.encodeInt(this.trHashcode);
         byte[] trTime = RLP.encodeBigInteger(BigInteger.valueOf(this.trTime));
 
         this.rlpEncoded = RLP.encodeList(trHashcode, trTime);
 
         return rlpEncoded;
     }

}
