package io.taucoin.db.file;

import io.taucoin.core.Utils;
import io.taucoin.db.file.LargeFileStoreGroup.OpFilePosition;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import static io.taucoin.config.SystemProperties.CONFIG;

/**
 * BlockIndex wraps the disk position(file, offset, length) where the block data store.
 */
class BlockIndex {

    private static final Logger logger = LoggerFactory.getLogger("fileblockqueue");

    // Sizeof(file) + Sizeof(position) + Sizeof(length)
    public static final int ENCODED_SIZE = 12;

    private int file;
    private int position;
    private int length;

    public BlockIndex(int file, int position, int length) {
        this.file = file;
        this.position = position;
        this.length = length;
    }

    public BlockIndex(OpFilePosition filePosition) {
        this.file = filePosition.file;
        this.position = filePosition.position;
        this.length = filePosition.length;
    }

    public BlockIndex(byte[] bytes) {
        parse(bytes);
    }

    public static BlockIndex withBlockNumber(long blockNumber) {
        long file = blockNumber / CONFIG.indexStoreFileMetaMaxAmount();
        long position = (blockNumber % CONFIG.indexStoreFileMetaMaxAmount() - 1)
                * (long)ENCODED_SIZE;

        return new BlockIndex((int)file, (int)position, ENCODED_SIZE);
    }

    public OpFilePosition getOpFilePosition() {
        return new OpFilePosition(this.file, this.position, this.length);
    }

    public byte[] getEncoded() {
        byte[] encoded = new byte[ENCODED_SIZE];

        Utils.uint32ToByteArrayBE((long)file, encoded, 0);
        Utils.uint32ToByteArrayBE((long)position, encoded, 4);
        Utils.uint32ToByteArrayBE((long)length, encoded, 8);

        return encoded;
    }

    private void parse(byte[] bytes) {
        if (bytes == null || bytes.length < ENCODED_SIZE) {
            throw new RuntimeException("BlockIndex bytes error :"
                    + (bytes == null ? "null" : Hex.toHexString(bytes)));
        }

        this.file = (int)Utils.readUint32BE(bytes, 0);
        this.position = (int)Utils.readUint32BE(bytes, 4);
        this.length = (int)Utils.readUint32BE(bytes, 8);

        assert isValid();
    }

    public void setFile(int file) {
        this.file = file;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public int getFile() {
        return this.file;
    }

    public int getPosition() {
        return this.position;
    }

    public int getLength() {
        return this.length;
    }

    public boolean isValid() {
        if (this.file >= 0 && this.position >= 0 && this.length > 0) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        return "BlockIndex(" + this.file + ", "
                + this.position + ", " + this.length + ")";
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof BlockIndex) {
            BlockIndex index = (BlockIndex)obj;
            return this.file == index.getFile()
                    && this.position == index.getPosition()
                    && this.length == index.getLength();
        }
        return false;
    }
}
