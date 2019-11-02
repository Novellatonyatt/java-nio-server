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
 * @Auther: ZHUANGHAOTANG
 * @Date: 2019/10/26 16:35
 * @Description:
 */
public class Server {


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
        socketChannel.configureBlocking(false);
        socketChannel.register(selector, SelectionKey.OP_READ);
        System.out.println("accept client connection " + socketChannel.getLocalAddress());
    }

    private void readHandler(SelectionKey selectionKey) throws IOException {
        SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
        ByteBuffer byteBuffer = ByteBuffer.allocate(100);
        int num = socketChannel.read(byteBuffer);
        if (num == -1) {
            System.out.println("client " + socketChannel.getLocalAddress() + " disconnection");
            socketChannel.close();
            return;
        }
        byteBuffer.flip();
        while (byteBuffer.hasRemaining()) {
            byte b = byteBuffer.get();
            System.out.println((char) b);
        }
    }

    public void open() throws IOException {
        Selector selector = Selector.open();
        ServerSocketChannel serverSocketChannel = createNIOServerSocketChannel();
        System.out.println("start nio server and bind port 8888");
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        int ready = selector.select();
        while (ready > 0) {
            System.out.println("ready channel count " + ready);
            Set<SelectionKey> selectionKeySet = selector.selectedKeys();
            for (Iterator<SelectionKey> iterator = selectionKeySet.iterator(); iterator.hasNext(); ) {
                SelectionKey selectionKey = iterator.next();
                if (selectionKey.isAcceptable()) {
                    System.out.println("acceptable");
                    acceptHandler(selectionKey);
                } else if (selectionKey.isReadable()) {
                    System.out.println("readable");
                    readHandler(selectionKey);
                }
                iterator.remove();
            }
            ready = selector.select();
        }
    }

}
