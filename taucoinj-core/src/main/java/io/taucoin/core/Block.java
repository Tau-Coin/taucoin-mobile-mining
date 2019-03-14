package io.taucoin.core;

import io.taucoin.crypto.ECKey;
import io.taucoin.crypto.ECKey.ECDSASignature;
import io.taucoin.crypto.ECKey.MissingPrivateKeyException;
import io.taucoin.crypto.HashUtil;
import io.taucoin.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.Arrays;
import org.spongycastle.util.BigIntegers;
import org.spongycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static io.taucoin.config.SystemProperties.CONFIG;

/**
 * The block in taucoin is the collection of relevant pieces of information
 * (known as the blockheader), H, together with information corresponding to
 * the comprised transactions, T.
 */
public class Block {

    private static final Logger logger = LoggerFactory.getLogger("block");

    private BlockHeader header;

    /*ensure the integrity of the block 512 bits*/
    /* the elliptic curve signature
     * (excluding public key recovery bits) */
    private ECDSASignature blockSignature;
    /*this is left for future use 8 bits*/
    private byte option;
    /* A scalar value equal to the number of ancestor blocks.
     * The genesis block has a number of zero */
    private long number;
    private BigInteger baseTarget; //this is uint64 type so here we should use compact type
    private byte[] generationSignature;
    private BigInteger cumulativeDifficulty = BigInteger.ZERO; //this is total chain difficulty
    private BigInteger cumulativeFee = BigInteger.ZERO;

    /* Transactions */
    private List<Transaction> transactionsList = new CopyOnWriteArrayList<>();

    protected byte[] rlpEncoded;
    private byte[] rlpEncodedMsg;
    private byte[] rlpRaw;
    private boolean isMsg = false;
    private boolean parsed = false;

    /* Constructors */

    private Block() {
    }

    public Block(byte[] rawData) {
        logger.debug("new from [" + Hex.toHexString(rawData) + "]");
        this.rlpEncoded = rawData;
        this.rlpEncodedMsg = null;
        this.rlpRaw = null;
        this.parsed = false;
    }

    public Block(byte[] rawData, boolean isMsg) {
        logger.debug("new from net [" + Hex.toHexString(rawData) + "]");
        if (isMsg) {
            this.rlpEncoded = null;
            this.rlpEncodedMsg = rawData;
            this.rlpRaw = null;
        } else {
            this.rlpEncoded = rawData;
            this.rlpEncodedMsg = null;
            this.rlpRaw = null;
        }
        this.parsed = false;
        this.isMsg = isMsg;
    }

    public Block(BlockHeader header, byte[] r, byte[] s, byte option,List<Transaction> transactionsList) {

        this(header.getVersion(),
                header.getTimeStamp(),
                header.getPreviousHeaderHash(),
                header.getGeneratorPublicKey(),
                r,
                s,
                option,
                transactionsList);
    }

    public Block(byte version, byte[] timestamp, byte[] previousHeaderHash, byte[] generatorPublickey,
                 byte option, List<Transaction> transactionsList) {
        /*
         * TODO: calculate GenerationSignature
         *
         */
        this.header = new BlockHeader(version, timestamp, previousHeaderHash, generatorPublickey);

        this.option = option;

        this.transactionsList = transactionsList;
        if (this.transactionsList == null) {
            this.transactionsList = new CopyOnWriteArrayList<>();
        }

        this.parsed = true;
    }

    public Block(byte version, byte[] timestamp, byte[] previousHeaderHash, byte[] generatorPublickey,
                 byte[] r, byte[] s, byte option,
                 List<Transaction> transactionsList) {
        /*
        * TODO: calculate GenerationSignature
        *
         */
        this.header = new BlockHeader(version, timestamp, previousHeaderHash, generatorPublickey);

        ECDSASignature signature = new ECDSASignature(new BigInteger(r), new BigInteger(s));
        this.blockSignature = signature;

        this.option = option;
        this.transactionsList = transactionsList;
        if (this.transactionsList == null) {
            this.transactionsList = new CopyOnWriteArrayList<>();
        }

        this.parsed = true;
    }


