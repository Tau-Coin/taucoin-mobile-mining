package io.taucoin.db.state;

import io.taucoin.core.AccountState;
import io.taucoin.core.Block;
import io.taucoin.core.Repository;
import io.taucoin.db.BlockStore;
import io.taucoin.db.ByteArrayWrapper;
import io.taucoin.db.file.FileBlockStore;
import io.taucoin.listener.TaucoinListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;

import javax.inject.Inject;
import javax.inject.Singleton;

import static io.taucoin.config.SystemProperties.CONFIG;

@Singleton
public class StateLoader {

    private static final Logger logger = LoggerFactory.getLogger("stateloader");

    private static final String STATES_TAG_DIRECTORY = "states-tag";

    private static final long STATES_BATCH_SIZE = 100L;

    private BlockStore blockStore;

    private Repository repository;

    private FileBlockStore fileBlockStore;

    private TaucoinListener listener;

    private long tagNumber = 0;

    private long accountAmount = 0;
    private long accountsLoaded = 0;

    private static String sTagDir = null;

    private HashMap<ByteArrayWrapper, AccountState> stateBatch
            = new HashMap<ByteArrayWrapper, AccountState>();

    @Inject
    public StateLoader(BlockStore blockStore, Repository repository,
            FileBlockStore fileBlockStore, TaucoinListener listener) {
        this.blockStore = blockStore;
        this.repository = repository;
        this.fileBlockStore = fileBlockStore;
        this.listener = listener;
    }

    public synchronized boolean loadStatesTag() {
        if (!stateTagExist()) {
            logger.error("States tag doesn't exist");
            return false;
        }

        // 1. Set start number for file block store
        setupFileBlockStore();

        // 2. Load latest blocks into block store.

        // First of all, unzip zip archive.
        unzipTagFiles();

        // Load blocks.
        loadBlocks();

        // 3. Load states db into repository.
        loadStates();

        // Lastly, broadcast the event of loading successfully.
        Block bestBlock = blockStore.getBestBlock();
        long bestNumber = bestBlock != null ? bestBlock.getNumber() : 0;

        if ((accountsLoaded == accountAmount)
                && (bestNumber != 0 && bestNumber == tagNumber)) {
            listener.onBlockConnected(bestBlock);
            listener.onStatesLoadedCompleted(tagNumber);
            logger.info("States loaded successfully: {}/{}",
                accountsLoaded, accountAmount);

            return true;
        } else {
            listener.onStatesLoadedFailed(tagNumber);
            logger.error("States loaded error: {}/{}",
                    accountsLoaded, accountAmount);
            return false;
        }
    }

    private void setupFileBlockStore() {
        getTagNumberAndAccountamout();
        logger.info("states tag number: {}, accounts amount: {}", tagNumber, accountAmount);

        fileBlockStore.setStartNumber(tagNumber + 1);
    }

    private void loadBlocks() {
        File f = new File(stateTagDir());

        if (!f.exists() || !f.isDirectory()) {
            return;
        }

        File[] files = f.listFiles();
        if (files == null || files.length == 0) {
            return;
        }

        for (int i = 0; i < files.length; i++) {
            if (files[i] == null || !files[i].isFile()) {
                continue;
            }

            String name = files[i].getName();
            if (name.startsWith("blocks") && !name.endsWith("zip")) {
                loadBlocks(files[i]);

                // Remove blocks file.
                files[i].delete();
                break;
            }
        }
    }

    private void loadBlocks(File in) {
        BufferedReader br = null;
        String strLine;

        try {
            br = new BufferedReader(new FileReader(in));

            while ((strLine = br.readLine()) != null) {
                Block block = new Block(Hex.decode(strLine));

                blockStore.saveBlock(block, block.getCumulativeDifficulty(), true);
                blockStore.flush();
                logger.info("Load block with number {}", block.getNumber());
            }
        } catch (FileNotFoundException e) {
            // This should never happen.
            logger.error("load block fatal err:{} {}", in.getName(), e);
        } catch (IOException e) {
            logger.error("load block fatal err:{} {}", in.getName(), e);
        } finally {
            try {
                if (br != null) {
                    br.close();
                }
            } catch (IOException e) {
                logger.error("load block fatal err:{} {}", in.getName(), e);
            }
        }
    }

