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

import java.io.IOException;
import java.math.BigInteger;
import java.security.SignatureException;
import java.util.ArrayList;

import static io.taucoin.util.ByteUtil.*;
import static org.apache.commons.lang3.ArrayUtils.isEmpty;

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

    /**
     * sender Address to used retrieve account info quickly.
     * it is a key to Turbo apk.local app also used it.
     */
    private byte[] sendAddress = null;
    
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

    /**
     *property of genesisTransaction
     *coinName that is new chain name property 32 bytes.
     *coinTotalAmount that is total supply of new block chain 5 bytes.
     */
    private byte[] coinName = null;
    private byte[] coinTotalAmount = null;

    public static final int TTIME = 144;
    public static final int HASH_LENGTH = 32;
    public static final int ADDRESS_LENGTH = 20;
    public static final byte tflag = 0;
    public static final byte xflag = 1;

    /* Tx in encoded form */
    protected byte[] rlpEncoded;
    private byte[] rlpEncodedSig = null;
    private byte[] rlpEncodedHash = null;
    private byte[] rlpEncodedCache = null;
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

    /**
     * creation tx that contains expire time but without tx signature.
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

    /**
     * creation genesis tx that contains expire time but without tx signature.
     * @param version
     * @param option
     * @param timeStamp
     * @param toAddress
     * @param amount
     * @param fee
     * @param expireTime
     * @param coinName
     * @param coinTotalAmount
     */
    public Transaction(byte version, byte option, byte[] timeStamp, byte[] toAddress, byte[] amount, byte[] fee,byte[] expireTime,byte[] coinName,byte[] coinTotalAmount) {
        this.version = version;
        this.option = option;
        this.timeStamp = timeStamp;
        this.toAddress = toAddress;
        this.amount = amount;
        this.fee = fee;
        this.expireTime = expireTime;
        this.coinName = coinName;
        this.coinTotalAmount = coinTotalAmount;

        if (toAddress == null) {
            //burn some money
            this.toAddress = ByteUtil.EMPTY_BYTE_ARRAY;
        }
        parsed = true;
    }

    /**
     * creation tx that contains default expire time(144 block). this expire time is default 144*5*60.
     * without tx signature.
     * @param version
     * @param option
     * @param timeStamp
     * @param toAddress
     * @param amount
     * @param fee
     */
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

    /**
     * creation genesis tx that contains default expire time(144 block),this expire time is default 144*5*60
     * without tx signature.
     * @param version
     * @param option
     * @param timeStamp
     * @param toAddress
     * @param amount
     * @param fee
     * @param coinName
     * @param coinTotalAmount
     */
    public Transaction(byte version, byte option, byte[] timeStamp, byte[] toAddress, byte[] amount, byte[] fee,byte[] coinName,byte[] coinTotalAmount) {
        this.version = version;
        this.option = option;
        this.timeStamp = timeStamp;
        this.toAddress = toAddress;
        this.amount = amount;
        this.fee = fee;
        this.expireTime = shortToBytes((short)TTIME);
        this.coinName = coinName;
        this.coinTotalAmount = coinTotalAmount;
        
        if (toAddress == null) {
            //burn some money
            this.toAddress = ByteUtil.EMPTY_BYTE_ARRAY;
        }

        parsed = true;
    }

    /**
     * creation tx that contains tx signature and expire time
     * @param version
     * @param option
     * @param timeStamp
     * @param toAddress
     * @param amount
     * @param fee
     * @param expireTime
     * @param r
     * @param s
     * @param v
     */
    public Transaction(byte version, byte option, byte[] timeStamp, byte[] toAddress, byte[] amount, byte[] fee,byte[] expireTime, byte[] r, byte[] s, byte v) {
        this(version, option, timeStamp, toAddress, amount, fee,expireTime);
        ECDSASignature signature = new ECDSASignature(new BigInteger(r), new BigInteger(s));
        signature.v = v;
        this.signature = signature;
    }

    /**
     * creation genesis tx that contains tx signature and expire time
     * @param version
     * @param option
     * @param timeStamp
     * @param toAddress
     * @param amount
     * @param fee
     * @param expireTime
     * @param coinName
     * @param coinTotalAmount
     * @param r
     * @param s
     * @param v
     */
    public Transaction(byte version, byte option, byte[] timeStamp, byte[] toAddress, byte[] amount, byte[] fee,byte[] expireTime,byte[] coinName,byte[] coinTotalAmount, byte[] r, byte[] s, byte v) {
        this(version, option, timeStamp, toAddress, amount, fee,expireTime,coinName,coinTotalAmount);
        ECDSASignature signature = new ECDSASignature(new BigInteger(r), new BigInteger(s));
        signature.v = v;
        this.signature = signature;
    }

    /**
     * creation tx that contains tx signature and default expire time(144 blocks count)
     * @param version
     * @param option
     * @param timeStamp
     * @param toAddress
     * @param amount
     * @param fee
     * @param r
     * @param s
     * @param v
     */
    public Transaction(byte version, byte option, byte[] timeStamp, byte[] toAddress, byte[] amount, byte[] fee, byte[] r, byte[] s, byte v) {
        this(version, option, timeStamp, toAddress, amount, fee);
        ECDSASignature signature = new ECDSASignature(new BigInteger(r), new BigInteger(s));
        signature.v = v;
        this.signature = signature;
    }

    /**
     * creation genesis tx that contains tx signature and default expire time(144 blocks count)
     * @param version
     * @param option
     * @param timeStamp
     * @param toAddress
     * @param amount
     * @param fee
     * @param coinName
     * @param coinTotalAmount
     * @param r
     * @param s
     * @param v
     */
    public Transaction(byte version, byte option, byte[] timeStamp, byte[] toAddress, byte[] amount, byte[] fee,byte[] coinName,byte[] coinTotalAmount, byte[] r, byte[] s, byte v) {
        this(version, option, timeStamp, toAddress, amount, fee,coinName,coinTotalAmount);
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
            if (this.version == tflag && this.option == tflag) {
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

                /**
                * a<item></>
                * transaction from memory pool hasn't contained sendAddress
                * transaction from block synced has contained sendAddress.
                * e<item></>
                * transaction from block mined by self hasn't contained senderAddress
                */
                if (transaction.size() > 10) {
                     this.sendAddress = transaction.get(10).getRLPData();
                }
            } else if (this.version == xflag && this.option == xflag) {
                this.timeStamp = transaction.get(2).getRLPData();
                //logger.info("item timestamp is {}",ByteUtil.byteArrayToLong(this.timeStamp));
                this.toAddress = transaction.get(3).getRLPData();
                this.amount = transaction.get(4).getRLPData();
                this.fee = transaction.get(5).getRLPData();
                this.expireTime = transaction.get(6).getRLPData();
                this.coinName = transaction.get(7).getRLPData();
                if (this.coinName.length > 32) {
                    throw new IllegalArgumentException("x chain name too long");
                }
                this.coinTotalAmount = transaction.get(8).getRLPData();
                if (this.coinTotalAmount.length > 5) {
                    throw new IllegalArgumentException("x chain total supply is too much");
                }

                // only parse signature in case tx is signed
                if (transaction.get(9).getRLPData() != null) {
                    byte v = transaction.get(9).getRLPData()[0];
                    byte[] r = transaction.get(10).getRLPData();
                    byte[] s = transaction.get(11).getRLPData();
                    this.signature = ECDSASignature.fromComponents(r, s, v);
                } else {
                    logger.debug("RLP encoded tx is not signed!");
                }

                /**
                 * a<item></>
                 * transaction from memory pool hasn't contained sendAddress
                 * transaction from block synced has contained sendAddress.
                 * e<item></>
                 * transaction from block mined by self hasn't contained senderAddress
                 */
                if (transaction.size() > 12) {
                    this.sendAddress = transaction.get(12).getRLPData();
                }
            }
        } else {
            RLPList decodedTxList = RLP.decode2(rlpEncodedComposite);
            RLPList transaction = (RLPList) decodedTxList.get(0);
//            logger.info("composite transaction item size is {}", transaction.size());
            this.version = transaction.get(0).getRLPData() == null ? (byte) 0 : transaction.get(0).getRLPData()[0];
            //logger.info("item version is {}",(int)this.version);
            //logger.info("item option size is {}",transaction.get(1) == null ? 0 : transaction.get(1).getRLPData().length);
            this.option = transaction.get(1).getRLPData() == null ? (byte) 0 : transaction.get(1).getRLPData()[0];
            if (this.version == tflag && this.option == tflag) {
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

                /**
                 * b<item></>
                 * transaction from block stored local disk
                 *   1,up to now this hasn't contained senderAddress.
                 *   2,current this has contained senderAddress.
                */
                if (transaction.size() > 14) {
                    this.sendAddress = transaction.get(14).getRLPData();
                }
            } else if (this.version == xflag && this.option == xflag){
                this.timeStamp = transaction.get(2).getRLPData();
                //logger.info("item timestamp is {}",ByteUtil.byteArrayToLong(this.timeStamp));
                this.toAddress = transaction.get(3).getRLPData();
                this.amount = transaction.get(4).getRLPData();
                this.fee = transaction.get(5).getRLPData();
                this.expireTime = transaction.get(6).getRLPData();
                this.coinName = transaction.get(7).getRLPData();
                if (coinName.length > 32) {
                    throw new IllegalArgumentException("x chain name too long");
                }
                this.coinTotalAmount = transaction.get(8).getRLPData();
                if (coinTotalAmount.length > 5) {
                    throw new IllegalArgumentException("x chain total supply too much");
                }
                this.senderWitnessAddress = transaction.get(9).getRLPData();
                this.receiverWitnessAddress = transaction.get(10).getRLPData();

                RLPList senderList = (RLPList) transaction.get(11);
                if (senderList != null) {
                    for (RLPElement senderAssociate : senderList) {
                        this.senderAssociatedAddress.add(senderAssociate.getRLPData());
                    }
                }

                RLPList receiverList = (RLPList) transaction.get(12);
                if (receiverList != null) {
                    for (RLPElement receiverAssociate : receiverList) {
                        this.receiverAssociatedAddress.add(receiverAssociate.getRLPData());
                    }
                }

                // only parse signature in case tx is signed
                if (transaction.get(13).getRLPData() != null) {
                    byte v = transaction.get(13).getRLPData()[0];
                    byte[] r = transaction.get(14).getRLPData();
                    byte[] s = transaction.get(15).getRLPData();
                    this.signature = ECDSASignature.fromComponents(r, s, v);
                } else {
                    logger.debug("RLP encoded tx is not signed!");
                }

                /**
                 * b<item></>
                 * transaction from block stored local disk
                 *   1,up to now this hasn't contained senderAddress.
                 *   2,current this has contained senderAddress.
                 */
                if (transaction.size() > 16) {
                    this.sendAddress = transaction.get(16).getRLPData();
                }
            }
        }
        this.parsed = true;
    }

    /**
     * this situation blocks and tx are from another store
     */
    public void rlpSyncParseDisk(){

        if (sendAddress != null) {
           return;
        }
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
        if (!isEmpty(hash)) return hash;
        if (!parsed) rlpParse();
        byte[] plainMsg = this.getEncodedHash();
        hash = HashUtil.sha3(plainMsg);
        return hash;
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

    public BigInteger getBigIntegerFee() {
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

    /**
     * Crypto, recover ECKey that only contains compressd pubkey
     */
    public ECKey getKey() {
        byte[] hash = getRawHash();
        return ECKey.recoverFromSignature(signature.v, signature, hash, true);
    }

    /**
     * firstly,try to parse from transaction if not contained
     * secondly,Crypto,recover compressed pubkey from signature further get sender address
    */
    public byte[] getSender() {
        if (sendAddress != null) return sendAddress;
        if (!parsed) rlpParse();
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
     * if tx version and option is 1 sign genesis tx.
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
        if (this.version == xflag && this.option == xflag) {
            byte[] coinName = RLP.encodeElement(this.coinName);
            byte[] coinTotalAmount = RLP.encodeElement(this.coinTotalAmount);
            rlpRaw = RLP.encodeList(version, option, timeStamp, toAddress,
                    amount, fee, expireTime,coinName,coinTotalAmount);
        } else {

            rlpRaw = RLP.encodeList(version, option, timeStamp, toAddress,
                    amount, fee, expireTime);
        }
        return rlpRaw;
    }

    /**
     * this method encode transaction used to transfer this to peer
     * and get hash of it also used to encode tx in block will be
     * sent to peer.
     * if version and option equals xflag means genesis tx drawed up by node.
     * @return
     */
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
        /**
         * c<item></>
         * transaction that is built local node hasn't contained senderAddress
         * transaction that included in block forged by self hasn't contained
         * senderAddress,so nothing to do...
         */
        if (this.version == xflag && this.option == xflag) {
            byte[] coinName = RLP.encodeElement(this.coinName);
            byte[] coinTotalAmount = RLP.encodeElement(this.coinTotalAmount);
            this.rlpEncoded = RLP.encodeList(version, option, timeStamp,
                   toAddress, amount, fee, expireTime, coinName, coinTotalAmount, v, r, s);
        } else {
            this.rlpEncoded = RLP.encodeList(version, option, timeStamp,
                    toAddress, amount, fee, expireTime, v, r, s);
        }
        return rlpEncoded;
    }

    /**
     * encode used when block downloaded was cached in BlockQueueFileSys
     * @return
     */
    public byte[] getEncodedForCache() {
        if (!parsed) rlpParse();
        if (rlpEncodedCache != null) return rlpEncodedCache;

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
        /**
         * because this cache is turboing ,it should be saved in block
         */
        if (this.version == xflag && this.option == xflag) {
            byte[] coinName = RLP.encodeElement(this.coinName);
            byte[] coinTotalAmount = RLP.encodeElement(this.coinTotalAmount);

            if (sendAddress == null) {
                this.sendAddress = getSender();
            }
            byte[] sendAddress = RLP.encodeElement(this.sendAddress);
            this.rlpEncodedCache = RLP.encodeList(version, option, timeStamp,
                    toAddress, amount, fee, expireTime, coinName, coinTotalAmount, v, r, s, sendAddress);
        } else {
            if (sendAddress == null) {
                this.sendAddress = getSender();
            }
            byte[] sendAddress = RLP.encodeElement(this.sendAddress);
            this.rlpEncodedCache = RLP.encodeList(version, option, timeStamp,
                    toAddress, amount, fee, expireTime, v, r, s, sendAddress);
        }
        return rlpEncodedCache;
    }

    /**
     * Encoded used when block was signed.
     * @return
     */
    public byte[] getEncodeForSig() {
        if (!parsed) rlpParse();
        if (rlpEncodedSig != null) return rlpEncodedSig;

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

        if (this.version == xflag && this.option == xflag) {
            byte[] coinName = RLP.encodeElement(this.coinName);
            byte[] coinTotalAmount = RLP.encodeElement(this.coinTotalAmount);
            this.rlpEncodedSig = RLP.encodeList(version, option, timeStamp,
                    toAddress, amount, fee, expireTime, coinName, coinTotalAmount, v, r, s);
        } else {
            this.rlpEncodedSig = RLP.encodeList(version, option, timeStamp,
                    toAddress, amount, fee, expireTime, v, r, s);
        }
        return rlpEncodedSig;
    }

    public byte[] getEncodedHash() {
        if (!parsed) rlpParse();
        if (rlpEncodedHash != null) return rlpEncodedHash;

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

        if (this.version == xflag && this.option == xflag) {
            byte[] coinName = RLP.encodeElement(this.coinName);
            byte[] coinTotalAmount = RLP.encodeElement(this.coinTotalAmount);
            this.rlpEncodedHash = RLP.encodeList(version, option, timeStamp,
                    toAddress, amount, fee, expireTime, coinName, coinTotalAmount, v, r, s);
        } else {
            this.rlpEncodedHash = RLP.encodeList(version, option, timeStamp,
                    toAddress, amount, fee, expireTime, v, r, s);
        }
        return rlpEncodedHash;
    }

    /**
     * encode transaction used to disk.
     */
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

        /**
         * d<item></>
         * transaction included in block forged by self hasn't contained senderAddress.
         * transaction included in block up to now hasn't contained senderAddress.
         * transaction included in block synced from peer has contained senderAddress currently
         */
        if (sendAddress == null) {
            this.sendAddress = getSender();
        }

        byte[] sendAddress = RLP.encodeElement(this.sendAddress);

        if (this.version == xflag && this.option == xflag) {
            byte[] coinName = RLP.encodeElement(this.coinName);
            byte[] coinTotalAmount = RLP.encodeElement(this.coinTotalAmount);

            this.rlpEncodedComposite = RLP.encodeList(version, option, timeStamp,
                    toAddress, amount, fee, expireTime, coinName, coinTotalAmount,
                    senderWitnessAddress, receiverWitnessAddress, senderAssociatedAddress, receiverAssociatedAddress,
                    v, r, s, sendAddress);
        } else {
            this.rlpEncodedComposite = RLP.encodeList(version, option, timeStamp,
                    toAddress, amount, fee, expireTime, senderWitnessAddress, receiverWitnessAddress,
                    senderAssociatedAddress, receiverAssociatedAddress, v, r, s, sendAddress);
        }
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
        if (!parsed) rlpParse();
        return expireTime;
    }

    public byte[] getCoinName() throws IOException {
        if (!parsed) rlpParse();
        if (this.version != xflag || this.option != xflag) {
            throw new IOException("nugenesis transaction has no coin name");
        }
        return coinName;
    }

    public byte[] getCoinTotalAmount() throws IOException {
        if (!parsed) rlpParse();
        if (this.version != xflag || this.option != xflag) {
            throw new IOException("ungenesis transaction without this property");
        }
        return coinTotalAmount;
    }
}