    private void parseRLP() {

        if (!isMsg) {
            RLPList params = RLP.decode2(rlpEncoded);
            RLPList block = (RLPList) params.get(0);

            // Parse Header
            RLPList header = (RLPList) block.get(0);
            this.header = new BlockHeader(header);

            byte[] nrBytes = block.get(1).getRLPData();
            this.number = nrBytes == null ? 0 : (new BigInteger(1, nrBytes)).longValue();

            byte[] btBytes = block.get(2).getRLPData();
            this.baseTarget = new BigInteger(1, btBytes);

            this.generationSignature = block.get(3).getRLPData();

            byte[] cyBytes = block.get(4).getRLPData();
            this.cumulativeDifficulty = cyBytes == null ? BigInteger.ZERO
                    : new BigInteger(1, cyBytes);

            byte[] culFee  = block.get(5).getRLPData();
            this.cumulativeFee = culFee == null ? BigInteger.ZERO
                    : new BigInteger(1,culFee);

            RLPList items = (RLPList) RLP.decode2(block.get(6).getRLPData()).get(0);
            logger.info("items size is {}",items.size());
            // Parse blockSignature
            byte[] r = items.get(0).getRLPData();
            byte[] s = items.get(1).getRLPData();
            this.blockSignature = ECDSASignature.fromComponents(r, s);
            // Parse option
            this.option = block.get(7).getRLPData()[0];

            if(block.size() > 8) {
                // Parse Transactions
                RLPList txTransactions = (RLPList) block.get(8);
                // here may need original trie
                this.parseTxs(/*this.header.getTxTrieRoot()*/ txTransactions);
            }
        } else {
            RLPList params = RLP.decode2(rlpEncodedMsg);
            RLPList block = (RLPList) params.get(0);

            // Parse Header
            RLPList header = (RLPList) block.get(0);
            this.header = new BlockHeader(header);

            // Parse blockSignature
            RLPList items = (RLPList) RLP.decode2(block.get(1).getRLPData()).get(0);
            byte[] r = items.get(0).getRLPData();
            byte[] s = items.get(1).getRLPData();
            this.blockSignature = ECDSASignature.fromComponents(r, s);
            // Parse option
            this.option = block.get(2).getRLPData()[0];
            // Parse Transactions
            if(block.size() > 3){
                RLPList txTransactions = (RLPList) block.get(3);
                this.parseTxs(/*this.header.getTxTrieRoot()*/ txTransactions);
            }
        }

        this.parsed = true;
    }


    public boolean isMsg() {
        return isMsg;
    }

    /**
     * Indicate this block is from network
     */
    public void setIsMsg(boolean isMsg) {
        this.isMsg = isMsg;
    }

    public BlockHeader getHeader() {
        if (!parsed) parseRLP();
        return this.header;
    }

    public byte[] getHash() {
        if (!parsed) parseRLP();
        //current block hash (sha256 ripemd160)
        //return HashUtil.ripemd160(HashUtil.sha256(this.getEncoded()));
        return this.header.getHash();
    }

    public byte[] getPreviousHeaderHash() {
        if (!parsed) parseRLP();
        return this.header.getPreviousHeaderHash();
    }

    public byte[] getTimestamp() {
        if (!parsed) parseRLP();
        return this.header.getTimeStamp();
    }

    public byte[] getGeneratorPublicKey() {
        if (!parsed) parseRLP();
        return this.header.getGeneratorPublicKey();
    }

    public byte getVersion() {
        if (!parsed) parseRLP();
        return this.header.getVersion();
    }

    public ECDSASignature getblockSignature(){
        if (!parsed) parseRLP();
        return this.blockSignature;
    }

    public byte getOption() {
        if (!parsed) parseRLP();
        return this.option;
    }

    public void setNumber(long number) {
        this.number = number;
    }

    public long getNumber() {
        if (!parsed) parseRLP();
        return this.number;
    }

    public void setBaseTarget(BigInteger baseTarget) {
        this.baseTarget = baseTarget;
    }

    public BigInteger getBaseTarget() {
        if (!parsed) parseRLP();
        return this.baseTarget;
    }

    public void setGenerationSignature(byte[] generationSignature) {
        this.generationSignature = generationSignature;
    }

    public byte[] getGenerationSignature() {
        if (!parsed) parseRLP();
        return this.generationSignature;
    }

    public void setCumulativeDifficulty(BigInteger cumulativeDifficulty) {
        this.cumulativeDifficulty = cumulativeDifficulty;
    }

    public BigInteger getCumulativeDifficulty() {
        if (!parsed) parseRLP();
        return this.cumulativeDifficulty;
    }
    public BigInteger getCumulativeFee() {
        if (!parsed) parseRLP();
        return cumulativeFee;
    }

    public void setCumulativeFee(BigInteger cumulativeFee) {
        this.cumulativeFee = cumulativeFee;
    }

    public List<Transaction> getTransactionsList() {
        if (!parsed) parseRLP();
        return transactionsList;
    }


