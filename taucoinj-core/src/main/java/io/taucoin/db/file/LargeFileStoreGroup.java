package io.taucoin.db.file;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.MappedByteBuffer;
import java.util.*;

/**
 * Manage large file write and read for block data and index data.
 * eg. blk00000.dat, blk00001.dat...
 *
 * This file group supports sequence append operation and random read operation.
 */
class LargeFileStoreGroup {

    private static final Logger logger = LoggerFactory.getLogger("fileblockqueue");

    // Read or write file operation info
    public static class OpFilePosition {
        public int file;
        public int position;
        public int length;

        public OpFilePosition() {
        }

        public OpFilePosition(int file, int position, int length) {
            this.file = file;
            this.position = position;
            this.length = length;
        }

        public String toString() {
           return "OpPosition(" + file + ", " + position + ", " + length + ")";
        }
    }

    // LargeFileStoreGroup maintains a list of files
    private Set<Integer> fileSet = Collections.synchronizedSet(new HashSet<Integer>());

    // When read/write the same file, sync read & write operations.
    Object lock = new Object();

    // file directory
    private String baseDir;
    // file prefix, eg, blk, idx
    private String prefix;
    // file suffix
    private String suffix;
    // If written bytes length is greater than maxFileSize, create the next new file.
    private int maxFileSize;

    // current read file name
    private int readFile;
    // current read file channel
    private FileChannel readFileChan;
    private MappedByteBuffer readMbb;

    // current write file name
    private int writeFile;
    // current write file channel
    private FileChannel writeFileChan;

    public LargeFileStoreGroup(String baseDir, String prefix, String suffix, int maxFile, int maxFileSize)
            throws IllegalArgumentException {
        File dataDir = new File(baseDir);

        if (!dataDir.exists() || !dataDir.isDirectory()) {
                throw new IllegalArgumentException("Data dir not exist or data dir is not directory");
        }
        this.baseDir = baseDir;
        this.prefix  = prefix;
        this.suffix  = suffix;
        this.maxFileSize = maxFileSize;

        // Initialize files set
        for (int i = maxFile; i >= 0; i--) {
            fileSet.add(new Integer(i));
        }

        try {
            initReadWriteFiles(maxFile);
        } catch (Exception e) {
            throw new IllegalArgumentException(e.toString());
        }
    }

    private void initReadWriteFiles(int maxFile) throws Exception {
        String filename = getFileName(maxFile);
        checkFile(filename);

        writeFile = maxFile;
        writeFileChan = new RandomAccessFile(filename, "rw").getChannel();

        readFile = -1;
        readFileChan = null;
        readMbb = null;
    }

    public byte[] read(OpFilePosition filePos) throws Exception {
        if (filePos == null || !fileSet.contains(new Integer(filePos.file))
            || filePos.position < 0 || filePos.length <= 0) {
            throw new IllegalArgumentException("Invalid read file pos:"
                    + (filePos != null ? filePos : "null"));
        }

        synchronized(lock) {
            if (filePos.file == writeFile) {
                logger.debug("Read the writing file " + writeFile);

                // Read through write channel. If this file channel is locked by another thread,
                // blocks until file channel lock is released.
                FileLock fileLock = null;
                try {
                    fileLock = writeFileChan.lock();
                    ByteBuffer bf = ByteBuffer.allocate(filePos.length);
                    int readSize = writeFileChan.read(bf, filePos.position);
                    if (readSize < filePos.length) {
                        throw new IOException("File has no enough bytes");
                    }

                    return bf.array();
                } catch(IOException e) {
                    logger.error("Read exception: {}", e);
                    throw e;
                } finally {
                    if (fileLock != null) {
                        fileLock.release();
                    }
                }
            } else if (filePos.file == readFile) {
                // In this condition, read channel is ready.
                logger.debug("Read the existed file channel " + readFile);

                // Read through read channel. If this file channel is locked by another thread,
                // blocks until file channel lock is released.
                FileLock fileLock = null;
                try {
                    fileLock = readFileChan.lock();
                    byte[] buffer = new byte[filePos.length];
                    readMbb.position(filePos.position);
                    readMbb.get(buffer, 0, filePos.length);
            
                    return buffer;
                } catch(IOException e) {
                    logger.error("Read exception: {}", e);
                    throw e;
                } finally {
                    if (fileLock != null) {
                        fileLock.release();
                    }
                }
            } else {
                // In this condition, read channel and read mapped buffer has not been inited.
                // Or the current readfile is not the same as filePos.file.
                // Close the current file channel and switch the resquested one.
                if (readFileChan != null && readMbb != null) {
                    try {
                        readFileChan.close(); 
                    } catch(IOException e) {
                        logger.error("Read exception: {}", e);
                        throw e;
                    }
                }

                logger.info("Switch to the target file " + filePos.file);
                String filename = getFileName(filePos.file);
                readFileChan = new RandomAccessFile(filename, "rw").getChannel();
                readMbb = readFileChan.map(FileChannel.MapMode.READ_ONLY, 0, readFileChan.size());
                readFile = filePos.file;

                // Read through read channel. If this file channel is locked by another thread,
                // blocks until file channel lock is released.
                FileLock fileLock = null;
                try {
                    fileLock = readFileChan.lock();
                    byte[] buffer = new byte[filePos.length];
                    readMbb.position(filePos.position);
                    readMbb.get(buffer, 0, filePos.length);
            
                    return buffer;
                } catch(IOException e) {
                    logger.error("Read exception: {}", e);
                    throw e;
                } finally {
                    if (fileLock != null) {
                        fileLock.release();
                    }
                }
            }
        }
    }

