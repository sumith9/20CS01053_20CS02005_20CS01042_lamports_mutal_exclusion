import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Timestamp;
import java.util.*;

public class Client {

    // server ports, addresses, sockets to connect to each server
    private int server_ports[] = new int [] {56391,56392,56393};

    private String servers[] = new String []{
            "10.10.13.104",
            "10.10.13.104",
            "10.10.13.104"
    };
    private Socket client;


    // client ports, addresses, sockets to connect to every other client
    private int client_ports[] = new int [] {56394,56395,56396};
    private String clients[] = new String []{
            "10.10.13.104",
            "10.10.13.104",
            "10.10.13.104"
    };
    private Socket[] coclient;
    private DataOutputStream[] douts;
    private DataInputStream[] diss;

    // file names, queue for each file, responses collected for each file
    private String[] files;
    private PriorityQueue<entryOfNode>[] queueOfFiles;
    private int[] responses;

    // client number
    private int node;

    public Client(String node){
        try {
            this.node = Integer.valueOf(node);
            coclient = new Socket[3];
            douts = new DataOutputStream[3];
            diss = new DataInputStream[3];

            Thread t = new ClientServer(this, client_ports[this.node-1]);
            t.start();

            enquire();
            clientConnection();
            randomRandW();

        }catch (Exception e){
            System.out.println("Found an exception in Client: " + e);
        }
    }


    // enquire returns list of all files in the server
    public void enquire(){
        try {
            Random random = new Random();
            int randomNode = random.nextInt(3);
            client = new Socket(servers[randomNode],server_ports[randomNode]);

            DataOutputStream dataOutStream=new DataOutputStream(client.getOutputStream());
            DataInputStream dataInStream = new DataInputStream(client.getInputStream());

            dataOutStream.writeUTF("enquire");
            dataOutStream.flush();

            String	listOfFiles = (String)dataInStream.readUTF();

            System.out.println("Files in the server are:");
            System.out.println(listOfFiles);

            // loading files and initializing file queues and responses
            files = listOfFiles.split(",");

            queueOfFiles = new PriorityQueue[files.length];
            for(int i=0; i < files.length; i++){
                queueOfFiles[i] =  new PriorityQueue<entryOfNode>();
            }

            responses = new int[files.length];
            Arrays.fill(responses, 0);

            dataInStream.close();
            dataOutStream.close();
            client.close();
        }catch (Exception e){
            System.out.println("Found an exception in enquire: "+ e);
        }
    }

    // connecting to all the clients except itself
    public void clientConnection() throws Exception{
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
    public void randomRandW(){
        try {
            Random random = new Random();
            String operation = "Read";
            for(int i=0; i < 5; i++){
                int rand1 = random.nextInt(2);
                int rand2 = random.nextInt(files.length);

                if(rand1 == 1)
                    operation = "Write";

                System.out.println("Requesting file '"+ files[rand2] + "' for "+ operation + " operation");
                requestingClients(rand2);

                // waiting till it is at the top of the respective file queue
                boolean waiting = true;
                while(waiting){
                    int top = queueOfFiles[rand2].peek().obtainNode();
                    if(top==node && responses[rand2]==2){
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
                        responses[rand2]=0;
                        waiting = false;
                    }else {
                        Thread.sleep(1000);
                    }
                }

                System.out.println(files[rand2] + " is being released");
                releaseMessage(rand2);
            }

            System.out.println("Successfully performed 5 Read/Write requests!!!");
        }catch (Exception e){
            System.out.println("Found an exception in randomRandW: "+ e);
        }

    }

    // sending request to all clients for a file
    public void requestingClients(int fileno){
        Long seconds = System.currentTimeMillis();
        queueOfFiles[fileno].add(new entryOfNode(node, new Timestamp(seconds)));
        try {
            for(int i = 0; i < 3 ; i++) {
                if(i!=(node-1)) {
                    douts[i].writeUTF("request," + fileno + "," + node+","+seconds);
                    douts[i].flush();

                    String reply = (String) diss[i].readUTF();
                    String msgs[] = reply.split(",");

                    if (msgs[0].equals("reply"))
                        responses[Integer.valueOf(msgs[1])] += 1;
                }
            }

        }catch (Exception e){
            System.out.println("Found an exception in requestingClients: "+e);
        }
    }

    // sending release of a file to all clients
    public void releaseMessage(int fileno){
        queueOfFiles[fileno].poll();
        try {
            for(int i = 0; i < 3 ; i++) {
                if(i!=(node-1)) {
                    douts[i].writeUTF("release," + fileno);
                    douts[i].flush();
                }
            }
        }catch (Exception e){
            System.out.println("Found an exception in releaseMessage: "+e);
        }
    }

    public void read(String filename) throws Exception{
        try{
            Random random = new Random();
            int randomNode = random.nextInt(3);
            client = new Socket(servers[randomNode],server_ports[randomNode]);

            DataOutputStream dataOutStream=new DataOutputStream(client.getOutputStream());
            DataInputStream dataInStream = new DataInputStream(client.getInputStream());

            dataOutStream.writeUTF("read,"+filename);
            dataOutStream.flush();

            String	message = (String)dataInStream.readUTF();
            System.out.println("Output: "+message);

            dataInStream.close();
            dataOutStream.close();
            client.close();
        }catch (Exception e){
            System.out.println("Found an exception in read: "+e);
        }
    }

    public void write(String filename){
        try{
            Timestamp timestamp =  new Timestamp(System.currentTimeMillis());
            String content = "Client "+node + ", " + timestamp.toString();

            for(int j = 0; j < 3 ; j++){
                client = new Socket(servers[j],server_ports[j]);
                DataOutputStream dataOutStream=new DataOutputStream(client.getOutputStream());
                DataInputStream dataInStream = new DataInputStream(client.getInputStream());
                dataOutStream.writeUTF("write,"+filename+","+content);
                dataOutStream.flush();

                String	message = (String)dataInStream.readUTF();
                System.out.println(message);

                dataInStream.close();
                dataOutStream.close();
                client.close();
            }
        }catch (Exception e){
            System.out.println("Found an exception in write: "+e);
        }
    }

    public PriorityQueue<entryOfNode>[] fetchFileQueue() {
        return queueOfFiles;
    }

    public static void main(String[] args) {
        if(args.length < 1){
            System.out.println("Provide the client number (1 to 5)");
        }else{
            new Client(args[0]);
        }
    }
}

//Client as a Server
class ClientServer extends Thread {
    private ServerSocket client_as_server;
    private Client client;

