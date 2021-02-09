package com.company;


import java.io.*;
import java.net.*;

public class HttpServer{

    private ServerSocket serverSocket;
    private Socket clientSocket;
    private int port;

    public HttpServer(){

    }

    public HttpServer(int port){
        this.port = port;
    }

    public HttpServer(Socket socket){
        clientSocket = socket;
    }

    public void startServer() throws IOException {
        //creating new web server with given port number
        try {
            serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            e.printStackTrace();
        }
        // while server is running new thread is created with accepted connection
        while(true){
            Socket connection = serverSocket.accept();
            GETHandler test = new GETHandler(connection);
            Thread t = new Thread(test);
            t.start();
        }
    }

}
