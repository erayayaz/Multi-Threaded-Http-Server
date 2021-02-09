package com.proxy;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;

public class ProxyServer{
    private int webServerPort;
    private ServerSocket proxySocket;
    static HashMap<String, File> cache;
    private final static int PORT = 8888;

    public ProxyServer(int webServerPort){
        this.webServerPort = webServerPort;
    }

    //we create proxy server and initialize cache
    public void startServer() throws IOException{
        try {
            proxySocket = new ServerSocket(PORT);
            cache = new HashMap<>();
        } catch (Exception e) {
            e.printStackTrace();
        }
        //while server is running it create a new thread for each connection
        while (true){
            Socket connection = proxySocket.accept();
            RequestHandler proxy = new RequestHandler(connection, webServerPort);
            Thread t = new Thread(proxy);
            t.start();

        }
    }
    //adding file to cache
    public static void addCache(String name, File file){
        cache.put(name,file);
    }
    //check if cache contains file
    public static boolean checkCache(String name){
        return cache.containsKey(name);
    }
    //return file from cache
    public static File getCachedFile(String name){
        return cache.get(name);
    }



}
