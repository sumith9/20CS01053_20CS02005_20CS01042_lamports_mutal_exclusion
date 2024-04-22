import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Timestamp;
import java.util.*;

public class Client {
    private String server_address = "10.10.13.104"; // Replace with actual server address
    private int server_port = 5555;
    private Socket client;


    // client ports, addresses, sockets to connect to every other client
    private int client_ports[] = new int [] {6666,7777,8888};
    private String clients[] = new String []{
            "10.10.13.104",
            "10.10.13.104",
            "10.10.13.104"
    };
    private Socket[] coclient;
    private DataOutputStream[] douts;
    private DataInputStream[] diss;

    // file names, queue for each file, replies collected for each file
    private String[] files;
    private PriorityQueue<NodeEntry>[] fileQueues;
    private int[] replies;

    // client number
    private int node;

    public Client(String node){
        try {
            this.node = Integer.valueOf(node);
            coclient = new Socket[3];
            douts = new DataOutputStream[3];
            diss = new DataInputStream[3];

            Thread t = new ClientasServer(this, client_ports[this.node-1]);
            t.start();

            enquire();
            connectClients();
            doRandomeReadandWrites();

        }catch (Exception e){
            System.out.println("Exception occurred in Client: " + e);
        }
    }


    // enquire returns list of all files in the server
    public void enquire(){
        try {
            client = new Socket(server_address, server_port);

            DataOutputStream dout=new DataOutputStream(client.getOutputStream());
            DataInputStream dis = new DataInputStream(client.getInputStream());

            dout.writeUTF("enquire");
            dout.flush();

            String	filesList = (String)dis.readUTF();

            System.out.println("List of files in the server:");
            System.out.println(filesList);

            // loading files and initializing file queues and replies
            files = filesList.split(",");

            fileQueues = new PriorityQueue[files.length];
            for(int i=0; i < files.length; i++){
                fileQueues[i] =  new PriorityQueue<NodeEntry>();
            }

            replies = new int[files.length];
            Arrays.fill(replies, 0);

            dis.close();
            dout.close();
            client.close();
        }catch (Exception e){
            System.out.println("Exception occurred in enquire: "+ e);
        }
    }

    // connecting to all the clients except itself
    public void connectClients() throws Exception{
        for(int i = 0; i < 3 ; i++) {
            if(i!=(node-1)){
                while (true){
                    try {
                        coclient[i] = new Socket(clients[i], client_ports[i]);

                        douts[i]=new DataOutputStream(coclient[i].getOutputStream());
                        diss[i] = new DataInputStream(coclient[i].getInputStream());

                        if (coclient[i] != null) {
                            break;
                        }

                    }catch (IOException e){
                       Thread.sleep(1000);
                    }

                }
            }
        }
    }


    // generates random Read/Write requests
    public void doRandomeReadandWrites(){
        try {
            Random random = new Random();
            String operation = "Read";
            for(int i=0; i < 5; i++){
                int rand1 = random.nextInt(2);
                int rand2 = random.nextInt(files.length);

                if(rand1 == 1)
                    operation = "Write";

                System.out.println("Requesting file '"+ files[rand2] + "' for "+ operation + " operation");
                sendRequestToClients(rand2);

                // waiting till it is at the peak of the respective file queue
                boolean waiting = true;
                while(waiting){
                    int peak = fileQueues[rand2].peek().getNode();
                    if(peak==node && replies[rand2]==2){
                        if(rand1 == 0){    // read
                            try{
                                read(files[rand2]);
                            }catch (Exception e){
                                System.out.println(e);
                            }
                        }else if(rand1 == 1){ // write
                            try{
                                write(files[rand2]);
                            }catch (Exception e){
                                System.out.println(e);
                            }
                        }
                        replies[rand2]=0;
                        waiting = false;
                    }else {
                        Thread.sleep(1000);
                    }
                }

                System.out.println("Releasing file: "+ files[rand2]);
                sendReleaseMsgs(rand2);
            }

            System.out.println("Successfully performed 10 Read/Write requests!!!");
        }catch (Exception e){
            System.out.println("Exception occurred doRandomeReadandWrites: "+ e);
        }

    }

    // sending request to all clients for a file
    public void sendRequestToClients(int fileno){
        Long seconds = System.currentTimeMillis();
        //System.out.println("vgvgvgvg ");
        fileQueues[fileno].add(new NodeEntry(node, new Timestamp(seconds)));
        try {
            for(int i = 0; i < 3 ; i++) {
                if(i!=(node-1)) {
                    //System.out.println("vgvgvgvg ");
                    douts[i].writeUTF("request," + fileno + "," + node+","+seconds);
                    douts[i].flush();

                    String reply = (String) diss[i].readUTF();
                    String msgs[] = reply.split(",");

                    if (msgs[0].equals("reply"))
                        replies[Integer.valueOf(msgs[1])] += 1;
                }
            }

        }catch (Exception e){
            System.out.println("Exception occurred in sendRequestToClients: "+e);
        }
    }

