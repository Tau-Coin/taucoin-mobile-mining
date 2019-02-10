package io.taucoin.android.service;

import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;

import io.taucoin.android.rpc.server.JsonRpcServer;
import io.taucoin.android.manager.BlockLoader;
import io.taucoin.android.interop.*;

import java.util.ArrayList;

public class TaucoinAidlService extends TaucoinService {

    protected ArrayList<IListener> clientListeners = new ArrayList<>();

    public static String log = "";

    IEthereumService.Stub binder = null;

    public TaucoinAidlService() {

        initializeBinder();
    }

    protected void initializeBinder() {

        binder = new IEthereumService.Stub() {

            public void loadBlocks(String dumpFile) throws RemoteException {

                BlockLoader blockLoader = (BlockLoader)taucoin.getBlockLoader();
                blockLoader.loadBlocks(dumpFile);
            }

            public void connect(String ip, int port, String remoteId) throws RemoteException {

                if (!isConnected) {
                    System.out.println("Connecting to : " + ip);
                    taucoin.connect(ip, port, remoteId);
                    isConnected = true;
                } else {
                    System.out.println("Already connected");
                    System.out.println("x " + taucoin.isConnected());
                }
            }

            public void addListener(IListener listener) throws RemoteException {

                clientListeners.clear();
                clientListeners.add(listener);
            }

            public void removeListener(IListener listener) throws RemoteException {

                try {
                    clientListeners.remove(listener);
                } catch (Exception e) {
                    System.out.println("ERRORRRR: " + e.getMessage());
                }
            }

            public void startJsonRpcServer() throws RemoteException {

                //TODO: add here switch between full and light version
                jsonRpcServer = new io.taucoin.android.rpc.server.full.JsonRpcServer(taucoin);
            }

            public void getLog(IAsyncCallback callback) throws  RemoteException {

                callback.handleResponse(TaucoinAidlService.log);
            }
        };
    }

    protected void broadcastMessage(String message) {

        for (IListener listener: clientListeners) {
            try {
                listener.trace(message);
            } catch (Exception e) {
                // Remove listener
                System.out.println("ERRORRRR: " + e.getMessage());
                clientListeners.remove(listener);
            }
        }
    }

    @Override
    public void onCreate() {

        super.onCreate();
    }

    @Override
    public IBinder onBind(Intent intent) {

        return binder;
    }

}
