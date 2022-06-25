import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
/*
 This class is responsible for Json type data
 */
public class JsonUtil {

    //deal with newIdentity
    public static JSONObject newIdentity(String type, String identity)
    {
        JSONObject jsonObj = new JSONObject();
        jsonObj.put("type", type);
        jsonObj.put("former","");
        jsonObj.put("identity", identity);
        return jsonObj;
    }

    //deal with identitychange
    public static JSONObject identityChange(String type, String former, String identity)
    {
        JSONObject jsonObj = new JSONObject();
        jsonObj.put("type", type);
        jsonObj.put("former",former);
        jsonObj.put("identity", identity);
        return jsonObj;
    }

    //deal with roomcontent
    public static JSONObject roomContent(String type, String roomId, ArrayList<String> clients,
                                         String owner)
    {
        JSONObject jsonObj = new JSONObject();
        jsonObj.put("type", type);
        jsonObj.put("roomid", roomId);
        jsonObj.put("identities", clients);
        jsonObj.put("owner", owner);
        return jsonObj;
    }

    //deal with join
    public static JSONObject join(String type, String identity, String formerRoom, String roomId)
    {
        JSONObject jsonObj = new JSONObject();
        jsonObj.put("type", type);
        jsonObj.put("identity", identity);
        jsonObj.put("former", formerRoom);
        jsonObj.put("roomid", roomId);
        return jsonObj;
    }

    // deal with message
    public static JSONObject message(String type, String identity, String content)
    {
        JSONObject jsonObj = new JSONObject();
        jsonObj.put("type", type);
        jsonObj.put("identity", identity);
        jsonObj.put("content", content);
        return jsonObj;
    }

    //deal with list
    public static JSONObject list(String type, HashMap<String, ArrayList<ChatServer.ClientHandler>>rooms)
    {
        JSONObject jsonObj = new JSONObject();
        jsonObj.put("type", type);
        JSONArray array = new JSONArray();
        for(String room : rooms.keySet())
        {
            JSONObject eachRoom = new JSONObject();
            eachRoom.put("roomid", room);
            eachRoom.put("count", rooms.get(room).size());
            array.add(eachRoom);
        }
        jsonObj.put("rooms", array);

        return jsonObj;
    }

}
