import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;

public class Server extends Thread{
    private ChatPeer cp;
    private ServerSocket serverSocket;

    public void closeSocket(){
        try {
            this.serverSocket.close();
        } catch (IOException e) {
            System.out.println("server has closed");
        }
    }

    public Server(ChatPeer cp){
        this.cp = cp;
    }

    public void run(){
        try {
            serverSocket = new ServerSocket(cp.getInPort());
            System.out.println("server socket address:" + serverSocket.getInetAddress().toString());
            System.out.println("server socket port:" + serverSocket.getLocalPort());
            System.out.println("server socket full address:" + serverSocket.getLocalSocketAddress().toString());
            System.out.println("start chatpeer-server");

            while(true) {
                Socket socket = serverSocket.accept();
                System.out.println("accept socket");
                String oppositeId = socket.getRemoteSocketAddress().toString().replaceAll("/", "");
                ServerThread thread = new ServerThread(cp, socket,  oppositeId);
                cp.addThread(thread);
                //cp.addSocket(socket);
                thread.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(0);
        }
    }
}
