package com.novellatonyatt.nio;

import com.google.common.collect.Iterables;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @Author: Zhuang HaoTang
 * @Date: 2019-10-28 17:00
 * @Description:
 */
public class MainSubReactorMultiThreadServer {

    private ThreadPoolExecutor eventHandlerPool = new ThreadPoolExecutor(10, 50, 2, TimeUnit.MINUTES, new ArrayBlockingQueue<Runnable>(200), new ThreadPoolExecutor.CallerRunsPolicy());

    private void start() throws IOException {
        final Selector mainSelector = Selector.open();
        final Selector subSelector = Selector.open();

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    startMainSelector(mainSelector, subSelector);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    startSubSelector(subSelector);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();

    }

    /**
     * 第一个事件分发器,用于监听ServerSocketChannel的接收就绪事件
     */
    private void startMainSelector(Selector mainSelector, final Selector subSelector) throws IOException {
        ServerSocketChannel serverSocketChannel = createNIOServerSocketChannel();
        System.out.println("start nio server and bind port 8888");
        serverSocketChannel.register(mainSelector, SelectionKey.OP_ACCEPT);
        mainSelector.select();
        for (; ; ) {
            Set<SelectionKey> selectionKeySet = mainSelector.selectedKeys();
            SelectionKey selectionKey = Iterables.getOnlyElement(selectionKeySet);
            if (selectionKey.isAcceptable()) {
                System.out.println("acceptable");
                acceptHandler(selectionKey, subSelector); // 单线程同步处理接收就绪
                selectionKeySet.clear();
            }
            mainSelector.select();
        }
    }

    /**
     * 第二个事件分发器,用于监听SockChannel的读写就绪事件
     */
    private void startSubSelector(Selector subSelector) throws IOException {
        subSelector.select();
        for (; ; ) {
            Set<SelectionKey> selectionKeySet = subSelector.selectedKeys();
            for (Iterator<SelectionKey> iterator = selectionKeySet.iterator(); iterator.hasNext(); ) {
                final SelectionKey selectionKey = iterator.next();
                if (selectionKey.isReadable()) {
                    System.out.println("readable");
                    eventHandlerPool.submit(new Runnable() {
                        @Override
                        public void run() {
                            readHandler(selectionKey);
                        }
                    });
                    iterator.remove();
                }
            }
            subSelector.select();
        }
    }

    private ServerSocketChannel createNIOServerSocketChannel() throws IOException {
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.bind(new InetSocketAddress(InetAddress.getLocalHost(), 8888));
        serverSocketChannel.configureBlocking(false);
        return serverSocketChannel;
    }

    private void acceptHandler(SelectionKey selectionKey, Selector subSelector) throws IOException {
        ServerSocketChannel serverSocketChannel = (ServerSocketChannel) selectionKey.channel();
        SocketChannel socketChannel = serverSocketChannel.accept();
        if (socketChannel != null) {
            socketChannel.configureBlocking(false);
            subSelector.wakeup(); // 往Selector注册Channel时,Selector要处于非阻塞状态
            socketChannel.register(subSelector, SelectionKey.OP_READ);
            System.out.println("accept client connection " + socketChannel.getLocalAddress() + " and register to subSelector");
        }
    }

    private void readHandler(SelectionKey selectionKey) {
        SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
        ByteBuffer byteBuffer = ByteBuffer.allocate(100);
        try {
            int num = socketChannel.read(byteBuffer);
            if (num == -1) {
                System.out.println("client " + socketChannel.getLocalAddress() + " disconnection");
                socketChannel.close(); // 底层有些逻辑
                return;
            }
            byteBuffer.flip();
            while (byteBuffer.hasRemaining()) {
                byte b = byteBuffer.get();
                System.out.println((char) b);
            }
        } catch (Exception e) {
            System.out.println("由于连接关闭导致并发线程读取异常");
        }
    }

    public static void main(String[] args) throws IOException {
        MainSubReactorMultiThreadServer reactorServer = new MainSubReactorMultiThreadServer();
        reactorServer.start();
    }

}
