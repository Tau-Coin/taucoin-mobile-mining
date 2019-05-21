package io.taucoin.core;

import io.taucoin.crypto.ECKey;
import io.taucoin.crypto.ECKey.ECDSASignature;
import io.taucoin.crypto.ECKey.MissingPrivateKeyException;
import io.taucoin.crypto.HashUtil;
import io.taucoin.util.ByteUtil;
import io.taucoin.util.RLP;
import io.taucoin.util.RLPElement;
import io.taucoin.util.RLPList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.spongycastle.util.BigIntegers;
import org.spongycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.Arrays;

import static org.apache.commons.lang3.ArrayUtils.getLength;
import static io.taucoin.util.ByteUtil.*;
import static io.taucoin.util.BIUtil.toBI;
import static io.taucoin.util.TimeUtils.timeNows;

/**
 * A transaction (formally, T) is a single cryptographically
 * signed instruction sent by an actor external to Ethereum.
 * An external actor can be a person (via a mobile device or desktop computer)
 * or could be from a piece of automated software running on a server.
 * There are two types of transactions: those which result in message calls
 * and those which result in the creation of new contracts.
 */
public class Transaction {

    private static final Logger logger = LoggerFactory.getLogger(Transaction.class);
    public String TRANSACTION_STATUS = "" ;
    public static final String TRANSACTION_SUCCESS = "transaction success!";
    public static final String TRANSACTION_INSUFFCIENT ="less sufficient funds,transaction fail";

    /* version is for upgrade to define the transition grace peroid */
    private byte version;

    /* option for future use*/
    private byte option;

    /* used for transaction validation eg expire or deny reentrance */
    private byte[] timeStamp;

    /* used for transaction expire time setting*/
    private byte[] expireTime = shortToBytes((short)TTIME);

    /* the address of the destination account
     * 20 bytes, this is SHA256-ripemd 160 on a public key */
    private byte[] toAddress;

    /* 5 bytes,the amount of taucoin */
    private byte[] amount;

    /* 2 bytes,transaction fee ralated to transaction*/
    private byte[] fee;

    /* 520 bytes,the elliptic curve signature
     * (including public key recovery bits) */
    private ECDSASignature signature = null;
    
    private byte[] sendAddress;
    
    private byte[] hash;

    /**
     *account address that forger who confirm this last state change.
     */
    private byte[] senderWitnessAddress = null;

    private byte[] receiverWitnessAddress = null;

    /**
     *account address that associated to last account state change.
     */
    private ArrayList<byte[]> senderAssociatedAddress = new ArrayList<>();
    private ArrayList<byte[]> receiverAssociatedAddress = new ArrayList<>();

    public static final int TTIME = 144;
    public static final int HASH_LENGTH = 32;
    public static final int ADDRESS_LENGTH = 20;

    /* Tx in encoded form */
    protected byte[] rlpEncoded;
    private byte[] rlpRaw;
    private byte[] rlpEncodedComposite;
    private boolean isCompositeTx = false;

    /* Indicates if this transaction has been parsed
     * from the RLP-encoded data */
    private boolean parsed = false;

    public Transaction() {

    }
    public Transaction(byte[] rawData) {
        this.rlpEncoded = rawData;
        parsed = false;
    }

    public Transaction(byte[] rawData ,boolean isComposite) {
        if (!isComposite) {
            this.rlpEncoded = rawData;
        } else {
            this.rlpEncodedComposite = rawData;
        }
        this.isCompositeTx = isComposite;
        parsed = false;
    }

    /* creation tx
     * [ version, option, timeStamp, toAddress, amount, fee, expireTime, signature(v, r, s) ]
     */
    public Transaction(byte version, byte option, byte[] timeStamp, byte[] toAddress, byte[] amount, byte[] fee,byte[] expireTime) {
        this.version = version;
        this.option = option;
        this.timeStamp = timeStamp;
        this.toAddress = toAddress;
        this.amount = amount;
        this.fee = fee;
        this.expireTime = expireTime;

        if (toAddress == null) {
            //burn some money
            this.toAddress = ByteUtil.EMPTY_BYTE_ARRAY;
        }
        parsed = true;
    }

