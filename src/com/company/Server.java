package com.company;

import java.io.IOException;

public class Server {

    public static void main(String[] args) throws IOException {

        //if port number is not speficied, usage is printed
        if(args.length != 1){
            System.out.println("Usage: java Server {port-number}");
        }
        System.out.println(args[0]);
        //creating web server with given port number
        HttpServer server = new HttpServer(Integer.parseInt(args[0]));
        server.startServer();

    }
}
