package io.taucoin.db.state;

import io.taucoin.core.AccountState;
import io.taucoin.db.ByteArrayWrapper;

import org.spongycastle.util.encoders.Hex;

/**
 States tag file record format:
   Hex(address):Hex(RLP(AccountState))
 */
public class StateRecord {

    private ByteArrayWrapper address;
    private AccountState state;

    public StateRecord(String recordStr) {
        if (recordStr != null && !recordStr.isEmpty()) {
            String[] elements = recordStr.split(":");
            if (elements != null && elements.length == 2) {
                address = new ByteArrayWrapper(Hex.decode(elements[0]));
                state = new AccountState(Hex.decode(elements[1]));
            }
        }
    }

    public ByteArrayWrapper getAddress() {
        return address;
    }

    public AccountState getAccountState() {
        return state;
    }

    public boolean isValid() {
        return address != null && state != null;
    }
}
