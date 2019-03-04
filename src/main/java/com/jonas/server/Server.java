package com.jonas.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;

/**
 * C/S架构的服务端对象
 *
 * @author shenjy 2019/03/04
 */
public class Server {
    private int port;
    private volatile boolean running = false;
    private long receiveTimeDelay = 3000;
    private ConcurrentHashMap<Class, ServerAction> actionMapping = new ConcurrentHashMap<Class, ServerAction>();
    private Thread connWatchDog;

    public static void main(String[] args) {
        new Server(8080).start();
    }

    public Server(int port) {
        this.port = port;
    }

    public void start() {
        if (running) {
            return;
        }

        running = true;
        connWatchDog = new Thread(new ConnWatchDog());
        connWatchDog.start();
    }

    public void stop() {
        if (running) {
            running = false;
        }
    }

    public void addActionMap(Class<Object> cls, ServerAction action) {
        actionMapping.put(cls, action);
    }

    class ConnWatchDog implements Runnable {
        @Override
        public void run() {
            try {
                ServerSocket ss = new ServerSocket(port, 5);
                while (running) {
                    Socket s = ss.accept();
                    new Thread(new SocketAction(s)).start();
                }
            } catch (IOException e) {
                e.printStackTrace();
                Server.this.stop();
            }
        }
    }

    class SocketAction implements Runnable {

        Socket socket;
        boolean threadRunning = true;
        long lastReceiveTime = System.currentTimeMillis();

        public SocketAction(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            while (running && threadRunning) {
                if (System.currentTimeMillis() - lastReceiveTime > receiveTimeDelay) {
                    overThis();
                } else {
                    try {
                        InputStream in = socket.getInputStream();
                        if (in.available() > 0) {
                            ObjectInputStream objectInputStream = new ObjectInputStream(in);
                            Object obj = objectInputStream.readObject();
                            lastReceiveTime = System.currentTimeMillis();
                            System.out.println("接收：\t" + obj);
                            ServerAction action = actionMapping.get(obj.getClass());
                            action = null == action ? new DefaultServerAction() : action;
                            Object out = action.doAction(obj, Server.this);
                            if (null != out) {
                                ObjectOutputStream objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
                                objectOutputStream.writeObject(out);
                                objectOutputStream.flush();
                            }
                        } else {
                            Thread.sleep(10);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        overThis();
                    }
                }
            }
        }

        private void overThis() {
            if (threadRunning) {
                threadRunning = false;
            }

            if (null != socket) {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            System.out.println("关闭：" + socket.getRemoteSocketAddress());
        }
    }
}
