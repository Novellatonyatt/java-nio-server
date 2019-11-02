package com.novellatonyatt.nio;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

/**
 * @Auther: Zhuang HaoTang
 * @Date: 2019/10/26 16:36
 * @Description:
 */
public class Client {

    public static void main(String[] args) throws IOException, InterruptedException {
        SocketChannel socketChannel = SocketChannel.open();
        socketChannel.connect(new InetSocketAddress(InetAddress.getLocalHost(), 8888));
        String message = "today is sunday";
        ByteBuffer byteBuffer = ByteBuffer.allocate(message.getBytes().length);
        byteBuffer.put(message.getBytes());
        byteBuffer.flip();
        socketChannel.write(byteBuffer);
        Thread.sleep(2000);
        ByteBuffer byteBuffer1 = ByteBuffer.allocate("wo".getBytes().length).put("wo".getBytes());
        byteBuffer1.flip();
        socketChannel.write(byteBuffer1);


        ByteBuffer receiveBuffer = ByteBuffer.allocate(1024);
        while (true) {
            socketChannel.read(receiveBuffer);
            receiveBuffer.flip();
            while (receiveBuffer.hasRemaining()) {
                System.out.println((char)receiveBuffer.get());
            }
            receiveBuffer.clear();
        }
    }

}
