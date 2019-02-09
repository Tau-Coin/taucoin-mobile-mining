package io.taucoin.android.rpc.server.full;

import com.thetransactioncompany.jsonrpc2.*;
import com.thetransactioncompany.jsonrpc2.server.*;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;

//import io.taucoin.android.db.BlockTransactionVO;
//import io.taucoin.android.db.OrmLiteBlockStoreDatabase;
import io.taucoin.android.rpc.server.full.JsonRpcServer;
import io.taucoin.core.Account;
import io.taucoin.core.AccountState;
import io.taucoin.core.Block;
import io.taucoin.core.BlockHeader;
import io.taucoin.core.Blockchain;
import io.taucoin.core.Transaction;
import io.taucoin.core.transaction.TransactionVersion;
import io.taucoin.core.transaction.TransactionOptions;
import io.taucoin.crypto.ECKey;
import io.taucoin.crypto.HashUtil;
import io.taucoin.facade.Taucoin;
import io.taucoin.util.ByteUtil;
import io.taucoin.util.RLP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

import static io.taucoin.core.Denomination.SZABO;

public abstract class JsonRpcServerMethod implements RequestHandler {

    private static final Logger logger = LoggerFactory.getLogger("rpc");

    private String name = "";
    protected Taucoin taucoin;

    public JsonRpcServerMethod(Taucoin taucoin) {
        this.taucoin = taucoin;
        name = this.getClass().getSimpleName();
    }

    public String[] handledRequests() {
        return new String[]{name};
    }

    public JSONRPC2Response process(JSONRPC2Request req, MessageContext ctx) {
        if (req.getMethod().equals(name)) {
            return worker(req, ctx);
        } else {
            return new JSONRPC2Response(JSONRPC2Error.METHOD_NOT_FOUND, req.getID());
        }
    }

    protected abstract JSONRPC2Response worker(JSONRPC2Request req, MessageContext ctx);

    protected long getBlockNumber(String height) {
        long blockNumber = 0;
        switch (height) {
            case "earliest":
                blockNumber = 0;
                break;
            case "latest":
                blockNumber = -1;
                break;
            case "pending":
                blockNumber = -2;
                break;
            default:
                blockNumber = jsToLong(height);
        }
        if (blockNumber >= 0)
            blockNumber = -1;
        return blockNumber;
    }

    protected String clearJSString(String data) {
        if (data.substring(0, 2).equals("0x"))
            return data.substring(2);
        return data;
    }

    protected byte[] jsToAddress(String data) {
        return Hex.decode(clearJSString(data));
    }

    protected byte[] jsToByteArray(String data) {
        return Hex.decode(clearJSString(data));
    }

    protected int jsToInt(String data) {
        return Integer.parseInt(clearJSString(data), 16);
    }

    protected long jsToLong(String data) {
        return Long.parseLong(clearJSString(data), 16);
    }

    protected BigInteger jsToBigInteger(String data) {
        return new BigInteger(clearJSString(data), 16);
    }

    protected BigInteger jsToBigIntegerDecimal(String data) {
        return new BigInteger(data, 10);
    }

