package Proxy;

import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.SecureRandom;


// RequestHandler is thread that process requests of one client connection
public class RequestHandler extends Thread {
    Socket clientSocket;
    InputStream inFromClient;
    OutputStream outToClient;

    byte[] request = new byte[1024];
    private ProxyServer server;

    private boolean previousWasR = false;

    public RequestHandler(Socket clientSocket, ProxyServer proxyServer) {
        this.clientSocket = clientSocket;

        this.server = proxyServer;

        try {
            clientSocket.setSoTimeout(2000);
            inFromClient = clientSocket.getInputStream();
            outToClient = clientSocket.getOutputStream();

        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    @Override
    public void run() {
        /**
         * To do
         * Process the requests from a client. In particular,
         * (1) Check the request type, only process GET request and ignore others
         * (2) Write log.
         * (3) If the url of GET request has been cached, respond with cached content
         * (4) Otherwise, call method proxyServertoClient to process the GET request
         *
         */

        try {
            inFromClient.read(this.request);
            String requestString = new String(request);
            if (!requestString.toLowerCase().contains("get")) {
                return;
            }
            else {
                String url = requestString.substring(requestString.indexOf(" ") + 1);
                url = url.substring(0, url.indexOf(" "));

                server.writeLog(url); //CALEBX - We might need to include browser ip in this whatever that means. browserIP = ip of the requesting client?

                String cachedFileName = server.getCache(url);
                if (cachedFileName != null)
                    sendCachedInfoToClient(cachedFileName);
                else
                    proxyServertoClient(request, url);
            }
        }
        catch (Exception e)
        {
            server.writeError(e.getMessage());
        }
    }


    private void proxyServertoClient(byte[] clientRequest, String urlString) {
        FileOutputStream fileWriter = null;
        Socket toWebServerSocket = null;
        InputStream inFromServer;
        OutputStream outToServer;

        // Create Buffered output stream to write to cached copy of file
        String fileName = "cached/" + generateRandomFileName() + ".dat";

        // to handle binary content, byte is used
        byte[] serverReply = new byte[4096];


        /**
         * To do
         * (1) Create a socket to connect to the web server (default port 80)
         * (2) Send client's request (clientRequest) to the web server, you may want to use flush() after writing.
         * (3) Use a while loop to read all responses from web server and send back to client
         * (4) Write the web server's response to a cache file, put the request URL and cache file name to the cache Map
         * (5) close file, and sockets.
         */
        String url = urlString.substring(7);
        String pieces[] = url.split(":");
        url = pieces[0];
        int port  = Integer.valueOf(pieces[1]);
        try {
            InetAddress address = InetAddress.getByName(url);
            toWebServerSocket = new Socket(address, port);
            inFromServer = toWebServerSocket.getInputStream();
            outToServer = toWebServerSocket.getOutputStream();
            fileWriter = new FileOutputStream(fileName);
            outToServer.write(clientRequest);
            outToServer.flush();
            while(inFromServer.read(serverReply) != -1){
                outToClient.write(serverReply, 0, inFromServer.read(serverReply));
                outToClient.flush();
                fileWriter.write(serverReply, 0, inFromServer.read(serverReply));
                fileWriter.close();
            }
            server.putCache(clientSocket.getInetAddress().getHostAddress(), fileName);
            inFromServer.close();
            toWebServerSocket.close();
            fileWriter.close();
            outToServer.close();
        } catch (Exception e) {
            server.writeError(e.getMessage());
        }
    }



    // Sends the cached content stored in the cache file to the client
    private void sendCachedInfoToClient(String fileName) {

        try {

            byte[] bytes = Files.readAllBytes(Paths.get(fileName));

            outToClient.write(bytes);
            outToClient.flush();

        } catch (Exception e) {
            e.printStackTrace();
        }

        try {

            if (clientSocket != null) {
                clientSocket.close();
            }

        } catch (Exception e) {
            e.printStackTrace();

        }
    }


    // Generates a random file name
    public String generateRandomFileName() {

        String ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ-_";
        SecureRandom RANDOM = new SecureRandom();
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < 10; ++i) {
            sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }

}