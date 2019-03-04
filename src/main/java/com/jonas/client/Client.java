package com.jonas.client;

import com.jonas.KeepAlive;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;

/**
 * C/S架构的客户端对象，持有该对象，可以随时向服务端发送消息
 *
 * @author shenjy 2019/03/04
 */
public class Client {

    private String serverIp;
    private int port;
    private Socket socket;
    private volatile boolean running = false;
    private long lastSendTime;
    private ConcurrentHashMap<Class, ClientAction> actionMapping = new ConcurrentHashMap<Class, ClientAction>();

    public static void main(String[] args) {
        Client client = new Client("127.0.0.1", 8080);
        client.start();
    }

    public Client(String serverIp, int port) {
        this.serverIp = serverIp;
        this.port = port;
    }

    public void start() {
        if (running) {
            return;
        }
        try {
            socket = new Socket(serverIp, port);
            System.out.println("本地端口：" + socket.getLocalPort());
            lastSendTime = System.currentTimeMillis();
            running = true;
            //保持长连接的线程，每隔2秒项服务器发一个一个保持连接的心跳消息
            new Thread(new KeepAliveWatchDog()).start();
            //接受消息的线程，处理消息
            new Thread(new ReceiveWatchDog()).start();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void stop() {
        if (running) {
            running = false;
        }
    }

    /**
     * 添加接收对象的处理对象。
     *
     * @param cls    待处理的对象，其所属的类。
     * @param action 处理过程对象。
     */
    public void addActionMap(Class<Object> cls, ClientAction action) {
        actionMapping.put(cls, action);
    }

    public void sendObject(Object obj) {
        try {
            ObjectOutputStream stream = new ObjectOutputStream(socket.getOutputStream());
            stream.writeObject(obj);
            System.out.println("发送：\t" + obj);
            stream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    class KeepAliveWatchDog implements Runnable {
        long checkDelay = 10;
        long keepAliveDelay = 2000;

        @Override
        public void run() {
            while (running) {
                if (System.currentTimeMillis() - lastSendTime > keepAliveDelay) {
                    try {
                        Client.this.sendObject(new KeepAlive());
                    } catch (Exception e) {
                        e.printStackTrace();
                        Client.this.stop();
                    }
                    lastSendTime = System.currentTimeMillis();
                } else {
                    try {
                        Thread.sleep(checkDelay);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        Client.this.stop();
                    }
                }
            }
        }
    }

    class ReceiveWatchDog implements Runnable {
        @Override
        public void run() {
            while (running) {
                try {
                    InputStream in = socket.getInputStream();
                    if (in.available() > 0) {
                        ObjectInputStream inputStream = new ObjectInputStream(in);
                        Object obj = inputStream.readObject();
                        System.out.println("接收：\t" + obj);

                        ClientAction action = actionMapping.get(obj.getClass());
                        action = action == null ? new DefaultClientAction() : action;
                        action.doAction(obj, Client.this);
                    } else {
                        Thread.sleep(10);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Client.this.stop();
                }
            }
        }
    }
}
