import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;

public class ServerProtocol {
    private ChatPeer cp;
    private ServerThread thread;

    public ServerProtocol(ChatPeer cp){
        this.cp = cp;
    }

    public ServerProtocol(ChatPeer cp, ServerThread thread){
        this.cp = cp;
        this.thread = thread;
    }

    public void handleSocketIn(String input) {
        JSONObject json = new JSONObject(input.strip());

        String command = json.getString("type");

        if (command.equals("message")) {

            if (json.has("identity")) {
                System.out.println();
                System.out.println(json.getString("identity") + " says: " + json.getString("content"));
                System.out.print("(inServerprotocolmessage)["+cp.getRoom()+"] " + cp.getClientId() + ">");
            } else if (!thread.getRoom().equals("")) {
                for (ServerThread td : cp.getRoomlist().get(thread.getRoom())) {
                    json.put("identity", thread.getOppositeId());
                    cp.sendJson(json, td);
                }
            }
        }


        //server side//
        //////////////////////////////////////////
        if (command.equals("list")) {
            roomList();
        }

        if (command.equals("join")) {
            join(json.getString("roomid"));
        }

        if (command.equals("quit")) {
            quit();
        }

        if (command.equals("who")) {
            who(json.getString("roomid"));
        }

        if (command.equals("listneighbors")) {
            listNeighbors();
        }


        //client side//
        //////////////////////////////////////////
        if (command.equals("hostchange")) {
            String host = json.getString("host");
            if(!cp.getBlacklist().contains(host)) {
                thread.setHostId(host);
                JSONObject js = new JSONObject();
                js.put("type", "roomchange");
                js.put("identity", thread.getOppositeId());
                js.put("former", "");
                js.put("roomid", "");
                cp.sendJson(js, thread);
            } else {
                cp.getThreadSet().remove(thread);
                thread.closeSocket();
                System.out.println();
                System.out.println("Successfully ban login " + thread.getOppositeId() + " by its hostname " + thread.getHostId());
                System.out.print("(inServerprotocolhoschange)["+cp.getRoom()+"] " + cp.getClientId() + ">");
            }
        }
        if (command.equals("roomlist")) {
            System.out.println("show room list");
            String.valueOf(json.get("roomlist"));
            JSONArray roomlist = new JSONArray(json.get("roomlist").toString());
            System.out.println();
            for (int i = 0; i < roomlist.length(); i++) {
                String roomid = roomlist.getJSONObject(i).getString("roomid");
                String count = roomlist.getJSONObject(i).get("count").toString();
                System.out.println("Room " + roomid + " has " + count + " members.");
            }
            System.out.print("(inServerprotocolroomlist)["+cp.getRoom()+"] " + cp.getClientId() + ">");
        }
        if (command.equals("roomchange")) {
            System.out.println();
            String identity = json.getString("identity");
            String former = json.getString("former");
            String roomid = json.getString("roomid");
            if (identity.equals(cp.getClientId())) {
                //System.out.println(cp.getClientId() + "|" + identity + "|" + thread.getOppositeId());

                if (cp.getQuitFlag() && roomid.equals("")) {
                    cp.quit();
                } else if (former.equals(roomid)) {
                    System.out.println("The requested room is invalid or non existent.");
                } else if (former.equals("")) {
                    System.out.println(identity + " moved to " + roomid);
                    thread.setRoom(roomid);
                    cp.setRoom(roomid);
                } else {
                    System.out.println(identity +
                            " moved from " + thread.getRoom() +
                            " to " + roomid);
                    thread.setRoom(roomid);
                    cp.setRoom(roomid);

                }
            } else {
                if(cp.getClientId().equals("") && former.equals("") && roomid.equals("")){
                    cp.setId(identity);
                } else {
                    if (former.equals("")) {
                        System.out.println(identity + " moved to " + roomid);
                    } else {
                        System.out.println(identity +
                                " moved from " + thread.getRoom() +
                                " to " + roomid);
                    }
                }
            }
            System.out.print("(inServerprotocolroomchange)["+cp.getRoom()+"] " + cp.getClientId() + ">");
        }
        if (command.equals("roomcontents")) {
            System.out.println();
            String ids = json.get("identities").toString();
            String ids1 = ids.replace('[', ' ');
            String ids2 = ids1.replace(']', ' ');
            String ids3 = ids2.replace('\"', ' ');
            String ids4 = ids3.replaceAll(" ", "");
            String[] idss = ids4.split(",");
            System.out.println("Room " + json.getString("roomid") + " has users:");
            for (int i = 0; i < idss.length; i++) {
                System.out.println(idss[i]);
            }
            System.out.print("(inServerprotocolroomcontents)["+cp.getRoom()+"] " + cp.getClientId() + ">");
        }
        if (command.equals("neighbors")) {
            System.out.println();
            System.out.println("Server " + thread.getHostId() + " has neighbors:");
            String ids = json.get("neighbors").toString();
            String ids1 = ids.replace('[', ' ');
            String ids2 = ids1.replace(']', ' ');
            String ids3 = ids2.replace('\"', ' ');
            String ids4 = ids3.replaceAll(" ", "");
            String[] idss = ids4.split(",");
            for(String host: idss){
                System.out.println(host);
            }
            System.out.print("(inServerprotocolneighbors)["+cp.getRoom()+"] " + cp.getClientId() + ">");
        }



    }

