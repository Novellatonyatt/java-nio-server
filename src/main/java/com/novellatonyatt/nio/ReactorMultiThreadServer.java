package com.novellatonyatt.nio;

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
public class ReactorMultiThreadServer {

    private ThreadPoolExecutor eventHandlerPool = new ThreadPoolExecutor(10, 50, 2, TimeUnit.MINUTES, new ArrayBlockingQueue<Runnable>(200), new ThreadPoolExecutor.CallerRunsPolicy());

    private void start() throws IOException {
        Selector selector = Selector.open();
        ServerSocketChannel serverSocketChannel = createNIOServerSocketChannel();
        System.out.println("start nio server and bind port 8888");
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        selector.select();
        for (;;) {
            Set<SelectionKey> selectionKeySet = selector.selectedKeys();
            for (Iterator<SelectionKey> iterator = selectionKeySet.iterator(); iterator.hasNext(); ) {
                final SelectionKey selectionKey = iterator.next();
                if (selectionKey.isAcceptable()) {
                    System.out.println("acceptable");
                    acceptHandler(selectionKey);
                } else if (selectionKey.isReadable()) {
                    System.out.println("readable");
                    eventHandlerPool.submit(new Runnable() {
                        @Override
                        public void run() {
                            readHandler(selectionKey);
                        }
                    });
                }
                iterator.remove();
            }
            selector.select();
        }
    }

    private ServerSocketChannel createNIOServerSocketChannel() throws IOException {
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.bind(new InetSocketAddress(InetAddress.getLocalHost(), 8888));
        serverSocketChannel.configureBlocking(false);
        return serverSocketChannel;
    }

    private void acceptHandler(SelectionKey selectionKey) throws IOException {
        Selector selector = selectionKey.selector();
        ServerSocketChannel serverSocketChannel = (ServerSocketChannel) selectionKey.channel();
        SocketChannel socketChannel = serverSocketChannel.accept();
        if (socketChannel != null) {
            socketChannel.configureBlocking(false);
            socketChannel.register(selector, SelectionKey.OP_READ);
            System.out.println("accept client connection " + socketChannel.getLocalAddress());
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
        ReactorMultiThreadServer reactorServer = new ReactorMultiThreadServer();
        reactorServer.start();
    }

}
