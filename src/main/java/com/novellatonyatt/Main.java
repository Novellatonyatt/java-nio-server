package com.novellatonyatt;

import com.novellatonyatt.nio.Server;

import java.io.IOException;

/**
 * @Auther: ZHUANGHAOTANG
 * @Date: 2019/10/26 16:11
 * @Description:
 */
public class Main {

    public static void main(String[] args) throws IOException {
        Server server = new Server();
        server.open();
    }


}