    /**
     * Create transaction by rpc method parameters.
     * {
     *     "to" : <toAddress>,
     *     "value": <value>,
     *     "fee": <tx fee>,
     *     "privkey": <sender private key>
     * }
     */
    protected Transaction jsToTransaction(JSONObject obj) throws Exception {
        logger.info("json to tx: {}", obj);

        if ((!obj.containsKey("to") || ((String)obj.get("to")).equals(""))   || (!obj.containsKey("value") || ((long)obj.get("value")) <= 0)
                || (!obj.containsKey("privkey") || ((String)obj.get("privkey")).equals(""))) {
            throw new Exception("Invalid params");
        }

        byte[] to = null;
        if (obj.containsKey("to") && !((String)obj.get("to")).equals("")) {
            to = jsToAddress((String) obj.get("to"));
        }

        BigInteger value = BigInteger.ZERO;
        if (obj.containsKey("value") && ((long)obj.get("value")) > 0) {
            value = BigInteger.valueOf((long) obj.get("value"));
        }

        BigInteger fee = BigInteger.ZERO;
        if (obj.containsKey("fee") && ((long)obj.get("fee")) > 0) {
            fee = BigInteger.valueOf((long) obj.get("fee"));
        }

        byte[] senderPrivkey = null;
        if (obj.containsKey("privkey") && !((String)obj.get("privkey")).equals("")) {
            senderPrivkey = jsToByteArray((String) obj.get("privkey"));
        }

        // Check account balance
        byte[] accountAddress = ECKey.fromPrivate(senderPrivkey).getAddress();
        BigInteger balance = this.taucoin.getRepository().getBalance(accountAddress);
        logger.info("Sender address: {}, balance: {}", Hex.toHexString(accountAddress), balance);
        if (value.add(fee).compareTo(balance) > 0) {
            logger.error("Not enough balance");
            throw new Exception("Not enough balance");
        }

        long timeStamp = System.currentTimeMillis() / 1000;

        Transaction tx = taucoin.createTransaction(TransactionVersion.V01.getCode(),
                TransactionOptions.TRANSACTION_OPTION_DEFAULT, ByteUtil.longToBytes(timeStamp),
                to, value.toByteArray(), fee.toByteArray());

        tx.sign(senderPrivkey);

        return tx;
    }


    protected JSONObject blockToJS (Block block, Boolean detailed) {
        JSONObject res = new JSONObject();
        if (block == null)
            return null;

        res.put("number", "0x" + Long.toHexString(block.getNumber()));

        res.put("hash", "0x" + Hex.toHexString(block.getHash()));

        res.put("parentHash", "0x" + Hex.toHexString(block.getPreviousHeaderHash()));

        res.put("totalDifficulty", "0x" + block.getCumulativeDifficulty().toString(16));

        // No way to get size of block in bytes, so I try calculate it using formula from  getEncoded
        byte[] header = block.getHeader().getEncoded();
        byte[] transactions = RLP.encodeList();

        JSONArray transactionsJA = new JSONArray();
        int i = 0;
        for (Transaction transaction : block.getTransactionsList()) {
            if (detailed) {
                JSONObject tx = transactionToJS(block, transaction);
                tx.put("transactionIndex", "0x" + Integer.toHexString(i));
                tx.put("blockHash", "0x" + Hex.toHexString(block.getHash()));
                tx.put("blockNumber", "0x" + Long.toHexString(block.getNumber()));
                transactionsJA.add(tx);
            } else {
                transactionsJA.add("0x" + Hex.toHexString(transaction.getHash()));
            }
            ++i;
        }
        res.put("transactions", transactionsJA);

        return res;
    }

    protected JSONObject transactionToJS (Block block, Transaction transaction) {
        JSONObject res = new JSONObject();

        res.put("hash", "0x" + Hex.toHexString(transaction.getHash()));

        res.put("from", "0x" + Hex.toHexString(transaction.getSender()));

        res.put("to", "0x" + Hex.toHexString(transaction.getReceiveAddress()));

        /*
        if (block == null) {
            OrmLiteBlockStoreDatabase db = OrmLiteBlockStoreDatabase.getHelper(null);
            BlockTransactionVO relation = db.getTransactionLocation(transaction.getHash());
            if (relation == null) {
                res.put("transactionIndex", null);
                res.put("blockHash", null);
                res.put("blockNumber", null);
            } else {
                block = taucoin.getBlockchain().getBlockByHash(relation.getBlockHash());
            }
        }
        if (block != null) {
            long txi = 0;
            for (Transaction tx : block.getTransactionsList()) {
                if (Arrays.equals(tx.getHash(), transaction.getHash()))
                    break;
                txi++;
            }
            res.put("transactionIndex", "0x" + Long.toHexString(txi));
            res.put("blockHash", "0x" + Hex.toHexString(block.getHash()));
            res.put("blockNumber", "0x" + Long.toHexString(block.getNumber()));
        }
        */

        return res;
    }

    protected byte[] getCoinBase() {
        return ((Account) taucoin.getWallet().getAccountCollection().toArray()[0]).getEcKey().getAddress();
    }
}