    public boolean contains(OpFilePosition filePos) throws Exception {
        if (filePos == null || !fileSet.contains(new Integer(filePos.file))
            || filePos.position < 0 || filePos.length <= 0) {
            return false;
        }

        synchronized(lock) {
            if (filePos.file < writeFile
                    && filePos.position + filePos.length <= maxFileSize) {
                return true;
            }

            FileLock fileLock = null;
            try {
                fileLock = writeFileChan.lock();
                if ((long)(filePos.position + filePos.length) <= writeFileChan.size()) {
                    return true;
                }
            } catch(IOException e) {
                logger.error("Read exception: {}", e);
                return false;
            } finally {
                if (fileLock != null) {
                    fileLock.release();
                }
            }
        }

        return false;
    }

    public OpFilePosition write(byte[] bytes) throws Exception {
        if (bytes == null || bytes.length == 0) {
            throw new IllegalArgumentException("Invalid write arguments");
        }

        synchronized(lock) {
            findWritePosition(bytes.length);

            OpFilePosition position = new OpFilePosition();
            position.file = writeFile;
            position.position = (int)writeFileChan.size();
            position.length = bytes.length;

            ByteBuffer bf = ByteBuffer.wrap(bytes);
            FileLock fileLock = null;
            try {
                fileLock = writeFileChan.lock();
                writeFileChan.position(writeFileChan.size());
                writeFileChan.write(bf);
                writeFileChan.force(true);
            } catch (IOException e) {
                logger.error("Write exception: {}", e);
                throw e;
            } finally {
                if (fileLock != null) {
                    fileLock.release();
                }
            }

            return position;
        }
    }

    private void findWritePosition(int length) throws Exception {
        logger.debug("now size:" + writeFileChan.size() + ", written:" + length
            + ", max:" + maxFileSize);

        if (writeFileChan.size() + length > maxFileSize) {
            // Flush the current write file.
            FileLock fileLock = null;
            try {
                fileLock = writeFileChan.lock();
                writeFileChan.force(true);
                fileLock.release();
                writeFileChan.close();
            } catch (IOException e) {
                logger.error("IO exception: {}", e);
                throw e;
            }

            // Create the next new file.
            String newFileName = getFileName(writeFile + 1);
            checkFile(newFileName);
            writeFileChan = new RandomAccessFile(newFileName, "rw").getChannel();
            writeFile = writeFile + 1;
            fileSet.add(new Integer(writeFile));
        }
    }

    public void switchToNextWriteFile() throws Exception {
        synchronized(lock) {
            FileLock fileLock = null;
            try {
                fileLock = writeFileChan.lock();
                writeFileChan.force(true);
                fileLock.release();
                writeFileChan.close();
            } catch (IOException e) {
                logger.error("IO exception: {}", e);
                throw e;
            }

            // Switch to the next file
            writeFile = writeFile + 1;
            String nextFileName = getFileName(writeFile);
            writeFileChan = new RandomAccessFile(nextFileName, "rw").getChannel();
            if (!fileSet.contains(new Integer(writeFile))) {
                fileSet.add(new Integer(writeFile));
            }
        }
    }

    // Note: just truncate from now write file.
    public void rollback(OpFilePosition filePos) throws Exception {
        if (filePos == null || !fileSet.contains(new Integer(filePos.file))
            || filePos.position < 0 || filePos.length <= 0
            || filePos.file != writeFile) {
            throw new IllegalArgumentException("Invalid truncate file pos");
        }

        synchronized(lock) {
            // In this condition, rollback data must be the end of the writefile.
            int writeFileSize = (int)writeFileChan.size();
            if (filePos.position + filePos.length != writeFileSize) {
                throw new IllegalArgumentException("Invalid truncate file pos");
            }

            FileLock fileLock = null;
            try {
                fileLock = writeFileChan.lock();
                writeFileChan.truncate((long)filePos.position);
                writeFileChan.force(true);
                fileLock.release();
            } catch (IOException e) {
                logger.error("IO exception: {}", e);
                throw e;
            }

            if (writeFileChan.size() == 0 && writeFile != 0) {
                // Remove the old empty file
                writeFileChan.close();
                String filename = getFileName(writeFile);
                File file = new File(filename);
                file.delete();
                fileSet.remove(new Integer(writeFile));

                // Switch to the previous file
                writeFile = writeFile - 1;
                String preFileName = getFileName(writeFile);
                writeFileChan = new RandomAccessFile(preFileName, "rw").getChannel();
            }
        }
    }

