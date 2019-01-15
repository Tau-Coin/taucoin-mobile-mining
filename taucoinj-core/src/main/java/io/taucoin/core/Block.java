package io.taucoin.core;

import org.ethereum.crypto.HashUtil;
import org.ethereum.crypto.SHA3Helper;
import org.ethereum.trie.Trie;
import org.ethereum.trie.TrieImpl;
import io.taucoin.core.BlockHeader;
import org.ethereum.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.Arrays;
import org.spongycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.ethereum.config.SystemProperties.CONFIG;

/**
 * The block in taucoin is the collection of relevant pieces of information
 * (known as the blockheader), H, together with information corresponding to
 * the comprised transactions, T.
 */
public class Block {

    private static final Logger logger = LoggerFactory.getLogger("block");

    private BlockHeader header;

    /*ensure the integrity of the block 512 bits*/
    private byte[] blockSignature;
    /*this is left for future use 8 bits*/
    private byte option;
    /* A scalar value equal to the number of ancestor blocks.
     * The genesis block has a number of zero */
    private long number;
    private BigInteger baseTarget; //this is uint64 type so here we should use compact type
    private BigInteger cumulativeDifficulty; //this is total chain difficulty

    /* Transactions */
    private List<Transaction> transactionsList = new CopyOnWriteArrayList<>();

    protected byte[] rlpEncoded;
    private boolean isMsg = false;
    private boolean parsed = false;

    /* Constructors */

    private Block() {
    }

    public Block(byte[] rawData) {
        logger.debug("new from [" + Hex.toHexString(rawData) + "]");
        this.rlpEncoded = rawData;
    }

    public Block(byte[] rawData, boolean isMsg) {
        logger.debug("new from net [" + Hex.toHexString(rawData) + "]");
        this.rlpEncoded = rawData;
        this.isMsg = isMsg;
    }

    public Block(BlockHeader header, byte[] blockSignature,byte option,List<Transaction> transactionsList) {

        this(header.getVersion(),
                header.getTimeStamp(),
                header.getPreviousHeaderHash(),
                header.getGeneratorPublicKey(),
                blockSignature,
                option,
                transactionsList);
    }

    public Block(byte version, byte[] timestamp, byte[] previousHeaderHash, byte[] generatorPublickey,
                 byte[] blockSignature,byte option,
                 List<Transaction> transactionsList) {
        /*
        * TODO: calculate GenerationSignature
        *
         */
        this.header = new BlockHeader(version, timestamp, previousHeaderHash, generatorPublickey);

        this.transactionsList = transactionsList;
        if (this.transactionsList == null) {
            this.transactionsList = new CopyOnWriteArrayList<>();
        }

        this.parsed = true;
    }


    private void parseRLP() {

        RLPList params = RLP.decode2(rlpEncoded);
        RLPList block = (RLPList) params.get(0);

        // Parse Header
        RLPList header = (RLPList) block.get(0);
        this.header = new BlockHeader(header);
        if (isMsg) {
            byte[] nrBytes = block.get(1).getRLPData();
            this.number = nrBytes == null ? 0 : (new BigInteger(1, nrBytes)).longValue();

            byte[] btBytes = block.get(2).getRLPData();
            this.baseTarget = new BigInteger(1, btBytes);

            byte[] cyBytes = block.get(3).getRLPData();
            this.cumulativeDifficulty = new BigInteger(1, cyBytes);

            // Parse blockSignature
            this.blockSignature = block.get(4).getRLPData();
            // Parse option
            this.option = block.get(5).getRLPData()[0];
            // Parse Transactions
            RLPList txTransactions = (RLPList) block.get(6);
            //this.parseTxs(this.header.getTxTrieRoot(), txTransactions);
        } else {
            // Parse blockSignature
            this.blockSignature = block.get(1).getRLPData();
            // Parse option
            this.option = block.get(2).getRLPData()[0];
            // Parse Transactions
            RLPList txTransactions = (RLPList) block.get(3);
            //this.parseTxs(this.header.getTxTrieRoot(), txTransactions);
        }

        this.parsed = true;
    }

    public BlockHeader getHeader() {
        if (!parsed) parseRLP();
        return this.header;
    }

    public byte[] getHash() {
        if (!parsed) parseRLP();
        //temporary used header hash
        return this.header.getHeaderHash();
    }

    public byte[] getPreviousHeaderHash() {
        if (!parsed) parseRLP();
        return this.header.getPreviousHeaderHash();
    }

