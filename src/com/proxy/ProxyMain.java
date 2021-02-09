package com.proxy;


import java.io.IOException;

public class ProxyMain {
    public static void main(String[] args) throws IOException {
        //we get port number of web server, if it is not given usage is printed
        if(args.length != 1){
            System.out.println("Usage: java ProxyMain {port-number}");
        }
      //  System.out.println(args[0]);
        //proxy server is printed with given port number
        ProxyServer server = new ProxyServer(Integer.parseInt(args[0]));
        server.startServer();

    }
}