    // Roll back to OpFilePosition filePos and the data behind 'filePos'
    // will be deleted.
    public void rollbackTo(OpFilePosition filePos) throws Exception {
        if (filePos == null || !fileSet.contains(new Integer(filePos.file))
            || filePos.position < 0) {
            throw new IllegalArgumentException("Invalid truncate file pos");
        }

        logger.warn("Roll back to the file {}", filePos);

        int maxFile = writeFile;

        synchronized(lock) {
            // First of all, close read channel.
            if (readFileChan != null && readMbb != null) {
                try {
                    readFileChan.close();
                } catch(IOException e) {
                    logger.error("Close read file exception: {}", e);
                    throw e;
                }
            }

            // Choose which files should be removed.
            if (filePos.file == writeFile && writeFileChan != null) {
                FileLock fileLock = null;
                try {
                    fileLock = writeFileChan.lock();
                    writeFileChan.truncate((long)(filePos.position + filePos.length));
                    writeFileChan.force(true);
                    fileLock.release();
                } catch (IOException e) {
                    logger.error("IO exception: {}", e);
                    throw e;
                }

                writeFileChan.close();
            } else if (filePos.file < writeFile && writeFileChan != null) {
                writeFileChan.close();

                // Remove files [filePos.file + 1, writeFile]
                for (int index = filePos.file + 1; index <= writeFile; index++) {
                    String filename = getFileName(index);
                    File file = new File(filename);
                    file.delete();
                    fileSet.remove(new Integer(index));
                }

                // Truncate filePos.file
                FileChannel targetFileChan;
                String targetFilename = getFileName(filePos.file);
                FileLock fileLock = null;
                try {
                    targetFileChan = new RandomAccessFile(targetFilename, "rw").getChannel();
                    fileLock = targetFileChan.lock();
                    targetFileChan.truncate((long)(filePos.position + filePos.length));
                    targetFileChan.force(true);
                    fileLock.release();
                    targetFileChan.close();
                } catch (IOException e) {
                    logger.error("IO exception: {}", e);
                    throw e;
                }

                // Set max file index.
                maxFile = filePos.file;
            }

            // Lastly, init everything again.
            initReadWriteFiles(maxFile);
        }
    }

    public void close() {
        synchronized(lock) {
            try {
                if (writeFileChan != null) {
                    writeFileChan.force(true);
                    writeFileChan.close();
                    writeFileChan = null;
                }

                if (readFile != writeFile && readFileChan != null) {
                    readFileChan.close();
                    readFileChan = null;
                }

                readFile = writeFile = -1;
            } catch (IOException e) {
                logger.error("Close exception: {}", e);
            }
        }
    }

    public int getReadFile() {
        synchronized(lock) {
            return readFile;
        }
    }

    public int getWriteFile() {
        synchronized(lock) {
            return writeFile;
        }
    }

    public long getWriteFileSize() throws Exception {
        synchronized(lock) {
            FileLock fileLock = null;
            try {
                fileLock = writeFileChan.lock();
                return writeFileChan.size();
            } catch(IOException e) {
                logger.error("Read exception: {}", e);
                return -1;
            } finally {
                if (fileLock != null) {
                    fileLock.release();
                }
            }
        }
    }

    public Set<Integer> getFileList() {
        synchronized(lock) {
            return fileSet;
        }
    }

    // If not exist, create file
    private static void checkFile(String file) throws Exception {
        File f = new File(file);

        try {
            if (!f.exists()) {
               f.createNewFile();
            }
        } catch (Exception e) {
            throw e;
        }
    }

    private String getFileName(int file) {
        String filename = prefix + String.format("%05d", file) + "." + suffix;
        return baseDir + "/" + filename;
    }

    public static void main(String[] args) {

        try {
            LargeFileStoreGroup fileGroup = new LargeFileStoreGroup(
                "/home/tester/taumobile", "blk", ".dat", 0, 10);

            // Write test
            byte[] bytes = new byte[6];
            bytes[0] = (byte)0x00;
            bytes[1] = (byte)0x01;
            bytes[2] = (byte)0x02;
            bytes[3] = (byte)0x03;
            bytes[4] = (byte)0x04;
            bytes[5] = (byte)0x05;

            LargeFileStoreGroup.OpFilePosition pos1 = fileGroup.write(bytes);
            logger.info(pos1.toString());

            byte[] byte2 = fileGroup.read(pos1);
            logger.info("Read content:");
            for (int i = 0; i < byte2.length; ++i) {
                logger.info("[" + i + "]=" + byte2[i]);
            }

            LargeFileStoreGroup.OpFilePosition pos2 = fileGroup.write(bytes);
            logger.info(pos2.toString());

            fileGroup.rollback(pos2);
            fileGroup.rollback(pos1);

            fileGroup.switchToNextWriteFile();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
