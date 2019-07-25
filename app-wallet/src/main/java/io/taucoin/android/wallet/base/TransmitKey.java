/**
 * Copyright 2018 Taucoin Core Developers.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.taucoin.android.wallet.base;

public class TransmitKey {
    public static final String SERVICE_TYPE = "service_type";

    public static class ServiceType {
        public static final String GET_HOME_DATA = "get_home_data";
        public static final String GET_SEND_DATA = "get_send_data";
        public static final String GET_BALANCE = "get_balance";
        public static final String GET_RAW_TX = "get_raw_tx";
        public static final String GET_INFO = "get_info";
        public static final String GET_BLOCK_HEIGHT = "get_block_height";
        public static final String GET_REWARD_INFO = "get_reward_info";
    }

    public static final String PUBLIC_KEY = "PubKey";
    public static final String ADDRESS = "Address";
    public static final String RAW_ADDRESS = "RawAddress";

    public static final String TITLE = "title";
    public static final String URL = "url";

    public static final int PAGE_SIZE = 15;
    public static final long MIN_TRANS_EXPIRY = 5;
    public static final long MAX_TRANS_EXPIRY = 720;

    public static class TxResult {
        public static final String FAILED = "Failed";
        public static final String SUCCESSFUL = "Successful";
        public static final String CONFIRMING = "Confirming";
        public static final String BROADCASTING = "Broadcasting";
    }

    public static class MiningState {
        public static final String Start = "Start";
        public static final String Stop = "Stop";
        public static final String LOADING = "Loading";
        public static final String ERROR = "Error";
    }

    public static final String BEAN = "bean";
    public static final String UPGRADE = "upgrade";
    public static final String ISSHOWTIP = "isShowTip";
    public static final String ID = "id";

    public static final String RESULT = "result";
    public static class RemoteResult {
        public static final String OK = "OK";
        public static final String FAIL = "Fail";
        public static final String HEIGHT = "height";
        public static final String BLOCK = "block";
        public static final String TRANSACTION = "transaction";
        public static final String IS_FINISH = "isfinish";

    }
    public static final String TYPE = "type";

    public static class ExternalUrl{
        public static final String HOW_IMPORT_KEY_URL = "http://tau.taucoin.io/static/html/help3.html";
        public static final String MINER_HISTORY = "http://tau.taucoin.io/minerblocks/?address=";
        public static final String PARTICIPANT_HISTORY = "http://tau.taucoin.io/minertxes/?address=";
        public static final String TAU_EXPLORER_TX_URL = "http://tau.taucoin.io/tx/";
        public static final String TAU_EXPLORER_SEE_MORE = "http://tau.taucoin.io/address/";
        public static final String P2P_EXCHANGE = "https://www.taucoin.io/p2pexchange";
        public static final String BLOCKS_INFO = "http://tau.taucoin.io/blocksInfo/";
        public static final String MINING_INFO = "http://tau.taucoin.io/miningInfo?address=";
        public static final String MINING_GROUP = "https://t.me/joinchat/Ifw8BhBICAMUVCXrWbq4OQ";
    }

    public static final String FORGING_WIFI_ONLY = "forging_wifi_only";
    public static final String FORGING_RELOAD = "FORGING_RELOAD";
}