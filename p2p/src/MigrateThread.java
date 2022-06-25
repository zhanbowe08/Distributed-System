import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;

import org.json.JSONArray;
import org.json.JSONObject;


public class MigrateThread extends Thread{
    private ChatPeer cp;

    public MigrateThread(ChatPeer cp){
        this.cp = cp;
    }

    public void run(){
            ArrayList<String> roomidlist = new ArrayList<String>();
            for (String roomid: cp.getRoomlist().keySet()){
                roomidlist.add(roomid);
            }

            JSONObject json = new JSONObject();
            json.put("type","migrateinfo");
            json.put("roomlist", roomidlist);


    }

    private void sendJsonMigrate(JSONObject json, Socket socket){
        try {
            if(!socket.isClosed()) {
                System.out.println("message_test(sended): " + json.toString());
                OutputStream outputStream = socket.getOutputStream();
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

}