    /* this expire time is default 144*5*60*/
    public Transaction(byte version, byte option, byte[] timeStamp, byte[] toAddress, byte[] amount, byte[] fee) {
        this.version = version;
        this.option = option;
        this.timeStamp = timeStamp;
        this.toAddress = toAddress;
        this.amount = amount;
        this.fee = fee;
        this.expireTime = shortToBytes((short)TTIME);

        if (toAddress == null) {
            //burn some money
            this.toAddress = ByteUtil.EMPTY_BYTE_ARRAY;
        }

        parsed = true;
    }

    public Transaction(byte version, byte option, byte[] timeStamp, byte[] toAddress, byte[] amount, byte[] fee,byte[] expireTime, byte[] r, byte[] s, byte v) {
        this(version, option, timeStamp, toAddress, amount, fee,expireTime);
        ECDSASignature signature = new ECDSASignature(new BigInteger(r), new BigInteger(s));
        signature.v = v;
        this.signature = signature;
    }

    public Transaction(byte version, byte option, byte[] timeStamp, byte[] toAddress, byte[] amount, byte[] fee, byte[] r, byte[] s, byte v) {
        this(version, option, timeStamp, toAddress, amount, fee);

        ECDSASignature signature = new ECDSASignature(new BigInteger(r), new BigInteger(s));
        signature.v = v;
        this.signature = signature;
    }

    public void setIsCompositeTx(boolean isCompositeTx){
        this.isCompositeTx = isCompositeTx;
    }

    public boolean isCompositeTx(){
        return isCompositeTx;
    }

    public byte getVersion() {
        if (!parsed) rlpParse();
        return this.version;
    }

    public byte getOption() {
        if (!parsed) rlpParse();
        return this.option;
    }

    public byte[] transactionCost(){

        if (!parsed) rlpParse();

        return fee;
    }

    public synchronized boolean verify() {

        rlpParse();

        if(!validate()){
            return false;
        }

        return true;
    }  

    //method to manufacture composite transaction to roll back
    public void setSenderWitnessAddress(byte[] senderWitnessAddress) {
        this.senderWitnessAddress = senderWitnessAddress;
    }

    public void setReceiverWitnessAddress(byte[] receiverWitnessAddress) {
        this.receiverWitnessAddress = receiverWitnessAddress;
    }

    public void setSenderAssociatedAddress(ArrayList<byte[]> senderAssociatedAddress) {
        this.senderAssociatedAddress.clear();
        this.senderAssociatedAddress.addAll(senderAssociatedAddress);
    }

    public void setReceiverAssociatedAddress(ArrayList<byte[]> receiverAssociatedAddress) {
        this.receiverAssociatedAddress.clear();
        this.receiverAssociatedAddress.addAll(receiverAssociatedAddress);
    }

    public byte[] getSenderWitnessAddress() {
        if (!parsed) rlpParse();
        return senderWitnessAddress;
    }

    public byte[] getReceiverWitnessAddress() {
        if (!parsed) rlpParse();
        return receiverWitnessAddress;
    }

    public ArrayList<byte[]> getSenderAssociatedAddress() {
        if (!parsed) rlpParse();
        return senderAssociatedAddress;
    }

    public ArrayList<byte[]> getReceiverAssociatedAddress() {
        if (!parsed) rlpParse();
        return receiverAssociatedAddress;
    }

    //NowTime - TransactionTime < 1440s;
    public synchronized boolean checkTime(Block benchBlock) {
        //get current unix time
        long benchTime = byteArrayToLong(benchBlock.getTimestamp());
        if (benchTime > byteArrayToLong(this.timeStamp)){
            return false;
		}
        return true;
    }  