    private StringBuffer toStringBuff = new StringBuffer();
    // [parent_hash, uncles_hash, coinbase, state_root, tx_trie_root,
    // difficulty, number, minGasPrice, gasLimit, gasUsed, timestamp,
    // extradata, nonce]

    @Override
    public String toString() {

        if (!parsed) parseRLP();

        toStringBuff.setLength(0);
        toStringBuff.append(Hex.toHexString(this.getEncodedMsg())).append("\n");
        toStringBuff.append("BlockData [ ");
        toStringBuff.append("hash=" + ByteUtil.toHexString(this.getHash())).append("\n");
        toStringBuff.append(header.toString());
//        toStringBuff.append("blocksig=" + ByteUtil.toHexString(this.blockSignature)).append("\n");
//        toStringBuff.append("option=" + ByteUtil.toHexString(this.option)).append("\n");
        toStringBuff.append("\nTransactions [\n");
        for (Transaction tx : getTransactionsList()) {
            toStringBuff.append("\n");
            toStringBuff.append(tx.toString());
        }
        toStringBuff.append("]");
        toStringBuff.append("\n]");

        return toStringBuff.toString();
    }

    public String toFlatString() {
        if (!parsed) parseRLP();

        toStringBuff.setLength(0);
        toStringBuff.append("BlockData [");
        toStringBuff.append("hash=").append(ByteUtil.toHexString(this.getHash()));
        toStringBuff.append(header.toFlatString());
//        toStringBuff.append("blocksig=" + ByteUtil.toHexString(this.blockSignature));
        //toStringBuff.append("option=" + ByteUtil.toHexString(this.option));

        for (Transaction tx : getTransactionsList()) {
            toStringBuff.append("\n");
            toStringBuff.append(tx.toString());
        }

        toStringBuff.append("]");
        return toStringBuff.toString();
    }

    private void parseTxs(RLPList txTransactions) {

        for (int i = 0; i < txTransactions.size(); i++) {
            RLPElement transactionRaw = txTransactions.get(i);
            this.transactionsList.add(new Transaction(transactionRaw.getRLPData()));
        }
    }

    /**
     * check if param block is son of this block
     *
     * @param block - possible a son of this
     * @return - true if this block is parent of param block
     */
    public boolean isParentOf(Block block) {
        return Arrays.areEqual(this.getHash(), block.getPreviousHeaderHash());
    }

    public boolean isGenesis() {
        return this.header.isGenesis();
    }

    public boolean isEqual(Block block) {
        return Arrays.areEqual(this.getHash(), block.getHash());
    }

    private byte[] getSignatureEncoded() {
        byte[] r, s;
        r = RLP.encodeElement(BigIntegers.asUnsignedByteArray(blockSignature.r));
        s = RLP.encodeElement(BigIntegers.asUnsignedByteArray(blockSignature.s));
        return RLP.encodeList(r, s);
    }

    private byte[] getOptionEncoded() {
        byte[] option = RLP.encodeByte(this.option);
        return option;
    }

    private byte[] getTransactionsEncoded() {

        byte[][] transactionsEncoded = new byte[transactionsList.size()][];
        int i = 0;
        for (Transaction tx : transactionsList) {
            transactionsEncoded[i] = tx.getEncoded();
            ++i;
        }
        return RLP.encodeList(transactionsEncoded);
    }

    //encode block on disk
    public byte[] getEncoded() {
        if (rlpEncoded == null) {
            byte[] header = this.header.getEncoded();

            List<byte[]> block = getFullBodyElements();
            logger.info("size of encode element is {}",block.size());
            block.add(0, header);
            logger.info("size of encode element is {}",block.size());
            byte[][] elements = block.toArray(new byte[block.size()][]);

            this.rlpEncoded = RLP.encodeList(elements);
        }
        return rlpEncoded;
    }

    //encode block on net
    public byte[] getEncodedMsg() {
        if (rlpEncodedMsg == null) {
            byte[] header = this.header.getEncoded();

            List<byte[]> block = getBodyElements();
            block.add(0, header);
            byte[][] elements = block.toArray(new byte[block.size()][]);

            this.rlpEncodedMsg = RLP.encodeList(elements);
        }
        return rlpEncodedMsg;
    }

    //encode block for signature
    public byte[] getEncodedRaw() {
        if (rlpRaw == null) {
            byte[] header = this.header.getEncoded();

            List<byte[]> block = getBodyElementsWithoutBlockSignature();
            block.add(0, header);
            byte[][] elements = block.toArray(new byte[block.size()][]);

            this.rlpRaw = RLP.encodeList(elements);
        }
        return rlpRaw;
    }


