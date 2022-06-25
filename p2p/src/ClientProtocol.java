import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;

public class ClientProtocol {
    private ChatPeer cp;

    public ClientProtocol(ChatPeer cp){
        this.cp = cp;
    }

    public void handleClientIn(String input) {
        String[] ins = input.split(" ");
        if (cp.getCurrentClientThread() == null && false) {
            connect(ins);
        } else {
            if (ins[0].charAt(0) == '#') {
                if (ins[0].equals("#join")) {
                    join(ins);
                } else if (ins[0].equals("#createroom")){
                    createRoom(ins);
                } else if (ins[0].equals("#list")){
                    list();
                } else if (ins[0].equals("#connect")){ //////////////////
                    connect(ins);//////////////////
                    //////////////////
                } else if (ins[0].equals("#delete")){
                    delete(ins);
                } else if (ins[0].equals("#quit")){
                    quit();
                } else if (ins[0].equals("#who")){
                    who(ins);
                } else if (ins[0].equals("#listneighbors")){
                    listNeighbors();
                } else if (ins[0].equals("#searchnetwork")){
                    search();
                } else if (ins[0].equals("#kick")){
                    kick(ins);
                } else if (ins[0].equals("#migrate")){
                    migrate(ins);
                }else if (ins[0].equals("#ls")){
                    System.out.println("active thread:");
                    for (ServerThread td: cp.getThreadSet()){
                        System.out.println("---- Opposite id" + td.getOppositeId() + " hostname " + td.getHostId());
                    }
                }








                else {
                    System.out.println("wrong format");
                }

            } else {
                try {
                    message(input);


                    //for(Socket socket: cp.getNeighbors()) {
                    //    OutputStream outputStream = socket.getOutputStream();
                    //    OutputStreamWriter writer = new OutputStreamWriter(outputStream);
                    //    writer.write(input + '\n');
                    //    writer.flush();
                        //cp.getSocket().shutdownOutput();
                        //message(ins);
                    //}
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }


    private void connect(String[] ins){
        if (ins[0].equals("#connect") && ins.length == 2){
            try {
                String[] address = ins[1].split(":");
                Integer port = Integer.valueOf(address[1]);
                cp.cpConnect(address[0], port, -1);
            } catch (Exception e) {
                System.out.println("args not match");
            }
            //if (cp.getSocket() != null){
            //    cp.addSocket(cp.getSocket());
            //}
        } else if (ins[0].equals("#connect") && ins.length == 3) {
            try {
                String[] address = ins[1].split(":");
                Integer port = Integer.valueOf(address[1]);
                cp.cpConnect(address[0], port, Integer.valueOf(ins[2]));
            } catch (Exception e) {
                System.out.println("args not match");
            }
        } else {
            System.out.println("Not connect yet!");
        }

    }


    private boolean checkAllLetter(String s) {

        char[] ss = s.toCharArray();
        for (char c : ss) {
            if (!((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z'))) {
                return false;
            }
        }
        return true;
    }

    private void createRoom(String[] ins){
        if (ins.length != 2){
            System.out.println("Wrong format");
        } else {
            if (checkAllLetter(ins[1]) && ins[1].length() >= 3 && ins[1].length() <= 32){
                if (!cp.getRoomlist().containsKey(ins[1])){
                    cp.getRoomlist().put(ins[1], new ArrayList<ServerThread>());
                    System.out.println("Room " + ins[1] +" created.");
                } else {
                    System.out.println("Room " + ins[1] +" is invalid or already in use.");
                }
            } else {
                System.out.println("Room " + ins[1] +" is invalid or already in use.");
            }
        }
    }

    private void delete(String[] ins){

        if (cp.getRoomlist().containsKey(ins[1])) {
            ArrayList<ServerThread> threads = cp.getRoomlist().get(ins[1]);
            for (int i = 0; i < threads.size(); i ++){
                ServerThread thread = threads.get(i);
                JSONObject json = new JSONObject();
                json.put("type","roomchange");
                json.put("identity", thread.getOppositeId());
                json.put("former", ins[1]);
                json.put("roomid", "");
                cp.sendJson(json, thread);

            }
            cp.getRoomlist().get(ins[1]).clear();
            cp.getRoomlist().remove(ins[1]);
            System.out.println("Delete successfully.");
        } else {
            System.out.println("Delete denied.");
        }

    }

    private void list() {
        JSONObject json = new JSONObject();
        json.put("type","list");
        cp.sendJson(json, cp.getCurrentClientThread());
    }

    private void join(String[] ins){
        if (ins.length != 2){
            System.out.println("Wrong format");
        } else {
            JSONObject json = new JSONObject();
            json.put("type", "join");
            json.put("roomid", ins[1]);
            cp.sendJson(json, cp.getCurrentClientThread());
        }
    }

    private void quit(){
        JSONObject json = new JSONObject();
        json.put("type", "quit");
        cp.sendJson(json, cp.getCurrentClientThread());
        cp.setQuitFlag(true);
    }

    private void message(String input){
        JSONObject json = new JSONObject();
        json.put("type", "message");
        json.put("content", input);
        cp.sendJson(json, cp.getCurrentClientThread());
    }

    private void who(String[] ins){
        if (ins.length != 2){
            System.out.println("Wrong format");
        } else {
            JSONObject json = new JSONObject();
            json.put("type", "who");
            json.put("roomid", ins[1]);
            cp.sendJson(json, cp.getCurrentClientThread());
        }
    }

    private void listNeighbors(){
        JSONObject json = new JSONObject();
        json.put("type", "listneighbors");
        cp.sendJson(json, cp.getCurrentClientThread());
    }

    private void search(){
        SearchThread st = new SearchThread(this.cp);
        st.start();
    }

    private void kick(String[] ins){
        if(ins.length == 2){
            String oppositeId = ins[1];
            ServerThread thread = null;
            for (ServerThread td: cp.getThreadSet()){
                if (td.getOppositeId().equals(oppositeId)){
                    thread = td;
                    break;
                }
            }
            if(thread != null){
                cp.getThreadSet().remove(thread);
                if(!thread.getRoom().equals("")){
                    cp.getRoomlist().get(thread.getRoom()).remove(thread);
                }
                thread.closeSocket();
                cp.getBlacklist().add(thread.getHostId());
                System.out.println("Successfully ban " + oppositeId + " by its hostname " + thread.getHostId());
            } else {
                System.out.println("no user " + oppositeId);
            }

        }
    }

    private void migrate(String[] ins){
        if(ins.length != 2){
            System.out.println("args not match");
        } else {
            cp.setMigrateCount(0);
        }
    }

}
