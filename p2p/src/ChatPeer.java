import org.json.JSONObject;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.*;


public class ChatPeer {
    private int inPort;
    private int outPort;
    private String id;
    private String ip;
    private String room;
    private String host;
    private HashSet<ServerThread> threadSet;
    //private HashSet<Socket> neighbors;
    private HashMap<String, ArrayList<ServerThread>> roomlist;
    private Socket cpSocket;
    private ServerThread cpThread;
    private boolean quitFlag;
    private ArrayList<String> blacklist;
    private int migrateCount;
    private InetAddress addr;

    public ChatPeer(){
        this.inPort = 4444;
        this.outPort = -1;
        this.threadSet = new HashSet<ServerThread>();
        //neighbors = new HashSet<Socket>();
        this.room = "";
        this.id = "";
    }

    public ChatPeer(int inPort, int outPort){
        this.inPort = inPort;
        this.outPort = outPort;
        this.threadSet = new HashSet<ServerThread>();
        //this.neighbors = new HashSet<Socket>();
        this.roomlist = new HashMap<String, ArrayList<ServerThread>>();
        this.room = "";
        this.id = "";
        this.quitFlag = false;
        this.blacklist = new ArrayList<String>();
        this.migrateCount = 0;
    }

    public void setId(String id){
        this.id = id;
    }

    public void resetId(){
        this.id = "";
    }

    public String getClientId(){
        return this.id;
    }

    public int getInPort() {
        return inPort;
    }

    public int getOutPort() {
        return outPort;
    }

    public String getIp() {
        return ip;
    }

    public String getRoom() {
        return room;
    }

    public void setRoom(String roomid){
        this.room = roomid;
    }

    public void updateRoom(String room){
        this.room = room;
    }

    public HashSet<ServerThread> getThreadSet() {
        return threadSet;
    }

    public void addThread(ServerThread peer){
        threadSet.add(peer);
    }

    public void removeThread(ServerThread peer){
        threadSet.remove(peer);
    }

    public ServerThread getCurrentClientThread(){
        return this.cpThread;
    }

    public Socket getCurrentClientSocket() {
        return this.cpSocket;
    }

    public void cpConnect(String ip, int port, int specPort){
        System.out.println("begin connect");
        try {
            Socket sc;
            int thisOutPort;


            if (specPort != -1) {
                sc = new Socket(ip, port, addr, specPort);
            } else if (outPort != -1) {
                sc = new Socket(ip, port, addr, outPort);
            } else {
                sc = new Socket(ip, port);
            }
            String oppositeId = sc.getRemoteSocketAddress().toString().replaceAll("/", "");
            ServerThread thread=new ServerThread(this, sc, oppositeId);
            thread.setHostId(ip + ":" + port);
            thread.setAsClientConnection();
            this.cpThread = thread;
            System.out.println("client getaddress:" + this.cpThread.getSocket().getLocalAddress().toString().replaceAll("/", ""));
            System.out.println("client getport:" + this.cpThread.getSocket().getLocalPort());
            System.out.println("client getsocketaddress:" + this.cpThread.getSocket().getLocalSocketAddress().toString().replaceAll("/", ""));
            thread.start();
            this.addThread(this.cpThread);
            thread.sendHost(host);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getHost(){
        return this.host;
    }

    public void setHost(String host){
        this.host = host;
    }

    public void resetHost(){
        this.host = "";
    }

    public ArrayList<String> getBlacklist(){
        return this.blacklist;
    }

    public HashMap<String, ArrayList<ServerThread>> getRoomlist(){
        return this.roomlist;
    }

    public boolean getQuitFlag(){
        return this.quitFlag;
    }

    public void setQuitFlag(boolean flag){
        this.quitFlag = flag;
    }

    public void sendJson(JSONObject json, ServerThread thread){
        try {
            if(!thread.getSocket().isClosed()) {
                System.out.println("****message sent****: " + json.toString());
                OutputStream outputStream = thread.getSocket().getOutputStream();
                OutputStreamWriter writer = new OutputStreamWriter(outputStream, "UTF-8");
                writer.write(json.toString() + '\n');
                writer.flush();
            } else{
                System.out.println("socket closed in sendJson");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void quit(){
        this.removeThread(this.cpThread);
        this.cpThread.closeSocket();
        this.id = "";
        //this.cpThread.interrupt();
        this.cpThread = null;
        this.setRoom("");
        this.setQuitFlag(false);

    }

    public int getMigrateCount(){
        return this.migrateCount;
    }

    public void setMigrateCount(int migrateCount) {
        this.migrateCount = migrateCount;
    }

    public void addMigrateCount(){
        this.migrateCount += 1;
    }

    public void resetThread(){
        this.cpThread = null;
    }

    public void setAddr() {
        try {
            this.addr = InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            e.printStackTrace();
            System.out.println("unable to find local address");
            System.exit(0);
        }
    }
    
    public InetAddress getAddr(){
        return this.addr;
    }

    public static void main(String[] args) {

        //ChatPeer cp = new ChatPeer();
        ChatPeer cp = new ChatPeer(Integer.parseInt(args[0]), Integer.parseInt(args[1]));


        cp.setAddr();
        System.out.println("ADDR:---- " + cp.getAddr().getHostAddress());
        cp.setHost(cp.getAddr().getHostAddress() + ":" + cp.getInPort());



        System.out.println("------");

        System.out.println("start chatpeer");
        Server server = new Server(cp);
        server.start();
        System.out.println("Server listens on: " + cp.getHost());

        System.out.println("start client");
        Scanner sc = new Scanner(System.in);
        System.out.println();
        ClientProtocol clHandler = new ClientProtocol(cp);
        while(true){
            System.out.print("(inChatPeer)["+cp.getRoom()+"] " + cp.getClientId() + ">");
            String keyboardIn;
            try
            {
                keyboardIn = sc.nextLine();
            } catch (NoSuchElementException e){
                break;
            }
            clHandler.handleClientIn(keyboardIn);
        }

        if (cp.getCurrentClientThread() != null) {
            clHandler.handleClientIn("#quit");
        }

        for(ServerThread thread: cp.getThreadSet()){
            try {
                thread.closeSocket();
            } catch (NullPointerException e){
                e.printStackTrace();
            }
        }
        server.closeSocket();
        System.exit(0);
    }
}
