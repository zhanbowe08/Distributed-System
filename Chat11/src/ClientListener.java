import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ClientListener implements Runnable
{

    private Socket socket;
    private String name;
    private String room;
    BufferedWriter out;
    BufferedReader in;
    Boolean connection = true;
    Boolean inputFlag = true;
    Boolean createRoom = false;
    String createRoomName=null;

    public ClientListener(Socket socket)
    {
        this.socket = socket;
    }
    // SET the client name
    private synchronized void setName(String name)
    {
        this.name = name;
    }
    //GET the client name
    public String getName()
    {
        return this.name;
    }
    //SET the room name
    private synchronized void setRoom(String room)
    {
        this.room = room;
    }
    //GET the room name
    public String getRoom()
    {
        return this.room;
    }


    @Override
    public void run()
    {

        new Thread(()-> {
            try {
                receiveMsg();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();


        while(connection)
        {
            try
            {
                sendMsg();
                if(!inputFlag)
                {
                    break;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


    }

    //deal the incoming commands from server
    public String command(String[]read, String info)
    {

        JSONParser parser = new JSONParser();
        String output = "";
        String total = "";
        if (read.length==3)
        {
            if(info.contains("newidentity"))
            {
                for (int index = 0; index< read.length; index++)
                {
                    JSONObject json = null;
                    try
                    {

                        json = (JSONObject) parser.parse(read[index]);

                    } catch (ParseException e)
                    {
                        System.out.println("error in client listener command-newidentity");
                    }
                    String type = (String) json.get("type");
                    if(type.equals("newidentity"))
                    {
                        String clientName = (String) json.get("identity");
                        setName(clientName);
                    }
                    if(type.equals("roomlist"))
                    {
                        //System.out.println("room list function");
                        JSONArray array = (JSONArray) json.get("rooms");
                        //System.out.println(array.toJSONString());

                        for (int i =0; i<array.size(); i++)
                        {
                            JSONObject objects = (JSONObject) array.get(i);
                            String rooms = (String) objects.get("roomid");
                            //System.out.println(rooms);
                            String count = String.valueOf(objects.get("count"));
                            total += rooms+": "+count+" guests\n";
                        }
                    }
                    if(type.equals("roomcontents"))
                    {
                        //System.out.println("roomcontents");
                        String roomName = (String) json.get("roomid");
                        String allMember = "";
                        setRoom(roomName);
                        JSONArray clients = (JSONArray) json.get("identities");
                        for (Object client : clients)
                        {
                            String member = (String) client;
                            allMember += member + " ";
                        }
                        String move = getName() + " move to " +getRoom()+"\n";
                        total += move + roomName +" "+"contains "+allMember+"\n";
                    }
                }
                String message = "Connected to localhost as " + getName() +"\n";
                output += message + total;
            }
            if(info.contains("roomchange"))
            {

                String former = null;
                String roomId = null;
                String identity = null;
                String move = null;
                for (int index = 0; index < read.length; index++)
                {
                    JSONObject json = null;
                    try {

                        json = (JSONObject) parser.parse(read[index]);

                    } catch (ParseException e) {
                        System.out.println("error in client listener command-join");
                    }
                    String type = (String) json.get("type");
                    if (type.equals("roomchange"))
                    {
                        former = (String) json.get("former");
                        roomId = (String) json.get("roomid");
                        identity = (String) json.get("identity");
                        if(identity.equals(getName()))
                        {
                            setRoom(roomId);
                            move = identity + " from " + former + " " + "move to " + roomId + "\n";
                        }
                        else
                        {
                            move = identity+" move to "+roomId+"\n";
                        }
                        output += move;
                    }

                    if (type.equals("roomlist")) {
                        //System.out.println("room list function");
                        JSONArray array = (JSONArray) json.get("rooms");
                        //System.out.println(array.toJSONString());

                        for (int i = 0; i < array.size(); i++) {
                            JSONObject objects = (JSONObject) array.get(i);
                            String rooms = (String) objects.get("roomid");
                            //System.out.println(rooms);
                            String count = String.valueOf(objects.get("count"));
                            total += rooms + ": " + count + " guests\n";
                        }
                    }
                    if (type.equals("roomcontents")) {
                        //System.out.println("roomcontents");
                        String roomName = (String) json.get("roomid");
                        String allMember = "";
                        setRoom(roomName);
                        JSONArray clients = (JSONArray) json.get("identities");
                        for (Object client : clients)
                        {
                            String member = (String) client;
                            allMember += member + " ";
                        }

                        total += roomName + " " + "contains " + allMember + "\n";
                    }
                }
              output += total;
            }
        }
        if(read.length == 2)
        {
            //roomchange, roomlist
            String former = null;
            String roomId = null;
            String identity = null;
            String move = null;
            for (int index = 0; index< read.length; index++) {
                JSONObject json = null;
                try
                {

                    json = (JSONObject) parser.parse(read[index]);
                    String type = (String) json.get("type");
                    if (type.equals("roomchange"))
                    {

                        former = (String) json.get("former");
                        roomId = (String) json.get("roomid");
                        identity = (String) json.get("identity");
                        if (roomId.equals(former))
                        {
                            output += "The request room for deletion is not valid or " +
                                    "non existent.\n";
                        }
                        else
                        {
                            if(identity.equals(getName()))
                            {
                                setRoom(roomId);
                                move = identity + " from " + former + " " + "move to " + roomId + "\n";
                            }
                            else
                            {
                                move = identity+" move to "+roomId+"\n";
                            }
                            output += move;

                        }
                    }

                    if (type.equals("roomlist")) {
                        //System.out.println("room list function");
                        JSONArray array = (JSONArray) json.get("rooms");
                        //System.out.println(array.toJSONString());

                        for (int i = 0; i < array.size(); i++) {
                            JSONObject objects = (JSONObject) array.get(i);
                            String rooms = (String) objects.get("roomid");
                            String count = String.valueOf(objects.get("count"));
                            total += rooms + ": " + count + " guests\n";
                        }
                    }

                }
                catch (ParseException e)
                {
                    System.out.println("error in client listener command-delete");
                }
            }
            output += total;
        }
        if(read.length==1)
        {
            try
            {
                //System.out.println("the input is " +read[0]);
                JSONObject json = (JSONObject) parser.parse(read[0]);
                String type = (String) json.get("type");
                if(type.equals("newidentity")) // identity change
                {
                    String former = (String)json.get("former");
                    String id = (String)json.get("identity");
                    if(former.equals(this.name))
                    {
                        if (former.equals(id))
                        {
                            output+="Requested identity invalid or in use";
                        }
                        else
                        {
                            output+=former+" is now "+id;
                            setName(id);
                        }
                    }
                    else
                    {
                        output+=former+" is now "+id;
                    }

                }
                if(type.equals("roomchange")) // join
                {
                    String roomName = (String)json.get("roomid");
                    String former = (String)json.get("former");
                    String clientName = (String)json.get("identity");
                    if(roomName == null)// if the joined room is not exists
                    {
                        output += "The requested room is invalid";
                    }
                    else if(roomName.equals(" "))//quit
                    {
                        //if the client name == this, close socket
                        output += clientName+" leaves "+former+"\n";

                        if(clientName.equals(getName()))
                        {
                            System.out.println("Disconnected from localhost");
                            connection = false;
                            this.socket.close();
                            System.exit(0);
                        }

                    }
                    else if (roomName.equals(former))
                    {
                        System.out.println("Room "+former+" is invalid to delete");
                    }
                    else if(roomName.equals("MainHall")&&former.equals(" "))
                    {
                        output += clientName + " move to " +getRoom()+"\n";
                    }
                    else
                    {
                        if(clientName.equals(getName()))
                        {
                            setRoom(roomName);
                            output += clientName+" leaves "+former+"\n";
                            output += clientName+" moved from "+former+" to "+roomName;
                        }
                        else
                        {
                            if(former.equals(this.getRoom()))
                            {
                                output += clientName+" leaves "+former+"\n";
                            }
                           else
                            {
                                output += clientName+" moved from "+former+" to "+roomName;
                            }
                        }
                    }
                }
                if(type.equals("roomcontents"))//who
                {
                    String roomName = (String) json.get("roomid");
                    String allMember = "";
                    String owner = (String)json.get("owner");
                    JSONArray clients = (JSONArray) json.get("identities");
                    for (Object client : clients)
                    {
                        String member = (String) client;
                        if(member.equals(owner))
                        {
                            member+="*";
                        }
                        allMember += member + " ";
                    }
                    if(owner == null)
                    {
                        owner = " ";
                    }
                    else
                    {
                        if(owner.equals(" "))
                        {
                            owner = " ";
                        }
                    }
                    if(allMember.equals(" "))
                    {
                        allMember = "null";
                    }
                    output+= "The owner of "+roomName+" is "+owner+" contains "+allMember;
                }
                if(type.equals("roomlist")) // list
                {
                    JSONArray array = (JSONArray) json.get("rooms");
                    if(createRoom)
                    {
                        ArrayList<String> roomName = new ArrayList<>();
                        for (int i =0; i<array.size(); i++)
                        {
                            JSONObject objects = (JSONObject) array.get(i);
                            String rooms = (String) objects.get("roomid");
                            roomName.add(rooms);
                        }
                        if(!roomName.contains(createRoomName))
                        {
                            output += "Room "+createRoomName+" is invalid or already in use";
                        }
                        else
                        {
                            output += "Room "+createRoomName+" created";
                        }
                        createRoomName =null;
                        createRoom = false;
                    }
                    else
                    {
                        for (int i =0; i<array.size(); i++)
                        {
                            JSONObject objects = (JSONObject) array.get(i);
                            String rooms = (String) objects.get("roomid");
                            String count = String.valueOf(objects.get("count"));
                            output += rooms+": "+count+" guests\n";
                        }
                    }
                }
                if(type.equals("message"))
                {
                    String id = (String) json.get("identity");
                    String content = (String) json.get("content");
                    output += id+": "+content+"\n";
                }

            }
            catch (ParseException | IOException e)
            {
                e.printStackTrace();
            }

        }

        return output;
    }

    //send message to server
    public void sendMsg() throws IOException {
        if(connection)
        {

            Scanner scanner = new Scanner(System.in);
            String userInput = scanner.nextLine();
            try
            {
                out =  new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(),
                        "UTF-8"));
                JSONObject json = new JSONObject();

                String [] input = userInput.split(" ");
                String[]commands = {"identitychange", "join", "who", "list", "createroom", "delete",
                        "quit"};
                List<String> cmd = Arrays.asList(commands);
                char first = userInput.charAt(0);
                if (first == '#')
                {
                    String command = input[0].substring(1);
                    while(!cmd.contains(command))
                    {
                        System.out.println("invalid command, try again");
                        System.out.print("["+getRoom()+"] "+ getName()+"> ");
                        userInput = scanner.nextLine();
                        input = userInput.split(" ");
                        command =  input[0].substring(1);
                    }
                    if(input[0].contains("identitychange"))
                    {
                        String userName = input[1];
                        json.put("type", "identitychange");
                        json.put("identity", userName);
                    }
                    if(input[0].contains("join"))
                    {
                        String roomName = input[1];
                        json.put("type","join");
                        json.put("roomid", roomName);
                    }
                    if(input[0].contains("who"))
                    {
                        String roomName = input[1];
                        json.put("type","who");
                        json.put("roomid", roomName);
                    }
                    if (input[0].contains("list"))
                    {
                        json.put("type","list");
                    }
                    if(input[0].contains("createroom"))
                    {
                        String roomName = input[1];
                        createRoom = true;
                        createRoomName = roomName;
                        json.put("type", "createroom");
                        json.put("roomid", roomName);
                    }
                    if(input[0].contains("delete"))
                    {
                        String roomName = input[1];
                        json.put("type", "delete");
                        json.put("roomid", roomName);
                    }
                    if(input[0].contains("quit"))
                    {
                        inputFlag = false;
                        json.put("type","quit");
                        out.write(json.toString()+"\n");
                        out.flush();
                        System.exit(0);
                    }
                }
                else
                {
                    json.put("type", "message");
                    json.put("content", userInput);
                }
                out.write(json.toString()+"\n");
                out.flush();
            }
            catch (IOException e)
            {
                System.out.println("");
            }
        }
    }

    //receive message from server
    public void receiveMsg() throws IOException {
        String getInfo = null;
        String result = null;

        in = new BufferedReader(new InputStreamReader(socket.getInputStream(),
                "UTF-8"));
        while(connection)
        {
            while((getInfo = in.readLine()) != null) // receive json
            {

                //System.out.println(getInfo);
                getInfo = getInfo.trim();
                String[] info = getInfo.split(";");
                //System.out.println(info.length);
                result = command(info, getInfo);
                System.out.println(result);
                if(!connection)
                {
                    break;
                }
                System.out.print("["+getRoom()+"] "+ getName()+"> ");
            }
        }

    }


}