    private void roomList(){
        JSONObject json = new JSONObject();
        json.put("type", "roomlist");
        ArrayList<JSONObject> roomlist = new ArrayList<JSONObject>();
        for (String roomid: cp.getRoomlist().keySet()){
            JSONObject temp = new JSONObject();
            temp.put("roomid", roomid);
            temp.put("count", cp.getRoomlist().get(roomid).size());
            roomlist.add(temp);
        }
        json.put("roomlist", roomlist);
        cp.sendJson(json, thread);
    }

    private void join(String roomid){
        JSONObject json = new JSONObject();
        json.put("type", "roomchange");
        json.put("identity", thread.getOppositeId());
        if (cp.getRoomlist().containsKey(roomid)){
            ArrayList<ServerThread> room = cp.getRoomlist().get(roomid);
            if (room.contains(thread)){
                json.put("former", thread.getRoom());
                json.put("roomid", thread.getRoom());
                cp.sendJson(json, thread);
                System.out.println("already in room: " + roomid);
            } else {
                json.put("former", thread.getRoom());
                json.put("roomid", roomid);
                if(!thread.getRoom().equals("")) {
                    cp.getRoomlist().get(thread.getRoom()).remove(thread);
                    for (ServerThread st:cp.getRoomlist().get(thread.getRoom())){
                        cp.sendJson(json, st);
                    }
                }
                room.add(thread);
                //System.out.println("requestroom: " + roomid);
                thread.setRoom(roomid);

                for (ServerThread st: room){
                    cp.sendJson(json, st);
                }
            }
        } else {
            json.put("former", thread.getRoom());
            json.put("roomid", thread.getRoom());
            cp.sendJson(json, thread);
            System.out.println("nosuchroom: " + roomid);
        }
    }

    private void quit(){
        JSONObject json = new JSONObject();
        json.put("type","roomchange");
        json.put("identity", thread.getOppositeId());
        json.put("former",thread.getRoom());
        json.put("roomid","");
        if (thread.getRoom().equals("")){
            cp.sendJson(json, thread);
            cp.getThreadSet().remove(thread);
            thread.closeSocket();
        } else {
            for (ServerThread td: cp.getRoomlist().get(thread.getRoom())){
                cp.sendJson(json, td);
            }
            cp.getRoomlist().get(thread.getRoom()).remove(thread);
            cp.getThreadSet().remove(thread);
            thread.closeSocket();

        }
    }

    private void who(String roomid){
        JSONObject json = new JSONObject();
        json.put("type","roomcontents");
        json.put("roomid", roomid);


        ArrayList<String> ids = new ArrayList<String>();
        if (cp.getRoomlist().containsKey(roomid)) {
            for (ServerThread td : cp.getRoomlist().get(thread.getRoom())) {
                ids.add(td.getOppositeId());
            }
            json.put("identities", ids);
            cp.sendJson(json, thread);
        }

    }

    private void listNeighbors(){
        JSONObject json = new JSONObject();
        json.put("type","neighbors");
        ArrayList<String> neighbors = new ArrayList<String>();
        for(ServerThread thread: cp.getThreadSet()){
            if(!thread.equals(this.thread)) {
                //if(!thread.equals(this.thread) && thread.getHostId() != null) {
                neighbors.add(thread.getHostId());
            }
        }
        json.put("neighbors", neighbors);
        cp.sendJson(json, thread);

    }

    private void migrateTest(){

    }

    private void migrateDo(){

    }

}