    public byte[] getTimestamp() {
        if (!parsed) parseRLP();
        return this.header.getTimeStamp();
    }

    public byte getVersion() {
        if (!parsed) parseRLP();
        return this.header.getVersion();
    }

    public byte[] getblockSignature(){
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
        return number;
    }

    public void setBaseTarget(BigInteger baseTarget) {
        this.baseTarget = baseTarget;
    }

    public BigInteger getBaseTarget() {
        return baseTarget;
    }

    public void setCumulativeDifficulty(BigInteger cumulativeDifficulty) {
        this.cumulativeDifficulty = cumulativeDifficulty;
    }

    public BigInteger getCumulativeDifficulty() {
        return cumulativeDifficulty;
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
        toStringBuff.append(Hex.toHexString(this.getEncoded())).append("\n");
        toStringBuff.append("BlockData [ ");
        toStringBuff.append("hash=" + ByteUtil.toHexString(this.getHash())).append("\n");
        toStringBuff.append(header.toString());
        toStringBuff.append("blocksig=" + ByteUtil.toHexString(this.blockSignature)).append("\n");
        //toStringBuff.append("option=" + ByteUtil.toHexString(this.option)).append("\n");
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
        toStringBuff.append("blocksig=" + ByteUtil.toHexString(this.blockSignature));
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

    private byte[] getSigAndOptionEncoded() {
        byte[] blockSig = RLP.encodeElement(this.blockSignature);
        byte[] option = RLP.encodeByte(this.option);
        return RLP.encodeList(blockSig,option);
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
            block.add(0, header);
            byte[][] elements = block.toArray(new byte[block.size()][]);

            this.rlpEncoded = RLP.encodeList(elements);
        }
        return rlpEncoded;
    }

    //encode block on net
    public byte[] getEncodedMsg() {
        if (rlpEncoded == null) {
            byte[] header = this.header.getEncoded();

            List<byte[]> block = getBodyElements();
            block.add(0, header);
            byte[][] elements = block.toArray(new byte[block.size()][]);

            this.rlpEncoded = RLP.encodeList(elements);
        }
        return rlpEncoded;
    }


    public byte[] getEncodedBody() {
        List<byte[]> body = getBodyElements();
        byte[][] elements = body.toArray(new byte[body.size()][]);
        return RLP.encodeList(elements);
    }

    private List<byte[]> getBodyElements() {
        if (!parsed) parseRLP();

        byte[] sigAndOption = getSigAndOptionEncoded();
        byte[] transactions = getTransactionsEncoded();

        List<byte[]> body = new ArrayList<>();
        body.add(sigAndOption);
        body.add(transactions);

        return body;
    }

    private List<byte[]> getFullBodyElements() {
        if (!parsed) parseRLP();

        byte[] number = RLP.encodeBigInteger(BigInteger.valueOf(this.number));
        byte[] baseTarget = RLP.encodeBigInteger(this.baseTarget);
        byte[] cumulativeDifficulty = RLP.encodeBigInteger(this.cumulativeDifficulty);
        byte[] sigAndOption = getSigAndOptionEncoded();
        byte[] transactions = getTransactionsEncoded();

        List<byte[]> body = new ArrayList<>();
        body.add(number);
        body.add(baseTarget);
        body.add(cumulativeDifficulty);
        body.add(sigAndOption);
        body.add(transactions);

        return body;
    }

    public String getShortHash() {
        if (!parsed) parseRLP();
        return Hex.toHexString(getHash()).substring(0, 6);
    }

    public static class Builder {

        private BlockHeader header;
        private byte[] body;

        public Builder withHeader(BlockHeader header) {
            this.header = header;
            return this;
        }

        public Builder withBody(byte[] body) {
            this.body = body;
            return this;
        }

        public Block create() {
            if (header == null || body == null) {
                return null;
            }

            Block block = new Block();
            block.header = header;
            block.parsed = true;
            block.blockSignature = RLP.decode2(body).get(0).getRLPData();
            block.option = RLP.decode2(body).get(1).getRLPData()[0];
            RLPList transactions = (RLPList) RLP.decode2(body).get(2);
            //RLPList transactions = (RLPList) items.get(0);

//            if (!block.parseTxs(header.getTxTrieRoot(), transactions)) {
//                return null;
//            }
            //TODO:decodeRLPList---->List<Transactions>
            //we avoid trie,because we think block header doesn't have large capacity

            return block;
        }
    }
}

