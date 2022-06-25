import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.simple.parser.ParseException;


public class ChatClient {



    public static void main(String[] args)
    {
        String ip = null;
        int port = 4444;

        // check the length of args
        try
        {
            if (args.length == 0)
            {
                System.out.println("please provide ip address and port number");
            }
            else if(args.length == 1)
            {
                ip = args[0].trim();
                Socket socket = new Socket();
                SocketAddress address = new InetSocketAddress(ip, port);
                socket.connect(address, 10000);
                new Thread(new ClientListener(socket)).start();
            }
            else
            {

                ip = args[0].trim();
                port = Integer.parseInt(args[2].trim());
                Socket socket = new Socket();
                SocketAddress address = new InetSocketAddress(ip, port);
                socket.connect(address, 10000);
                new Thread(new ClientListener(socket)).start();

            }

        }
        catch (IOException e)
        {
            System.out.println("Server connection lost, check if the server is available");
        }
        catch(Exception e)
        {
            System.out.println("please check input");
        }
    }

}