    public ClientServer(Client client, int port) throws Exception{
        client_as_server = new ServerSocket(port);
        this.client = client;
    }

    @Override
    public void run() {
        try {
            while(true){
                Socket  coClient = client_as_server.accept();

                DataInputStream dataInStream = new DataInputStream(coClient.getInputStream());
                DataOutputStream dataOutStream=new DataOutputStream(coClient.getOutputStream());

                Thread t = new coClientController(coClient, dataInStream, dataOutStream, client);
                t.start();
            }
        }catch (Exception e){
            System.out.println("Found an exception in ClientServer: "+ e);
        }
    }
}

class coClientController extends Thread {
    final DataInputStream dataInStream;
    final DataOutputStream dataOutStream;
    final Socket coClient;
    private Client client;

    public coClientController(Socket coClient, DataInputStream dataInStream, DataOutputStream dataOutStream, Client client) {
        this.coClient = coClient;
        this.dataInStream = dataInStream;
        this.dataOutStream = dataOutStream;
        this.client = client;
    }

    @Override
    public void run() {
        String received;

        try {
            while (true){
                if(dataInStream.available()>0){
                    received = (String) dataInStream.readUTF();
                    System.out.println("recived "+received);
                    String msgs[] = received.split(",");

                    int fileno = Integer.valueOf(msgs[1]);
                    if (msgs[0].equals("request")){             // if received message is request the client adds the node entry to its own queue and sends a reply
                        client.fetchFileQueue()[fileno].add(new entryOfNode(Integer.valueOf(msgs[2]), new Timestamp(Long.valueOf(msgs[3]))));
                        dataOutStream.writeUTF("reply,"+fileno);
                        dataOutStream.flush();
                    }else if (msgs[0].equals("release")){       // if received message is release the client removes the node entry from its own queue
                        client.fetchFileQueue()[fileno].poll();
                    }
                }
            }

        }catch (Exception e){
            System.out.println("Found an exception in coClientController: "+ e);
        }
    }
}

class entryOfNode implements Comparable<entryOfNode>{
    private Integer node;
    private Timestamp timestamp;

    public entryOfNode( Integer node, Timestamp timestamp){
        this.node = node;
        this.timestamp = timestamp;
    }

    public Integer obtainNode() {
        return node;
    }

    public Timestamp obtainTimestamp() {
        return timestamp;
    }

    @Override
    public int compareTo(entryOfNode other) {
        return this.obtainTimestamp().compareTo(other.obtainTimestamp());
    }
}