    public byte[] getEncodedBody() {
        List<byte[]> body = getBodyElements();
        byte[][] elements = body.toArray(new byte[body.size()][]);
        return RLP.encodeList(elements);
    }

    private List<byte[]> getBodyElementsWithoutBlockSignature() {
        if (!parsed) parseRLP();

        byte[] option = RLP.encodeByte(this.option);
        byte[] transactions = getTransactionsEncoded();

        List<byte[]> body = new ArrayList<>();
        body.add(option);
        body.add(transactions);

        return body;
    }

    private List<byte[]> getBodyElements() {
        if (!parsed) parseRLP();

        byte[] signature = getSignatureEncoded();
        byte[] option = getOptionEncoded();
        byte[] transactions = getTransactionsEncoded();

        List<byte[]> body = new ArrayList<>();
        body.add(signature);
        body.add(option);
        body.add(transactions);

        return body;
    }

    private List<byte[]> getFullBodyElements() {
        if (!parsed) parseRLP();

        byte[] number = RLP.encodeBigInteger(BigInteger.valueOf(this.number));
        byte[] baseTarget = RLP.encodeBigInteger(this.baseTarget == null ? BigInteger.valueOf(0x0ffffffff): this.baseTarget);
        byte[] generationSignature = RLP.encodeElement(this.generationSignature);
        byte[] cumulativeDifficulty = RLP.encodeBigInteger(this.cumulativeDifficulty == null ? BigInteger.valueOf(0xffffff):this.cumulativeDifficulty);
        byte[] cumulativeFee = RLP.encodeBigInteger(this.cumulativeFee == null ? BigInteger.ZERO: this.cumulativeFee);
        byte[] signature = getSignatureEncoded();
        byte[] option = getOptionEncoded();
        byte[] transactions = getTransactionsEncoded();

        List<byte[]> body = new ArrayList<>();
        body.add(number);
        body.add(baseTarget);
        body.add(generationSignature);
        body.add(cumulativeDifficulty);
        body.add(cumulativeFee);
        body.add(signature);
        body.add(option);
        body.add(transactions);

        return body;
    }

    public String getShortHash() {
        if (!parsed) parseRLP();
        return Hex.toHexString(getHash()).substring(0, 6);
    }

    public byte[] getRawHash() {
        if (!parsed) parseRLP();
        byte[] plainMsg = this.getEncodedRaw();
        return HashUtil.sha3(plainMsg);
    }

    public void sign(byte[] privKeyBytes) throws ECKey.MissingPrivateKeyException {
        byte[] hash = this.getRawHash();
        ECKey key = ECKey.fromPrivate(privKeyBytes).decompress();
        this.blockSignature = key.doSign(hash);
        this.rlpEncoded = null;
        this.rlpEncodedMsg = null;
    }

    /**
     * verify block signature with public key and signature
     * @return
     */
    public boolean verifyBlockSignature() {
        ECKey key = ECKey.fromPublicOnly(getGeneratorPublicKey());
        ECKey.ECDSASignature sig = getblockSignature();
        return key.verify(getRawHash(), sig);
    }

    public String getShortDescr() {
        return "#" + getNumber() + " (" + Hex.toHexString(getHash()).substring(0,6) + " <~ "
                + Hex.toHexString(getPreviousHeaderHash()).substring(0,6) + ") Txs:" + getTransactionsList().size();
    }

    public static class Builder {

        private BlockHeader header;
        private byte[] body;
        // Is from network or disk?
        private boolean isMsg = false;

        public Builder withHeader(BlockHeader header) {
            this.header = header;
            return this;
        }

        public Builder withBody(byte[] body ,boolean isMsg) {
            this.body = body;
            this.isMsg = isMsg;
            return this;
        }

        public Block create() {
            if (header == null || body == null) {
                return null;
            }
            //tempory support simplied pure block 
            if(this.isMsg){
                Block block = new Block();
                block.header = header;
                block.setIsMsg(true);
                block.parsed = true;
                RLPList items = (RLPList) RLP.decode2(body).get(0);
                RLPList signature = (RLPList) items.get(0);
                byte[] r = signature.get(0).getRLPData();
                byte[] s = signature.get(1).getRLPData();
                block.blockSignature = ECDSASignature.fromComponents(r, s);
                block.option = items.get(1).getRLPData()[0];
                RLPList transactions = (RLPList) items.get(2);
                //RLPList transactions = (RLPList) items.get(0);
                if (transactions.size() == 0){

                } else{
                   block.parseTxs(transactions);
                }
               //delete txState may be stupid....
               //we avoid trie,because we think block header doesn't have large capacity
                return block;
            }else{
                return null;
            }
        }
    }
}