    // sending release of a file to all clients
    public void sendReleaseMsgs(int fileno){
        fileQueues[fileno].poll();
        try {
            for(int i = 0; i < 3 ; i++) {
                if(i!=(node-1)) {
                    douts[i].writeUTF("release," + fileno);
                    douts[i].flush();
                }
            }
        }catch (Exception e){
            System.out.println("Exception occurred in sendReleaseMsgs: "+e);
        }
    }

    public void read(String filename) throws Exception{
      try{
          client = new Socket(server_address, server_port);

          DataOutputStream dout=new DataOutputStream(client.getOutputStream());
          DataInputStream dis = new DataInputStream(client.getInputStream());

          dout.writeUTF("read,"+filename);
          dout.flush();

          String	msg = (String)dis.readUTF();
          System.out.println("Output: "+msg);

          dis.close();
          dout.close();
          client.close();
      }catch (Exception e){
          System.out.println("Exception occurred in read: "+e);
      }
    }

    public void write(String filename){
        try{
            Timestamp timestamp =  new Timestamp(System.currentTimeMillis());
            String content = "Client "+node + ", " + timestamp.toString();
            client = new Socket(server_address, server_port); // Connect to the single server

            DataOutputStream dout = new DataOutputStream(client.getOutputStream());
            DataInputStream dis = new DataInputStream(client.getInputStream());

            dout.writeUTF("write," + filename + "," + content);
            dout.flush();

            String msg = (String) dis.readUTF();
            System.out.println(msg);

            dis.close();
            dout.close();
            client.close();
        }catch (Exception e){
            System.out.println("Exception occurred in write: "+e);
        }
    }

    public PriorityQueue<NodeEntry>[] getFileQueues() {
        return fileQueues;
    }

    public static void main(String[] args) {
        if(args.length < 1){
            System.out.println("Please provide the client number (1 to 3)");
        }else{
            new Client(args[0]);
        }
    }
}

class ClientasServer extends Thread {
    private ServerSocket client_as_server;
    private Client client;

    public ClientasServer(Client client, int port) throws Exception{
        client_as_server = new ServerSocket(port);
        this.client = client;
    }

    @Override
    public void run() {
        try {
            while(true){
                Socket  co_client = client_as_server.accept();

                DataInputStream dis = new DataInputStream(co_client.getInputStream());
                DataOutputStream dout=new DataOutputStream(co_client.getOutputStream());

                Thread t = new CoClientHandler(co_client, dis, dout, client);
                t.start();
            }
        }catch (Exception e){
            System.out.println("Exception occurred in ClientasServer: "+ e);
        }
    }
}

class CoClientHandler extends Thread {
    final DataInputStream dis;
    final DataOutputStream dout;
    final Socket co_client;
    private Client client;

    public CoClientHandler(Socket co_client, DataInputStream dis, DataOutputStream dout, Client client) {
        this.co_client = co_client;
        this.dis = dis;
        this.dout = dout;
        this.client = client;
    }

    @Override
    public void run() {
        String received;

        try {
            while (true){
                if(dis.available()>0){
                    received = (String) dis.readUTF();
                    System.out.println("recived "+received);
                    String msgs[] = received.split(",");

                    int fileno = Integer.valueOf(msgs[1]);
                    if (msgs[0].equals("request")){             // if received msg is request the client adds the node entry to its own queue and sends a reply
                        client.getFileQueues()[fileno].add(new NodeEntry(Integer.valueOf(msgs[2]), new Timestamp(Long.valueOf(msgs[3]))));
                        dout.writeUTF("reply,"+fileno);
                        dout.flush();
                    }else if (msgs[0].equals("release")){       // if received msg is release the client removes the node entry from its own queue
                        client.getFileQueues()[fileno].poll();
                    }
                }
            }

        }catch (Exception e){
            System.out.println("Exception occurred in CoClientHandler: "+ e);
        }
    }
}

class NodeEntry implements Comparable<NodeEntry>{
    private Integer node;
    private Timestamp timestamp;

    public NodeEntry( Integer node, Timestamp timestamp){
        this.node = node;
        this.timestamp = timestamp;
    }

    public Integer getNode() {
        return node;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    @Override
    public int compareTo(NodeEntry other) {
        return this.getTimestamp().compareTo(other.getTimestamp());
    }
}
