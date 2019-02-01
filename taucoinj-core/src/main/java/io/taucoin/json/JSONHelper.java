package io.taucoin.json;

import io.taucoin.core.AccountState;
import io.taucoin.core.Block;
import io.taucoin.db.ByteArrayWrapper;
import io.taucoin.core.Repository;
import io.taucoin.util.ByteUtil;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.spongycastle.util.encoders.Hex;

import java.math.BigInteger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * JSON Helper class to format data into ObjectNodes
 * to match PyEthereum blockstate output
 *
 *  Dump format:
 *  {
 *      "address":
 *      {
 *          "nonce": "n1",
 *          "balance": "b1",
 *          "stateRoot": "s1",
 *          "codeHash": "c1",
 *          "code": "c2",
 *          "storage":
 *          {
 *              "key1": "value1",
 *              "key2": "value2"
 *          }
 *      }
 *  }
 *
 * @author Roman Mandeleil
 * @since 26.06.2014
 */
public class JSONHelper {

    @SuppressWarnings("uncheked")
    public static void dumpState(ObjectNode statesNode, String address, AccountState state) {


        ObjectNode account = statesNode.objectNode();
        ObjectNode storage = statesNode.objectNode();

        account.put("balance", state.getBalance() == null ? "0" : state.getBalance().toString());
//        account.put("codeHash", details.getCodeHash() == null ? "0x" : "0x" + Hex.toHexString(details.getCodeHash()));
        account.put("nonce", state.getforgePower() == null ? "0" : state.getforgePower().toString());
        account.set("storage", storage);
        //account.put("storage_root", state.getStateRoot() == null ? "" : Hex.toHexString(state.getStateRoot()));

        statesNode.set(address, account);
    }

    public static void dumpBlock(ObjectNode blockNode, Block block,
                                 long gasUsed, byte[] state, List<ByteArrayWrapper> keys,
                                 Repository repository) {

        //blockNode.put("coinbase", Hex.toHexString(block));
        blockNode.put("difficulty", new BigInteger(1, block.getCumulativeDifficulty().toByteArray()).toString());
        blockNode.put("extra_data", "0x");
        blockNode.put("gas_used", String.valueOf(gasUsed));
        //blockNode.put("nonce", "0x" + Hex.toHexString(block.getNonce()));
        blockNode.put("number", String.valueOf(block.getNumber()));
        blockNode.put("prevhash", "0x" + Hex.toHexString(block.getPreviousHeaderHash()));

        ObjectNode statesNode = blockNode.objectNode();
        for (ByteArrayWrapper key : keys) {
            byte[] keyBytes = key.getData();
            AccountState accountState = repository.getAccountState(keyBytes);
            //ContractDetails details = repository.getContractDetails(keyBytes);
            dumpState(statesNode, Hex.toHexString(keyBytes), accountState);
        }
        blockNode.set("state", statesNode);

        blockNode.put("state_root", Hex.toHexString(state));
        blockNode.put("timestamp", String.valueOf(block.getTimestamp()));

        ArrayNode transactionsNode = blockNode.arrayNode();
        blockNode.set("transactions", transactionsNode);

        //blockNode.put("tx_list_root", ByteUtil.toHexString(block.getTxTrieRoot()));
        //blockNode.put("uncles_hash", "0x" + Hex.toHexString(block.getUnclesHash()));

//      JSONHelper.dumpTransactions(blockNode,
//              stateRoot, codeHash, code, storage);
    }

}
