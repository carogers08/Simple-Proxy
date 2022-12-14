package Proxy;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;



public class ProxyServer {

    //cache is a Map: the key is the URL and the value is the file name of the file that stores the cached content
    Map<String, String> cache;

    ServerSocket proxySocket;

    String logFileName = "log.txt";
    String errorLog = "errors.txt";

    public static void main(String[] args) {
        new ProxyServer().startServer(Integer.parseInt(args[0]));
    }

    void startServer(int proxyPort) {

        cache = new ConcurrentHashMap<>();

        // create the directory to store cached files.
        File cacheDir = new File("cached");
        if (!cacheDir.exists() || (cacheDir.exists() && !cacheDir.isDirectory())) {
            cacheDir.mkdirs();
        }

        while (true) {
            ServerSocket server;

            try {
                server = new ServerSocket(proxyPort);
                Socket client = server.accept();

                RequestHandler clientHandler = new RequestHandler(client, this);
                clientHandler.run();

                client.close();
            }
            catch (Exception e)
            {
                writeError(e.getMessage());
            }
        }
    }



    public String getCache(String hashcode) {
        return cache.get(hashcode);
    }

    public void putCache(String hashcode, String fileName) {
        cache.put(hashcode, fileName);
    }

    public synchronized void writeLog(String info) {
        try {
            FileWriter fileWriter = new FileWriter(logFileName);
            String timeStamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date());
            fileWriter.append(timeStamp + " " + info);
            fileWriter.close();
        }
        catch (Exception e)
        {
            writeError(e.getMessage());
        }
    }

    public synchronized void writeError(String error) {
        try {
            FileWriter fileWriter = new FileWriter(errorLog);
            String timeStamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date());
            fileWriter.append(timeStamp + " " + error);
            fileWriter.close();
        }
        catch (Exception e) { } //Not sure what to do if even our logger fails :joy:
    }
}