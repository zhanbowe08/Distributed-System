import java.io.*;
import java.net.Socket;

import org.json.JSONObject;

public class ServerThread extends Thread{
    private Socket socket = null;
    private String oppositeId;
    private String hostId;
    private ChatPeer cp;
    private String room;
    private int testNum;
    private boolean alive;
    private boolean asClientConnection;
    private Socket migrateSocket = null;

    public ServerThread(ChatPeer cp, int num){
        testNum = num;
    }

    public ServerThread(ChatPeer cp, Socket socket, String id) {
        this.socket = socket;
        this.cp = cp;
        this.room = "";
        this.alive = true;
        this.asClientConnection = false;
        this.oppositeId = id;
    }

    public Socket getSocket() {
        return this.socket;
    }

    public String getOppositeId(){
        return this.oppositeId;
    }

    public String getHostId(){
        return this.hostId;
    }

    public void setHostId(String hostId){
        this.hostId = hostId;
    }

    public void closeSocket(){
        try {
            this.socket.close();
            this.alive = false;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getRoom(){
        return this.room;
    }

    public void setRoom(String roomid){
        this.room = roomid;
    }

    public boolean isAsClientConnection(){
        return this.asClientConnection;
    }

    public void setAsClientConnection(){
        this.asClientConnection = true;
    }

    private Socket getMigrateSocket(){
        return this.migrateSocket;
    }

    @Override
    public void run() {
        //sendId();
        InputStream inputStream = null;
        InputStreamReader inputStreamReader = null;
        BufferedReader bufferedReader = null;
        OutputStream outputStream = null;
        OutputStreamWriter writer = null;
        try {
            inputStream = socket.getInputStream();
            inputStreamReader = new InputStreamReader(inputStream, "UTF-8");
            bufferedReader = new BufferedReader(inputStreamReader);
            String info = null;
            ServerProtocol serverHandler = new ServerProtocol(cp, this);

            while (this.alive && !socket.isClosed()){
                info = bufferedReader.readLine();
                //System.out.println("while loop");
                if (info != null) {

                    System.out.println("\n@@@@server receive@@@@: " + info.toString());

                    serverHandler.handleSocketIn(info);
                    //System.out.print("(inServerThreadwhile)["+cp.getRoom()+"] " + cp.getClientId() + ">");

                } else {
                    //System.out.println("while loop break");

                    if(asClientConnection){
                        System.out.println("server connection lost");
                        this.closeSocket();
                        cp.removeThread(this);
                        cp.setRoom("");
                        cp.resetId();
                        cp.resetHost();
                        cp.resetThread();
                    } else {
                        if(!this.getRoom().equals("")){
                            cp.getRoomlist().get(this.getRoom()).remove(this);
                            JSONObject json = new JSONObject();
                            json.put("type", "roomchange");
                            json.put("identity", this.getOppositeId());
                            json.put("former", this.getRoom());
                            json.put("roomid", "");
                            for(ServerThread td: cp.getRoomlist().get(this.getRoom())){
                                cp.sendJson(json, td);
                            }
                        }
                        this.closeSocket();
                        cp.removeThread(this);
                    }
                    break;
                }
            }

            //System.out.println("to end thread");
            System.out.print("(inServerThreadend)["+cp.getRoom()+"] " + cp.getClientId() + ">");
            return;
            //socket.shutdownInput();
        } catch (IOException e) {
            System.out.println("force quit--------------------------");
            System.out.print("(inServerThreadexception)["+cp.getRoom()+"] " + cp.getClientId() + ">");
            /*
            JSONObject json = new JSONObject();
            json.put("type","roomchange");
            json.put("identity", this.getOppositeId());
            json.put("former", this.getRoom());
            json.put("roomid","");
            if (this.getRoom().equals("")){
                System.out.println("quit in empty room");
                //cp.sendJson(json, this.getSocket());
                //this.closeSocket();
            } else {
                for (ServerThread td: cp.getRoomlist().get(this.getRoom())){
                    if (!td.equals(this)) {
                        cp.sendJson(json, td.getSocket());
                    }
                }
                cp.getRoomlist().get(this.getRoom()).remove(this);
                this.closeSocket();
                cp.getThreadSet().remove(this);
            }

             */
        }
        finally {
            try {
                if (writer != null) {
                    writer.close();
                }
                if (outputStream != null) {
                    outputStream.close();
                }
                if (bufferedReader != null) {
                    bufferedReader.close();
                }
                if (inputStreamReader != null) {
                    inputStreamReader.close();
                }
                if (inputStream != null) {
                    inputStream.close();
                }
                if (socket != null) {
                    socket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }


        }


    }


    public void sendHost(String hostId){
        try {
            /*
            this.id = socket.getInetAddress().toString().split("/")[1]
                    + ":"
                    + socket.getPort();

             */
            JSONObject json = new JSONObject();
            json.put("type", "hostchange");
            json.put("host", hostId);

            System.out.println("in sendHost");
            cp.sendJson(json, this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
