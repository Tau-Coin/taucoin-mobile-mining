package io.taucoin.rpc.server;

import io.taucoin.facade.Taucoin;

public abstract class JsonRpcServer {
    public JsonRpcServer(Taucoin taucoin) {};
    public abstract void start(int port) throws Exception;
    public abstract void stop();
}