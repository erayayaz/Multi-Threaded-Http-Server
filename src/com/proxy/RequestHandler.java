package com.proxy;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class RequestHandler implements Runnable {
    private String ifModifiedSinceHeader;
    private Socket clientSocket;
    private int webServerPort;
    private InputStream clientToProxy;
    private OutputStream proxyToClient;
    private DataInputStream in;
    private DataOutputStream out;
    InputStreamReader inputStreamReader;


    public RequestHandler(Socket clientSocket, int webServerPort) throws IOException {
        this.clientSocket = clientSocket;
        this.clientToProxy = clientSocket.getInputStream();
        this.proxyToClient = clientSocket.getOutputStream();
        this.webServerPort = webServerPort;
        this.in = new DataInputStream(clientSocket.getInputStream());
        //this.out = new DataOutputStream(clientSocket.getOutputStream());
        this.inputStreamReader = new InputStreamReader(clientSocket.getInputStream());
    }

    @Override
    public void run() {
        try {
            processRequest();
        } catch (Exception e) {

        }
    }

    private void processRequest() {
        try{
            String line = "";
            String response = "";
            // get request from client
            BufferedReader b = new BufferedReader(new InputStreamReader(in));
            //read first line of request
            String request = b.readLine();
            System.out.println("Accept from client:");
            System.out.println(request);
            System.out.println();
            request = analyzeURL(request);

            // reading remaining part of request
            String remaningHeaders = gettingRemainingPart(b);
            System.out.println("Send to web server:");
            System.out.println(request);
            System.out.println(remaningHeaders);



            String[] query = request.split("\\s+");
            //String[] parameters = query[0].split("/");
            String methodType = query[0];
            System.out.println("Send to Client:");

            //check if server is open, send not found otherwise
            int size = 0;
            Socket webServer = null;
            try{
                webServer = new Socket(InetAddress.getLocalHost().getHostName(),webServerPort);
            } catch (IOException e) {
                // e.printStackTrace();
                notFound();
            }
            //send request to the web server
            try{

                if(!(query[1].substring(1).matches("[0-9]+"))){
                    size=0;
                }else{
                    size = Integer.parseInt(query[1].substring(1));
                }


                //check the size of request
                if(size > 9999){
                    requestTooLong();
                }else{
                    String filePath = size + "c" + ".html";

                    //if cache contains file, we add a new header and send request to server
                    if(ProxyServer.checkCache(filePath)){
                        //this header is indicates conditional get header
                        String conditional = "cache";
                        PrintWriter toClient = new PrintWriter(clientSocket.getOutputStream());
                        PrintWriter printWriter = new PrintWriter(webServer.getOutputStream());
                        printWriter.println(conditional);
                        printWriter.println(request);
                        printWriter.println(remaningHeaders);
                        printWriter.flush();

                        int counter = 0;


                        File cachedFile= null ;
                        FileWriter fileWriter =null;
                        PrintWriter cachedFileWriter = null;
                        BufferedReader fromServer = new BufferedReader(new InputStreamReader(webServer.getInputStream()));
                        while((response = fromServer.readLine()) != null){
                            // we return from cache if file is not modified
                            if(response.contains("NOT_MODIFIED")){
                                returnCachedFile(filePath);
                                break;
                            }
                            //if response doesn't contain not modified header, we create new file
                            if(counter == 0){
                                counter = 1;
                                cachedFile = new File(filePath);
                                cachedFile.createNewFile();
                                fileWriter = new FileWriter(cachedFile);
                                cachedFileWriter = new PrintWriter(fileWriter);
                            }
                            //we write response to file and client
                            System.out.println(response);
                            cachedFileWriter.println(response);
                            cachedFileWriter.flush();
                            toClient.println(response);
                            toClient.flush();
                        }
                       // System.out.println("-----------------------------------");
                    //if file is not in the cache, new file is created and request is sent to server
                    }else{
                        File cachedFile = new File(filePath);
                        cachedFile.createNewFile();
                        FileWriter fileWriter = new FileWriter(cachedFile);
                        PrintWriter cachedFileWriter = new PrintWriter(fileWriter);
                        PrintWriter toClient = new PrintWriter(clientSocket.getOutputStream());
                        PrintWriter printWriter = new PrintWriter(webServer.getOutputStream());
                        printWriter.println(request);
                        printWriter.println(remaningHeaders);
                        printWriter.flush();
                        //get response from web server and writing response to client and file together
                        BufferedReader fromServer = new BufferedReader(new InputStreamReader(webServer.getInputStream()));
                        while((response = fromServer.readLine()) != null){
                            System.out.println(response);
                            cachedFileWriter.println(response);
                            cachedFileWriter.flush();
                            toClient.println(response);
                            toClient.flush();
                        }


                        if(fromServer != null){
                            fromServer.close();
                        }
                        if(cachedFileWriter != null){
                            cachedFileWriter.close();
                        }
                        if(toClient != null){
                            toClient.close();
                        }
                        //we add file to cache if it is a GET request and contains only digit
                        if(!("0c.html".equals(filePath)) && ("GET".equals(methodType))){
                            ProxyServer.addCache(filePath,cachedFile);
                        }
                        webServer.close();
                    }



                }

                System.out.println();
                System.out.println("-------------------------------");
                System.out.println();
                // size = Integer.parseInt(parameters[1].substring(15));
            } catch (NumberFormatException e) {
//                e.printStackTrace();
            }



        } catch (Exception e) {
           //e.printStackTrace();
        }
        try{
            in.close();
        } catch (IOException e) {
          //  e.printStackTrace();
        }
    }

























    //send file from cache when it is in cache and not modified
    private void returnCachedFile(String name) throws IOException {
        System.out.println("returned from cache");
        File cachedFile = ProxyServer.getCachedFile(name);
        OutputStream outToClient = clientSocket.getOutputStream();
        Path filePath = Paths.get(name);
        try {
            outToClient.write(Files.readAllBytes(filePath));
            outToClient.flush();

        } catch (Exception e) {
           // e.printStackTrace();
        }
        try {
            in.close();
            outToClient.close();

        } catch (Exception e) {
           // e.printStackTrace();
        }

    }
    //send request too long error when size is higher than 9999
    private void requestTooLong() throws IOException {
        OutputStream outToClient = clientSocket.getOutputStream();
        String status = "HTTP/1.1 414 Request-URI Too Long\r\n";
        String server = "Server: HTTP Server/1.1\r\n";
        String content_type = "Content-Type: text/html\r\n";
        String content_length = "Content-Length: 0\r\n\r\n";

        System.out.println(status + server + content_type + content_length);
        try{
            outToClient.write(status.getBytes(StandardCharsets.UTF_8));
            outToClient.write(server.getBytes(StandardCharsets.UTF_8));
            outToClient.write(content_type.getBytes(StandardCharsets.UTF_8));
            outToClient.write(content_length.getBytes(StandardCharsets.UTF_8));
            outToClient.flush();
        } catch (Exception e) {
           // e.printStackTrace();
        }
        try{
            in.close();
            outToClient.close();
        } catch (IOException e) {
           // e.printStackTrace();
        }
    }
    // send not found error when web server is not found
    private void notFound() throws IOException {
        OutputStream outToClient = clientSocket.getOutputStream();
        String status = "HTTP/1.1 404 Not Found\r\n";
        String server = "Server: HTTP Server/1.1\r\n";
        String content_type = "Content-Type: text/html\r\n";
        String content_length = "Content-Length: 0\r\n\r\n";

        System.out.println(status + server + content_type + content_length);
        System.out.println("-------------------------------");


        try{
            outToClient.write(status.getBytes(StandardCharsets.UTF_8));
            outToClient.write(server.getBytes(StandardCharsets.UTF_8));
            outToClient.write(content_type.getBytes(StandardCharsets.UTF_8));
            outToClient.write(content_length.getBytes(StandardCharsets.UTF_8));
            outToClient.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
        try{
            in.close();
            outToClient.close();
        } catch (IOException e) {
          //  e.printStackTrace();
        }
    }
    //converting first line of the request that web server can handle
    private String analyzeURL(String request) {

        String decode = "";
        try {
            String[] parameters = request.split("\\s+");

            String[] test = parameters[1].split("/");


            decode += parameters[0] + " /" + test[3] + " " + parameters[2];
        } catch (Exception e) {
//            e.printStackTrace();
        }

        return decode;
    }
    //reading other fields of request and saving in a string
    private String gettingRemainingPart(BufferedReader b) throws IOException {
        String decode = "";
        String line = "";
        while(!(line = b.readLine()).equals("")){
            decode += line;
            decode += "\n";
        }
        decode += "\r\n";
        return decode;
    }




}