package com.xiezhiai.wechatplugin.func.transfer;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.text.TextUtils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.xiezhiai.wechatplugin.utils.LogUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by shijiwei on 2018/11/29.
 *
 * @Desc:
 */
public class PluginClient extends Service {

    private static final String TAG = "PluginClient";
    private static final String SUFFIX = "\r\n";
    /* tcp server port */
    private final int TCP_PORT = 6666;
    private final int CHECK_TCP_SERVER_INTERVAL = 5 * 1000;
    /* The monitoring service is opened */
    private boolean isStartServer = false;
    private boolean TCP_SERVER_ENABLE = true;

    private Socket client;
    private StringBuilder buffer = new StringBuilder();
    private ExecutorService mClientPolice = Executors.newSingleThreadExecutor();
    private static LinkedBlockingQueue<PluginMessasge> pluginMessasges = new LinkedBlockingQueue<>();
    private PluginHandler pluginHandler;

    private static Handler mClientLooper = new Handler();
    private Runnable mClientRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isStartServer && TCP_SERVER_ENABLE) {
                startClient();
            }
            mClientLooper.postDelayed(this, CHECK_TCP_SERVER_INTERVAL);
        }
    };


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mClientLooper.removeCallbacks(mClientRunnable);
        mClientLooper.postDelayed(mClientRunnable, 0);
        pluginHandler = new PluginHandler(this);
    }

    @Override
    public void onDestroy() {
        release();
        super.onDestroy();
    }


    /**
     * 消息轮训机
     */
    private class MessageLooper implements Runnable {

        private WeakReference<Socket> wf;

        public MessageLooper(Socket client) {
            wf = new WeakReference<>(client);
        }

        @Override
        public void run() {

            Socket socket = wf.get();
            try {
                if (socket != null) {
                    while (socket != null && socket.isConnected() && !socket.isClosed() && !socket.isOutputShutdown() && !socket.isOutputShutdown()) {
                        int index = buffer.indexOf(SUFFIX);
                        if (index != -1) {
                            String _msg = buffer.substring(0, index);
                            buffer.delete(0, index + SUFFIX.length());
                            Message msg = pluginHandler.obtainMessage();
                            msg.obj = _msg;
                            pluginHandler.sendMessage(msg);
                        }
                    }
                }
            } catch (Exception e) {

            } finally {
                try {
                    if (socket != null) {
                        socket.close();
                        socket = null;
                    }
                } catch (Exception e) {

                } finally {
                    LogUtil.e(TAG + " 服务器掉线了. MessageLooper");
                }

            }

        }
    }

    /**
     * 客户端报文读操作
     */
    class ClientReader implements Runnable {

        private WeakReference<Socket> wf;

        public ClientReader(Socket client) {
            wf = new WeakReference<>(client);
        }

        @Override
        public void run() {
            Socket socket = wf.get();
            if (socket != null) {
                InputStream ins = null;
                try {
                    ins = socket.getInputStream();
                    byte[] buf = new byte[1024];
                    int len = 0;
                    while (((len = ins.read(buf)) != -1) && socket != null && !socket.isClosed() && socket.isConnected() && !socket.isOutputShutdown() && !socket.isInputShutdown() && TCP_SERVER_ENABLE) {
                        buffer.append(new String(buf, 0, len));
                    }
                    LogUtil.e(TAG + " len = " + len);
                    logError(socket);

                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        LogUtil.e(TAG + " 服务器掉线了. Read");
                        if (ins != null) {
                            ins.close();
                            ins = null;
                        }

                        if (socket != null) {
                            socket.close();
                            socket = null;
                        }
                    } catch (Exception e) {

                    } finally {
                        isStartServer = false;
                    }
                }
            }
        }
    }

    /**
     * 客户端报文写操作
     */
    class ClientWriter implements Runnable {

        private WeakReference<Socket> wf;

        public ClientWriter(Socket client) {
            wf = new WeakReference<>(client);
        }

        @Override
        public void run() {
            Socket socket = wf.get();
            if (socket != null) {
                OutputStream outs = null;
                try {
                    outs = socket.getOutputStream();
                    while (outs != null && socket != null && !socket.isClosed() && socket.isConnected() && !socket.isOutputShutdown() && !socket.isInputShutdown() && TCP_SERVER_ENABLE) {
                        PluginMessasge msg = pluginMessasges.poll();
                        if (msg == null) continue;
                        outs.write(msg.toJSONString().getBytes());
                        outs.flush();
                    }
                    logError(socket);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    try {
                        LogUtil.e(TAG + " 服务器掉线了. Write");
                        if (socket != null) {
                            socket.close();
                            socket = null;
                        }
                        if (outs != null) {
                            outs.close();
                            outs = null;
                        }
                    } catch (Exception e) {
                    } finally {
                        isStartServer = false;
                    }
                }
            }
        }
    }


    /**
     * 开启客户端
     */
    private void startClient() {
        mClientPolice.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    LogUtil.e(TAG + " 客户端预连接.... ");
                    release();
                    isStartServer = true;
                    TCP_SERVER_ENABLE = true;
                    client = new Socket();
                    SocketAddress socketAddress = new InetSocketAddress("localhost", TCP_PORT);
                    client.connect(socketAddress, 0);
                    client.setKeepAlive(true);
                    new Thread(new ClientReader(client)).start();
                    new Thread(new ClientWriter(client)).start();
                    new Thread(new MessageLooper(client)).start();
                } catch (Exception e) {
                    isStartServer = false;
                }
            }
        });
    }

    /**
     * 释放资源
     */
    private void release() {

        try {
            if (client != null) client.close();
        } catch (Exception e) {
        } finally {
            client = null;
            isStartServer = false;
            TCP_SERVER_ENABLE = false;
        }

    }

    /**
     * 发送指令
     *
     * @param pMsg
     */
    public static void tansferMessage(PluginMessasge pMsg) {
        if (pMsg == null) return;
        try {
            Iterator<PluginMessasge> iterator = pluginMessasges.iterator();
            while (iterator.hasNext()) {
                PluginMessasge next = iterator.next();
                if (next.getAction() == pMsg.getAction()) {
                    pluginMessasges.remove(next);
                }
            }
            pluginMessasges.put(pMsg);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void logError(Socket socket) {
        LogUtil.e(TAG + " socket = " + socket);
        if (socket != null) {
            LogUtil.e(TAG + " isClosed = " + socket.isClosed());
            LogUtil.e(TAG + " isConnected = " + socket.isConnected());
            LogUtil.e(TAG + " isOutputShutdown = " + socket.isOutputShutdown());
            LogUtil.e(TAG + " isInputShutdown = " + socket.isInputShutdown());
            LogUtil.e(TAG + " TCP_SERVER_ENABLE = " + TCP_SERVER_ENABLE);
        }
    }
}