    public void rlpParse() {
        if (!isCompositeTx) {
            RLPList decodedTxList = RLP.decode2(rlpEncoded);
            RLPList transaction = (RLPList) decodedTxList.get(0);
//            logger.info("pure transaction item size is {}", transaction.size());
            this.version = transaction.get(0).getRLPData() == null ? (byte) 0 : transaction.get(0).getRLPData()[0];
            //logger.info("item version is {}",(int)this.version);
            //logger.info("item option size is {}",transaction.get(1) == null ? 0 : transaction.get(1).getRLPData().length);
            this.option = transaction.get(1).getRLPData() == null ? (byte) 0 : transaction.get(1).getRLPData()[0];
            this.timeStamp = transaction.get(2).getRLPData();
            //logger.info("item timestamp is {}",ByteUtil.byteArrayToLong(this.timeStamp));
            this.toAddress = transaction.get(3).getRLPData();
            this.amount = transaction.get(4).getRLPData();
            this.fee = transaction.get(5).getRLPData();
            this.expireTime = transaction.get(6).getRLPData();

            // only parse signature in case tx is signed
            if (transaction.get(7).getRLPData() != null) {
                byte v = transaction.get(7).getRLPData()[0];
                byte[] r = transaction.get(8).getRLPData();
                byte[] s = transaction.get(9).getRLPData();
                this.signature = ECDSASignature.fromComponents(r, s, v);
            } else {
                logger.debug("RLP encoded tx is not signed!");
            }
        } else {
            RLPList decodedTxList = RLP.decode2(rlpEncodedComposite);
            RLPList transaction = (RLPList) decodedTxList.get(0);
//            logger.info("composite transaction item size is {}", transaction.size());
            this.version = transaction.get(0).getRLPData() == null ? (byte) 0 : transaction.get(0).getRLPData()[0];
            //logger.info("item version is {}",(int)this.version);
            //logger.info("item option size is {}",transaction.get(1) == null ? 0 : transaction.get(1).getRLPData().length);
            this.option = transaction.get(1).getRLPData() == null ? (byte) 0 : transaction.get(1).getRLPData()[0];
            this.timeStamp = transaction.get(2).getRLPData();
            //logger.info("item timestamp is {}",ByteUtil.byteArrayToLong(this.timeStamp));
            this.toAddress = transaction.get(3).getRLPData();
            this.amount = transaction.get(4).getRLPData();
            this.fee = transaction.get(5).getRLPData();
            this.expireTime = transaction.get(6).getRLPData();
            this.senderWitnessAddress = transaction.get(7).getRLPData();
            this.receiverWitnessAddress = transaction.get(8).getRLPData();

            RLPList senderList = (RLPList) transaction.get(9);
            if (senderList != null) {
                for (RLPElement senderAssociate : senderList) {
                    this.senderAssociatedAddress.add(senderAssociate.getRLPData());
                }
            }

            RLPList receiverList = (RLPList) transaction.get(10);
            if (receiverList != null) {
                for (RLPElement receiverAssociate : receiverList) {
                    this.receiverAssociatedAddress.add(receiverAssociate.getRLPData());
                }
            }

            // only parse signature in case tx is signed
            if (transaction.get(11).getRLPData() != null) {
                byte v = transaction.get(11).getRLPData()[0];
                byte[] r = transaction.get(12).getRLPData();
                byte[] s = transaction.get(13).getRLPData();
                this.signature = ECDSASignature.fromComponents(r, s, v);
            } else {
                logger.debug("RLP encoded tx is not signed!");
            }
        }
        this.parsed = true;
        this.hash = getHash();
    }

    public boolean validate() {
        if (toAddress != null && toAddress.length != 0 && toAddress.length != ADDRESS_LENGTH)
            return false;

        if (getSignature() != null) {
            if (BigIntegers.asUnsignedByteArray(signature.r).length > HASH_LENGTH)
                return false;
            if (BigIntegers.asUnsignedByteArray(signature.s).length > HASH_LENGTH)
                return false;
            if (getSender() != null && getSender().length != ADDRESS_LENGTH)
                return false;
        }

        return true;
    }

    public boolean isParsed() {
        return parsed;
    }
    
    //entire transaction hash code
    public byte[] getHash() {
        if (!parsed) rlpParse();
        byte[] plainMsg = this.getEncoded();
        return HashUtil.sha3(plainMsg);
    }

    //get txid for wallet
    public String getTxid() {
        return Hex.toHexString(getHash());
    }
    
    // transaction except to signature
    public byte[] getRawHash() {
        if (!parsed) rlpParse();
        byte[] plainMsg = this.getEncodedRaw();
        return HashUtil.sha3(plainMsg);
    }

