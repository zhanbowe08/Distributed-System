import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;

import org.json.JSONArray;
import org.json.JSONObject;

public class SearchThread extends Thread{
    private ChatPeer cp;

    public SearchThread(ChatPeer cp){
        this.cp = cp;
    }

    public void run(){
        ArrayList<String> visited = new ArrayList<String>();
        Queue<String> unvisited = new LinkedList<String>();
        HashMap<String, JSONObject> result = new HashMap<String, JSONObject>();
        JSONObject jslist = new JSONObject();
        JSONObject jsneighbors = new JSONObject();
        JSONObject jsquit = new JSONObject();
        JSONObject jshostchange = new JSONObject();

        jsneighbors.put("type", "listneighbors");
        jslist.put("type", "list");
        jsquit.put("type", "quit");
        jshostchange.put("type", "hostchange");
        jshostchange.put("host", cp.getHost());

        System.out.println(cp.getHost());
        visited.add(cp.getHost());
        for(ServerThread peer: cp.getThreadSet()){
            if(!visited.contains(peer.getHostId())){
                visited.add(peer.getHostId());
                unvisited.offer(peer.getHostId());
            }
        }

        while(!unvisited.isEmpty()){
            System.out.println("-------------" + unvisited.toString());
            String addressPort = unvisited.poll();

            InputStream inputStream = null;
            InputStreamReader inputStreamReader = null;
            BufferedReader bufferedReader = null;
            OutputStream outputStream = null;
            OutputStreamWriter writer = null;
            try {
                Socket sc = new Socket(addressPort.split(":")[0], Integer.parseInt(addressPort.split(":")[1]));
                inputStream = sc.getInputStream();
                inputStreamReader = new InputStreamReader(inputStream, "UTF-8");
                bufferedReader = new BufferedReader(inputStreamReader);
                String info = null;

                sendJsonSearch(jshostchange, sc);
                info = bufferedReader.readLine();

                sendJsonSearch(jslist, sc);
                info = bufferedReader.readLine();
                JSONObject listreturn = new JSONObject(info.strip());
                result.put(addressPort, listreturn);

                sendJsonSearch(jsneighbors, sc);
                info = bufferedReader.readLine();
                JSONObject neighborsreturn = new JSONObject(info.strip());
                String ids = neighborsreturn.get("neighbors").toString();
                String ids1 = ids.replace('[', ' ');
                String ids2 = ids1.replace(']', ' ');
                String ids3 = ids2.replace('\"', ' ');
                String ids4 = ids3.replaceAll(" ", "");
                String[] idss = ids4.split(",");

                String nowhost = sc.getLocalAddress().toString().replaceAll("/", "")
                        + ":" + cp.getInPort();
                System.out.println("nowhost: " + nowhost);
                for(String host: idss){
                    if(!visited.contains(host) && !nowhost.equals(host)){
                        visited.add(host);
                        unvisited.add(host);
                    }
                }

                sendJsonSearch(jsquit, sc);
                info = bufferedReader.readLine();
                sc.close();
                inputStream.close();
                inputStreamReader.close();
                bufferedReader.close();





            } catch (IOException e) {
                e.printStackTrace();
                System.out.print("(inSearchexception)["+cp.getRoom()+"] " + cp.getClientId() + ">");
            }
        }
        System.out.println();
        for(String host: result.keySet()){
            System.out.println("find host: " + host);
            JSONObject json = result.get(host);
            System.out.println("show room list");
            String.valueOf(json.get("roomlist"));
            JSONArray roomlist = new JSONArray(json.get("roomlist").toString());
            for (int i = 0; i < roomlist.length(); i++) {
                String roomid = roomlist.getJSONObject(i).getString("roomid");
                String count = roomlist.getJSONObject(i).get("count").toString();
                System.out.println("Room " + roomid + " has " + count + " members.");
            }
        }
        System.out.print("(inSearch)["+cp.getRoom()+"] " + cp.getClientId() + ">");

    }

    private void sendJsonSearch(JSONObject json, Socket socket){
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