    private void loadStates() {
        File f = new File(stateTagDir());

        if (!f.exists() || !f.isDirectory()) {
            return;
        }

        File[] files = f.listFiles();
        if (files == null || files.length == 0) {
            return;
        }

        for (int i = 0; i < files.length; i++) {
            if (files[i] == null || !files[i].isFile()) {
                continue;
            }

            String name = files[i].getName();
            if (name.startsWith("states") && !name.endsWith("zip")) {
                loadStates(files[i]);

                // Remove states tag file.
                files[i].delete();
            }
        }
    }

    private void loadStates(File in) {
        BufferedReader br = null;
        String strLine;

        try {
            br = new BufferedReader(new FileReader(in));

            while ((strLine = br.readLine()) != null) {
                StateRecord state = new StateRecord(strLine);

                if (state.isValid()) {
                    logger.info("state record: {}", state);
                    stateBatch.put(state.getAddress(), state.getAccountState());
                    accountsLoaded++;

                    if (accountsLoaded % STATES_BATCH_SIZE == 0) {
                        flushStates();
                        logger.info("states loading progress {}/{}",
                                accountsLoaded, accountAmount);
                        listener.onStatesLoaded(accountsLoaded, accountAmount);
                    }
                } else {
                    logger.warn("Invalid state record: {}", strLine);
                }
            }

            if (stateBatch.size() > 0) {
                flushStates();
                logger.info("states loading progress {}/{}", accountsLoaded,
                        accountAmount);
                listener.onStatesLoaded(accountsLoaded, accountAmount);
            }
        } catch (FileNotFoundException e) {
            // This should never happen.
            logger.error("load states fatal err:{} {}", in.getName(), e);
        } catch (IOException e) {
            logger.error("load states fatal err:{} {}", in.getName(), e);
        } finally {
            try {
                if (br != null) {
                    br.close();
                }
            } catch (IOException e) {
                logger.error("load states fatal err:{} {}", in.getName(), e);
            }
        }
    }

    private void flushStates() {
        repository.updateBatch(stateBatch);
        repository.flush(tagNumber);
        stateBatch.clear();
    }

    private void getTagNumberAndAccountamout() {
        File f = new File(stateTagDir());

        if (!f.exists() || !f.isDirectory()) {
            return;
        }

        File[] files = f.listFiles();
        if (files == null || files.length == 0) {
            return;
        }

        for (int i = 0; i < files.length; i++) {
            if (files[i] == null || !files[i].isFile()) {
                continue;
            }

            String name = files[i].getName();
            if (tagNumber == 0 && accountAmount == 0
                    && name.startsWith("states") && name.endsWith("zip")) {
                String[] elements = name.split("_");
                if (elements != null && elements.length >= 3) {
                    tagNumber = Long.parseLong(elements[1]);
                    accountAmount = Long.parseLong(elements[2]);
                    break;
                }
            }
        }
    }

    private void unzipTagFiles() {
        File f = new File(stateTagDir());

        if (!f.exists() || !f.isDirectory()) {
            return;
        }

        File[] files = f.listFiles();
        if (files == null || files.length == 0) {
            return;
        }

        for (int i = 0; i < files.length; i++) {
            if (files[i] == null || !files[i].isFile()) {
                continue;
            }

            String name = files[i].getName();
            if (files[i] != null && name.endsWith("zip")) {
                logger.info("unzip file {}, dest dir {}", name, stateTagDir());
                ZipUtils.unzipFile(files[i], stateTagDir(),
                        name.substring(0, name.indexOf(".zip")));
            }
        }

    }

    private void resetChainData() {
    }

    private boolean stateTagExist() {
        File f = new File(stateTagDir());

        if (f.exists()) {
            if (!f.isDirectory()) {
                return false;
            }
            return true;
        }

        return false;
    }

    private static String stateTagDir() {
        if (sTagDir != null) {
            return sTagDir;
        }

        sTagDir = CONFIG.databaseDir() + File.separator + STATES_TAG_DIRECTORY;
        return sTagDir;
    }
}