    public byte[] getTime() {
        if (!parsed) rlpParse();

        return timeStamp;
    }

    public byte[] getAmount() {
        if (!parsed) rlpParse();
        return amount;
    }

    public byte[] getFee() {
        if (!parsed) rlpParse();
        return fee;
    }

    private BigInteger getBigIntegerFee() {
        if (!parsed) rlpParse();
        return new BigInteger(fee);
    }

    public BigInteger getTotoalCost() {
        if (!parsed) rlpParse();
        return (new BigInteger(amount)).add(new BigInteger(fee));
    }

    public byte[] getReceiveAddress() {
        if (!parsed) rlpParse();
        return toAddress;
    }

    public ECDSASignature getSignature() {
        if (!parsed) rlpParse();
        return signature;
    }

    /*
     * Crypto, recover ECKey that only contains compressd pubkey
     */

    public ECKey getKey() {
        byte[] hash = getRawHash();
        return ECKey.recoverFromSignature(signature.v, signature, hash, true);
    }
    /*
    * Crypto,recover compressed pubkey from signature further get sender address
    */

    public byte[] getSender() {
        try {
            if (sendAddress == null) {
                ECKey key = ECKey.signatureToKey(getRawHash(), getSignature().toBase64());
                sendAddress = key.getAddress();
            }
            return sendAddress;
        } catch (SignatureException e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    public void sign(byte[] privKeyBytes) throws MissingPrivateKeyException {
        byte[] hash = this.getRawHash();
        ECKey key = ECKey.fromPrivate(privKeyBytes).decompress();
        this.signature = key.sign(hash);
        this.rlpEncoded = null;
    }

    @Override
    public String toString() {
        if (!parsed) rlpParse();
        return "TransactionData [" +
                "  version=" + ByteUtil.toHexString(new byte[]{version}) +
                ", option=" + ByteUtil.toHexString(new byte[]{option}) +
                ", time=" + ByteUtil.byteArrayToLong(timeStamp) +
                ", receiveAddress=" + ByteUtil.toHexString(toAddress) +
                ", amount="  + ByteUtil.byteArrayToLong(amount) +
                ", fee=" + ByteUtil.byteArrayToLong(fee) +
                ", expireTime="+  ByteUtil.byteArrayToInt(expireTime)+
                ", signatureV=" + (signature == null ? "" : signature.v) +
                ", signatureR=" + (signature == null ? "" : ByteUtil.toHexString(BigIntegers.asUnsignedByteArray(signature.r))) +
                ", signatureS=" + (signature == null ? "" : ByteUtil.toHexString(BigIntegers.asUnsignedByteArray(signature.s))) +
                "]";
    }

    /**
     * For signatures you have to keep also
     * RLP of the transaction without any signature data
     */
    public byte[] getEncodedRaw() {

        if (!parsed) rlpParse();
        if (rlpRaw != null) return rlpRaw;
        byte[] version = RLP.encodeByte(this.version);
        byte[] option = RLP.encodeByte(this.option);
        byte[] timeStamp = RLP.encodeElement(this.timeStamp);
        byte[] toAddress = RLP.encodeElement(this.toAddress);
        byte[] amount = RLP.encodeElement(this.amount);
        byte[] fee = RLP.encodeElement(this.fee);
        byte[] expireTime = RLP.encodeElement(this.expireTime);

        rlpRaw = RLP.encodeList(version, option, timeStamp, toAddress,
                amount, fee,expireTime);
        return rlpRaw;
    }

    public byte[] getEncoded() {
        if (!parsed) rlpParse();
        if (rlpEncoded != null) return rlpEncoded;

        byte[] version = RLP.encodeByte(this.version);
        byte[] option = RLP.encodeByte(this.option);
        byte[] timeStamp = RLP.encodeElement(this.timeStamp);
        byte[] toAddress = RLP.encodeElement(this.toAddress);
        byte[] amount = RLP.encodeElement(this.amount);
        byte[] fee = RLP.encodeElement(this.fee);
        byte[] expireTime = RLP.encodeElement(this.expireTime);

        byte[] v, r, s;

        if (signature != null) {
            v = RLP.encodeByte(signature.v);
            r = RLP.encodeElement(BigIntegers.asUnsignedByteArray(signature.r));
            s = RLP.encodeElement(BigIntegers.asUnsignedByteArray(signature.s));
        } else {
            v = RLP.encodeElement(EMPTY_BYTE_ARRAY);
            r = RLP.encodeElement(EMPTY_BYTE_ARRAY);
            s = RLP.encodeElement(EMPTY_BYTE_ARRAY);
        }

        this.rlpEncoded = RLP.encodeList(version, option, timeStamp,
                toAddress, amount, fee, expireTime, v, r, s);

        this.hash = this.getHash();

        return rlpEncoded;
    }

    //encode transaction used to disk.
    public byte[] getEncodedComposite() {
        if (!parsed) rlpParse();
        if (rlpEncodedComposite != null) return rlpEncodedComposite;

        byte[] version = RLP.encodeByte(this.version);
        byte[] option = RLP.encodeByte(this.option);
        byte[] timeStamp = RLP.encodeElement(this.timeStamp);
        byte[] toAddress = RLP.encodeElement(this.toAddress);
        byte[] amount = RLP.encodeElement(this.amount);
        byte[] fee = RLP.encodeElement(this.fee);
        byte[] expireTime = RLP.encodeElement(this.expireTime);
        byte[] senderWitnessAddress = RLP.encodeElement(this.senderWitnessAddress);
        byte[] receiverWitnessAddress = RLP.encodeElement(this.receiverWitnessAddress);
        byte[][] senderAssociate = new byte[this.senderAssociatedAddress.size()][];
        for (int i=0; i< this.senderAssociatedAddress.size(); ++i) {
            senderAssociate[i] = RLP.encodeElement(this.senderAssociatedAddress.get(i));
        }
        byte[] senderAssociatedAddress = RLP.encodeList(senderAssociate);

        byte[][] receiverAssociate = new byte[this.receiverAssociatedAddress.size()][];
        for (int i=0; i< this.receiverAssociatedAddress.size(); ++i) {
            receiverAssociate[i] = RLP.encodeElement(this.receiverAssociatedAddress.get(i));
        }
        byte[] receiverAssociatedAddress = RLP.encodeList(receiverAssociate);

        byte[] v, r, s;

        if (signature != null) {
            v = RLP.encodeByte(signature.v);
            r = RLP.encodeElement(BigIntegers.asUnsignedByteArray(signature.r));
            s = RLP.encodeElement(BigIntegers.asUnsignedByteArray(signature.s));
        } else {
            v = RLP.encodeElement(EMPTY_BYTE_ARRAY);
            r = RLP.encodeElement(EMPTY_BYTE_ARRAY);
            s = RLP.encodeElement(EMPTY_BYTE_ARRAY);
        }

        this.rlpEncodedComposite = RLP.encodeList(version, option, timeStamp,
                toAddress, amount, fee,expireTime, senderWitnessAddress, receiverWitnessAddress, senderAssociatedAddress, receiverAssociatedAddress,v, r, s);
        this.hash = this.getHash();

        return rlpEncodedComposite;
    }

    @Override
    public int hashCode() {

        byte[] hash = this.getHash();
        int hashCode = 0;

        for (int i = 0; i < hash.length; ++i) {
            hashCode += hash[i] * i;
        }

        return hashCode;
    }

    @Override
    public boolean equals(Object obj) {

        if (!(obj instanceof Transaction)) return false;
        Transaction tx = (Transaction) obj;

        return tx.hashCode() == this.hashCode();
    }

    public static Transaction create(BigInteger version,BigInteger option,BigInteger timeStamp,String to, BigInteger amount, BigInteger fee){
        return new Transaction(BigIntegers.asUnsignedByteArray(version)[0],
                BigIntegers.asUnsignedByteArray(option)[0],
                BigIntegers.asUnsignedByteArray(timeStamp),
                Hex.decode(to),
                BigIntegers.asUnsignedByteArray(amount),
                BigIntegers.asUnsignedByteArray(fee));
    }

    public byte[] getExpireTime() {
        return expireTime;
    }
}
