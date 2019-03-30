package io.taucoin.http;

import io.taucoin.core.Blockchain;
import io.taucoin.http.message.Message;
import io.taucoin.listener.TaucoinListener;
import io.taucoin.net.message.ReasonCode;
import io.taucoin.net.rlpx.Node;
import io.taucoin.net.tau.TauVersion;
import io.taucoin.sync2.ChainInfoManager;
import io.taucoin.sync2.SyncStateEnum;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.math.BigInteger;
import java.util.List;

/**
 * HTTP Module Core Manager.
 *
 * Main Functions:
 *     (1) Sync state change invoke getting chain information or getting hashes.
 *     (2) Handle inbound messages, etc, ChainInfoMessage, HashesMessage and so on.
 */
@Singleton
public class RequestManager {

    protected static final Logger logger = LoggerFactory.getLogger("http");

    protected Blockchain blockchain;
    protected TaucoinListener listener;
    protected ChainInfoManager chainInfoManager;
    protected RequestQueue requestQueue;

    private Object stateLock = new Object();
    private SyncStateEnum syncState = SyncStateEnum.IDLE;

    @Inject
    public RequestManager(Blockchain blockchain, TaucoinListener listener,
            RequestQueue requestQueue, ChainInfoManager chainInfoManager) {
        this.blockchain = blockchain;
        this.listener = listener;
        this.requestQueue = requestQueue;
        this.chainInfoManager = chainInfoManager;
    }

    public void changeSyncState(SyncStateEnum state) {
        synchronized(stateLock) {
            this.syncState = state;
        }
    }

    public SyncStateEnum getSyncState() {
        synchronized(stateLock) {
            return this.syncState;
        }
    }
    public void handleMessage(Message message) {
    }

    /**
     * follow method should to be impletation.
     * all these are fit for main net.
     */
    public void init(){

    }
    public void stop(){

    }
    public boolean isHashRetrievingDone(){
        //todo
        return false;
    }
    public boolean isHashRetrieving(){
        //todo
        return false;
    }
    public void ban(RequestManager requestManager){

    }
    public void disconnect(ReasonCode reasonCode){

    }

    public byte[] getPeerIdShort(){
        return null;
    }

    public RequestManager getByNodeId(byte[] nodeId){
        return null;
    }

    public RequestManager getMaster(){
        return null;
    }
    public TauVersion getTauVersion(){
        return null;
    }

    public boolean hasCompatible(TauVersion version){
        return false;
    }
    /**
     * state change when at special condition.
     */
    public void changeStateForIdles(SyncStateEnum state,TauVersion version){

    }
    public void setLastHashToAsk(byte[] hash){

    }
    public byte[] getBestKnownHash(){
        return null;
    }
    public byte[] getLastHashToAsk(){
        return null;
    }
    public byte[] getMaxHashesAsk(){
        return null;
    }
    public List<RequestManager> nodesInUse(){
        return null;
    }
    public void logActivePeers(){

    }
    public void logBannedPeers(){

    }
    public boolean hasBlocksLack(){
        return false;
    }
    public BigInteger getTotalDifficulty(){
        return null;
    }
    public int activeCount(){
        return 0;
    }
    public void connect(Node node){

    }
}
