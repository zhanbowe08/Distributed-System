import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;


import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class ChatServer {


    private int portNum;
    private boolean alive = false;
    private HashMap<String, ArrayList<ClientHandler>> roomInfo = new HashMap<String, ArrayList<ClientHandler>>();
    private final String DEFAULT_ROOM_NAME ="MainHall";
    private Map<String, String> owner = new ConcurrentHashMap<String, String>();
    private String existRoom =null;
    private HashMap<String, ArrayList<ClientHandler>> roomExists = new HashMap<String, ArrayList<ClientHandler>>();



    ChatServer()
    {
        this.portNum = 4444;
    }
    ChatServer(int portNum)
    {
        this.portNum = portNum;
    }


    //handle the client
    private void handler()
    {
        ServerSocket serverSocket;
        try
        {
            System.out.println("waiting to connect!");
            serverSocket = new ServerSocket(portNum);
            alive = true;
            int count = 0;
            while(alive)
            {
                Socket socket = serverSocket.accept();
                count ++;
                String name = "guest"+count;
                ClientHandler clientHandler = new ClientHandler(socket, name, DEFAULT_ROOM_NAME);
                createMainHall(DEFAULT_ROOM_NAME, clientHandler, " ");
                clientHandler.start();
            }
        } catch (IOException e)
        {
            alive = false;
        }
    }



    // create mainHall if mainHall doesn't exist
    private void createMainHall(String roomName, ClientHandler socket, String roomOwner)
    {
        synchronized (roomInfo)
        {
            if (roomInfo.get(roomName) == null)
            {
                roomInfo.put(roomName, new ArrayList<ClientHandler>());
                owner.put("MainHall", roomOwner);
            }
            roomInfo.get(roomName).add(socket);
        }
    }


    class  ClientHandler extends Thread
    {
        private Socket socket;
        private BufferedReader reader;
        private BufferedWriter writer;
        private boolean connectionAlive;
        private String socketName;
        private String roomID;
        private boolean flag;

        public ClientHandler(Socket socket, String socketName, String roomName) throws IOException
        {
            this.socket = socket;
            this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
            this.writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"));
            this.socketName = socketName;
            this.roomID = roomName;
            this.connectionAlive = true;
            this.flag = true;
        }

        //SET the client name
        public void setSocketName(String name)
        {
            socketName = name;
        }

        //GET the client name
        public String getSocketName()
        {
            return socketName;
        }

        public Socket getSocket()
        {
            return this.socket;
        }

        // override the run method
        public void run()
        {
            System.out.println("thread come in");
            String welcomeMsg = WelcomeMsg(this);

            sendMsg(welcomeMsg+"\n");
            JSONObject msg = JsonUtil.join("roomchange",this.getSocketName()," ", "MainHall");

            String message = msg.toJSONString();
            try
            {
                broadcastNotSelf(message, this, "MainHall");
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }


            while(connectionAlive)
            {

                try {
                    receiveMsg();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }


        }

        //check if the client is the owner of the room
        private boolean checkOwner(String client)
        {
            for(String room : owner.keySet())
            {
                if(client.equals(owner.get(room)))
                {
                    return true;
                }
            }
            return false;
        }

        //get welcome message when the client join in
        private String WelcomeMsg(ClientHandler client)
        {

            JSONObject identity = new JSONObject();
            JSONObject roomList = new JSONObject();
            JSONObject roomContent = new JSONObject();
            ArrayList<String> names = new ArrayList<>();
            ArrayList<ClientHandler> clients = null;
            for (String each : roomInfo.keySet())
            {
                if (each.equals(DEFAULT_ROOM_NAME))
                {
                    clients = roomInfo.get(each);
                    //System.out.println(clients);
                    for (ClientHandler eachClient:clients)
                    {
                        //System.out.println(eachClient.getSocketName());
                        if(checkOwner(eachClient.getSocketName()))
                        {
                            names.add(eachClient.getSocketName()+"*");
                        }
                        else
                        {
                            names.add(eachClient.getSocketName());
                        }
                    }
                }
            }
            identity = JsonUtil.newIdentity("newidentity", client.getSocketName());
            roomList = JsonUtil.list("roomlist",roomInfo);
            roomContent = JsonUtil.roomContent("roomcontents", DEFAULT_ROOM_NAME,names," ");
            String welcomeMsg = identity.toJSONString()+";"+roomList.toJSONString()+";"+roomContent.toJSONString();
            //String [] welcomeMsg = {identity.toJSONString(),roomList.toJSONString(),roomContent.toJSONString() };
            return welcomeMsg;
        }

        //send message to client
        private void sendMsg(String message) {
            try {
                writer.write(message); // json
                writer.flush();
            } catch (IOException e) {
                //System.out.println("The client "+this.getSocketName()+"disconnected");
                this.interrupt();
            }


        }

        //receive the message from client
        private void receiveMsg() throws IOException {

            try
            {
                String read = null;
                if((read = reader.readLine()) != null)
                {
                    System.out.println(read);
                    JSONParser parser = new JSONParser();
                    JSONObject json = new JSONObject();
                    json = (JSONObject) parser.parse(read);
                    JSONObject object = null;

                    String type = (String) json.get("type");
                    if(!type.equals("message"))
                    {
                        if(type.equals("identitychange"))// check if the same name
                        {
                            String newName = (String) json.get("identity");
                            String originalName = this.getSocketName();
                            String room = roomClient(this);
                            if(checkIdentity(newName))
                            {
                                this.setSocketName(newName);
                                object = JsonUtil.identityChange("newidentity", originalName, newName);
                                this.broadcast(object.toJSONString(),this,room);
                            }
                            else
                            {
                                newName = originalName;
                                object = JsonUtil.identityChange("newidentity", originalName, newName);
                                sendMsg(object.toJSONString()+"\n");
                            }

                        }
                        if (type.equals("who"))
                        {
                            String roomName =  (String) json.get("roomid");
                            ArrayList<String> clientName = new ArrayList<>();
                            ArrayList<ClientHandler> clients = null;
                            String roomOwner = null;
                            for (String room : roomInfo.keySet())
                            {
                                if (room.equals(roomName))
                                {
                                    clients = roomInfo.get(room);
                                    roomOwner = owner.get(room);
                                    for (ClientHandler client:clients)
                                    {
                                        clientName.add(client.getSocketName());
                                    }
                                }
                            }
                            object = JsonUtil.roomContent("roomcontents", roomName, clientName, roomOwner);
                            this.sendMsg(object.toJSONString()+"\n");
                        }

                        if (type.equals("list"))
                        {
                            //System.out.println("list method call");
                            object = JsonUtil.list("roomlist", roomInfo);
                            this.sendMsg(object.toJSONString()+"\n");
                            //System.out.println(object.toJSONString());
                        }

                        if(type.equals("createroom"))
                        {
                            String roomName = (String) json.get("roomid");
                            object = createOtherHall(roomName, this);
                            this.sendMsg(object.toJSONString()+"\n");

                        }

                        if (type.equals("join"))
                        {
                            String newRoomName =  (String) json.get("roomid");
                            String originalRoom = roomClient(this);
                            String clientName = this.getSocketName();


                            if(!roomInfo.containsKey(newRoomName))
                            {
                                object = JsonUtil.join("roomchange",clientName,originalRoom, null);
                                this.sendMsg(object.toJSONString()+"\n");
                            }
                            else
                            {
                                replaceRoom(newRoomName, this);
                                if(!originalRoom.equals(DEFAULT_ROOM_NAME))
                                {
                                    if(roomInfo.get(originalRoom).size() == 0 && owner.get(originalRoom).equals(" "))
                                    {
                                        roomInfo.remove(originalRoom);
                                    }
                                }
                                object = JsonUtil.join("roomchange",clientName,originalRoom, newRoomName);
                                if(newRoomName.equals(DEFAULT_ROOM_NAME))
                                {
                                    ArrayList<ClientHandler>clients = roomInfo.get(DEFAULT_ROOM_NAME);
                                    ArrayList<String>identities = new ArrayList<>();
                                    for(ClientHandler client:clients)
                                    {
                                        identities.add(client.getSocketName());
                                    }
                                    JSONObject roomContents = JsonUtil.roomContent("roomcontents",
                                            DEFAULT_ROOM_NAME,identities, owner.get(DEFAULT_ROOM_NAME));
                                    JSONObject list = JsonUtil.list("roomlist", roomInfo);
                                    String out = object.toJSONString()+";"+roomContents.toJSONString()+";"+
                                            list.toJSONString();
                                    broadcast(out, this, DEFAULT_ROOM_NAME);
                                }
                                else
                                {
                                    broadcast(object.toJSONString(), this, newRoomName);
                                    broadcast(object.toJSONString(), this, originalRoom);
                                }
                            }

                        }

                        if(type.equals("delete"))
                        {
                            String roomName = (String) json.get("roomid");
                            ArrayList<ClientHandler> clients = new ArrayList<>();
                            ArrayList<String> mainHall = new ArrayList<>();
                            JSONObject list;
                            String roomOwner = null;
                            JSONObject change;
                            HashMap<String, ArrayList<ClientHandler>>roomHolder = new HashMap<String,
                                    ArrayList<ClientHandler>>();
                            JSONObject contents;
                            for(String room : roomInfo.keySet())
                            {
                                roomHolder.put(room, roomInfo.get(room));
                            }
                            synchronized (roomInfo)
                            {
                                if(roomInfo.containsKey(roomName)&& !roomName.equals(DEFAULT_ROOM_NAME))
                                {
                                    for(ClientHandler client:roomInfo.get(roomName))
                                    {
                                        clients.add(client);
                                    }
                                    for(ClientHandler client:roomInfo.get(DEFAULT_ROOM_NAME))
                                    {
                                        mainHall.add(client.getSocketName());
                                    }

                                    roomOwner = owner.get(roomName);
                                    if(roomOwner.equals(this.getSocketName()))
                                    {
                                        if(roomInfo.get(roomName).size()>0)
                                        {
                                            boolean flag = false;
                                            for(ClientHandler client :clients)
                                            {

                                                roomInfo.get(roomName).remove(client);
                                                roomInfo.get(DEFAULT_ROOM_NAME).add(client);
                                                mainHall.add(client.getSocketName());
                                                change = JsonUtil.join("roomchange", client.getSocketName(),
                                                        roomName, DEFAULT_ROOM_NAME);
                                                contents = JsonUtil.roomContent("roomcontents",DEFAULT_ROOM_NAME
                                                ,mainHall, " ");
                                                if(client.getSocketName().equals(this.getSocketName()))
                                                {
                                                    flag = true;
                                                    list = JsonUtil.list("roomlist", roomInfo);
                                                    String send = change.toJSONString()+";"+list.toJSONString()+";"+
                                                            contents.toJSONString();
                                                    this.sendMsg(send+"\n");
                                                }
                                                else
                                                {
                                                    roomHolder.remove(roomName);
                                                    list = JsonUtil.list("roomlist", roomHolder);
                                                    String send = change.toJSONString()+";"+list.toJSONString()+";"+
                                                    contents.toJSONString();
                                                    client.sendMsg(send+"\n");
                                                }

                                                try
                                                {
                                                    broadcastNotSelf(change.toJSONString(), client, DEFAULT_ROOM_NAME);
                                                }
                                                catch (IOException e)
                                                {
                                                    e.printStackTrace();
                                                }
                                            }
                                            roomInfo.remove(roomName);
                                            owner.remove(roomName);
                                            if(!flag)
                                            {
                                                //change = JsonUtil.join("roomchange", this.getSocketName(),
                                                        //roomName, DEFAULT_ROOM_NAME);
                                                list = JsonUtil.list("roomlist", roomInfo);
                                                String send = list.toJSONString();
                                                this.sendMsg(send+"\n");
                                            }
                                        }
                                        else
                                        {
                                            roomInfo.remove(roomName);
                                            owner.remove(roomName);
                                            JSONObject roomList = JsonUtil.list("roomlist", roomInfo);
                                            this.sendMsg(roomList+"\n");
                                        }

                                    }
                                    else
                                    {
                                        JSONObject roomChange = null;
                                        JSONObject roomList = null;
                                        roomChange = JsonUtil.join("roomchange", this.getSocketName(),
                                                roomName, roomName);
                                        roomList = JsonUtil.list("roomlist", roomInfo);
                                        String send = roomChange.toJSONString()+";"+roomList.toJSONString();
                                        this.sendMsg(send+"\n");
                                    }
                                }
                                else
                                {
                                   JSONObject delete = JsonUtil.join("roomchange", this.getSocketName(),
                                            roomName, roomName);
                                   this.sendMsg(delete.toJSONString()+"\n");
                                }
                            }

                        }

                        if(type.equals("quit"))
                        {
                            String roomName = roomClient(this);

                            synchronized (roomInfo)
                            {

                                JSONObject quit = JsonUtil.join("roomchange",this.getSocketName(),
                                        roomName, " ");
                                broadcast(quit.toJSONString(), this, roomName);
                                //Boolean roomOwner=false;
                                for(String room : roomInfo.keySet())
                                {
                                    if(room.equals(roomName))
                                    {
                                        roomInfo.get(room).remove(this);

                                    }
                                }
                                for(String each : owner.keySet())
                                {
                                    if(owner.get(each).equals(this.getSocketName()))
                                    {
                                        owner.replace(each," ");
                                        if(roomInfo.get(each).size() ==0)
                                        {
                                            System.out.println("reomove room "+each);
                                            roomInfo.remove(each);
                                        }
                                    }
                                }
                                for(String room : roomInfo.keySet())
                                {
                                    if(roomInfo.get(room).size() == 0 && owner.get(room).equals(" "))
                                    {
                                        roomInfo.remove(room);
                                    }
                                }
                            }

                        }
                    }
                    else
                    {
                        //System.out.println(read);
                        String roomName = roomClient(this);
                        JSONObject message = new JSONObject();
                        String content = (String) json.get("content");
                        message = JsonUtil.message("message",this.getSocketName(),content);
                        //System.out.println(message.toJSONString());
                        broadcast(message.toJSONString(), this, roomName);
                    }
                }
            } catch (IOException | ParseException e)
            {
                String roomName = roomClient(this);
                JSONObject quit = JsonUtil.join("roomchange",this.getSocketName(),
                        roomName, " ");
                broadcastNotSelf(quit.toJSONString(), this, roomName);

                for(String room : roomInfo.keySet())
                {
                    if(room.equals(roomName))
                    {
                        roomInfo.get(room).remove(this);
                    }
                }
                for(String each : owner.keySet())
                {
                    if(owner.get(each).equals(this.getSocketName()))
                    {
                        owner.replace(each," ");
                        if(roomInfo.get(each).size() ==0)
                        {
                            roomInfo.remove(each);
                        }
                    }
                }
                for(String room : roomInfo.keySet())
                {
                    if(roomInfo.get(room).size() == 0 && owner.get(room).equals(" "))
                    {
                        roomInfo.remove(room);
                    }
                }
                connectionAlive = false;
                //flag = false;
            }

        }

        //create another hall except MainHall
        private JSONObject createOtherHall(String roomName, ClientHandler socket) throws IOException {
            JSONObject object = new JSONObject();
            synchronized (roomInfo)
            {
                if(checkRoomName(roomName))
                {
                    if (!roomInfo.containsKey(roomName))
                    {
                        roomInfo.put(roomName, new ArrayList<ClientHandler>());
                        owner.put(roomName, socket.getSocketName());
                        object = JsonUtil.list("roomlist", roomInfo);
                    }
                    else
                    {
                        for(String room : roomInfo.keySet())
                        {
                            roomExists.put(room, roomInfo.get(room));
                        }
                        roomExists.remove(roomName);
                        object = JsonUtil.list("roomlist", roomExists);
                    }
                }
                else
                {
                    object = JsonUtil.list("roomlist", roomInfo);
                }



            }
            return object;
        }

        //replace the room if client wants to join another room
        private void replaceRoom(String room, ClientHandler socket)
        {
            synchronized (roomInfo)
            {
                for (String eachRoom : roomInfo.keySet())
                {

                    roomInfo.get(eachRoom).removeIf(client -> client.equals(socket));
                    String roomNameRemove = eachRoom;

                }
                String roomNameAdd = room;
                roomInfo.get(room).add(socket);
            }
        }

        //check which room belongs to which client
        private String roomClient(ClientHandler client)// check which client belongs to which room
        {
            String roomName = null;
            synchronized (roomInfo) {
                for (String eachRoom : roomInfo.keySet()) {
                    for (ClientHandler each : roomInfo.get(eachRoom)) {
                        if (each == client)
                        {
                            roomName = eachRoom;
                        }
                    }
                }
            }
            return roomName;
        }

        //check if the identity is valid or not
        private Boolean checkIdentity(String name)
        {
            String expression = "[a-zA-Z]+$";
            Pattern pattern = Pattern.compile(expression);
            Matcher matcher = pattern.matcher(name);
            if(matcher.matches())
            {
                if(name.length()>=3 && name.length()<=16)
                {
                    if(Pattern.compile("^[a-zA-Z]").matcher(name).find())
                    {
                        return true;
                    }
                }
            }
            return false;
        }

        //check if the room name is valid or not
        private Boolean checkRoomName(String roomName)
        {
            String expression = "[a-zA-Z]+$";
            Pattern pattern = Pattern.compile(expression);
            Matcher matcher = pattern.matcher(roomName);
            if(matcher.matches())
            {
                if(roomName.length()>=3 && roomName.length()<=32)
                {
                    if(Pattern.compile("^[a-zA-Z]").matcher(roomName).find())
                    {
                        return true;
                    }
                }
            }
            return false;
        }

        //broadcast the message including self
        private void broadcast(String message, ClientHandler client, String roomName) throws IOException
        {
            //System.out.println(message+" " +roomName);
            ArrayList<ClientHandler> clientHolder;
            synchronized (roomInfo)
            {
                clientHolder = roomInfo.get(roomName);
                //System.out.println(clientHolder.size());
                for (ClientHandler each : clientHolder)
                {
                    // check if the socket connected

                    each.sendMsg(message+"\n");

                }
            }
        }

        //broadcast the message excluding self
        private void broadcastNotSelf(String message, ClientHandler client, String roomName) throws IOException
        {
            //System.out.println(message+" " +roomName);
            ArrayList<ClientHandler> clientHolder;
            synchronized (roomInfo)
            {
                clientHolder = roomInfo.get(roomName);
                if(clientHolder!=null)
                {
                    for (ClientHandler each : clientHolder)
                    {
                        if(!each.equals(client))
                        {
                            each.sendMsg(message+"\n");
                        }

                    }
                }
                else
                {
                    return;
                }
            }
        }


    }

    public static void main(String[] args)
    {
        //check the number of args and assign the value
        if(args.length==2)
        {
            int port = Integer.parseInt(args[1].trim());
            ChatServer server = new ChatServer(port);
            server.handler();
        }
        else if(args.length==0)
        {
            ChatServer server = new ChatServer();
            server.handler();
        }
        else
        {
            System.out.println("please only provide valid port number");
        }

    }
}
