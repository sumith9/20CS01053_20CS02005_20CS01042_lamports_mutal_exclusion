import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {
    private int ports[] = new int [] {3333,4444,5555};
    private ServerSocket server;
    private Socket client;

    public Server(String node) throws IOException{
        try {
            server = new ServerSocket(ports[Integer.valueOf(node)-1]);

            while(true){
                client = server.accept();

                DataInputStream dataInStream = new DataInputStream(client.getInputStream());
                DataOutputStream dataOutStream=new DataOutputStream(client.getOutputStream());

                Thread t = new clientController(client, dataInStream, dataOutStream, node);

                t.start();

            }

        }catch (Exception e){
            client.close();
            System.out.println("Found an exception in Server: " + e);
        }

    }

    public static void main(String[] args) throws IOException{
        if(args.length < 1){
            System.out.println("Provide the server number (1 to 3)");
        }else{
            new Server(args[0]);
        }
    }

}

class clientController extends Thread {
    final DataInputStream dataInStream;
    final DataOutputStream dataOutStream;
    final Socket s;
    private String filePath;
    private String node;

    public clientController(Socket s, DataInputStream dataInStream, DataOutputStream dataOutStream, String node) {
        this.s = s;
        this.dataInStream = dataInStream;
        this.dataOutStream = dataOutStream;
        this.node = node;
        this.filePath = "C:\\Users\\SUMITH\\Desktop\\java\\lamport\\files";

    }

    @Override
    public void run() {
        String received;
        String toSend = "";

        try {
            received = (String) dataInStream.readUTF();
            while (received != null && received.length() != 0) {
                String messages[] = received.split(",");
                System.out.println("Request received: " + received);

                if (messages[0].equals("enquire")) {
                    toSend = enquire();
                } else if (messages[0].equals("read")) {
                    toSend = read(messages[1]);
                } else if (messages[0].equals("write")) {
                    toSend = write(messages[1], messages[2] + messages[3]);
                }
                dataOutStream.writeUTF(toSend);
                dataOutStream.flush();

                if(dataInStream.available()>0){
                    received = (String) dataInStream.readUTF();
                }else {
                    received = null;
                }
            }

            dataOutStream.close();
            dataInStream.close();

        }catch (Exception e){
            System.out.println("Found an exception in clientController: "+ e);
        }
    }

    public String enquire(){
        try{
            File folder = new File(filePath);
            File[] listOfFiles = folder.listFiles();
            String filesList  = "";

            for (int i = 0; i < listOfFiles.length; i++) {
                if(i!=listOfFiles.length-1){
                    filesList = filesList + listOfFiles[i].getName() + ",";
                }else{
                    filesList = filesList + listOfFiles[i].getName();
                }
            }

            return filesList;
        }catch (Exception e){
            System.out.println("Found an exception in enquire: "+ e);
            return null;
        }
    }


    public String read(String filename){
        try{
            File file = new File(filePath+filename);

            BufferedReader br = new BufferedReader(new FileReader(file));

            String st = "";
            String endLine = "";
            while ((st = br.readLine()) != null){
                endLine = st;
            }

            if(endLine.equals("")){
                endLine = filename+ " is empty";
            }

            return endLine;
        }catch(Exception e){
            System.out.println("Found an exception in enquire: "+e);
            return "Error in reading from " + filename + " in Server"+node;
        }
    }

    public String write(String filename, String content) {
        BufferedWriter bw = null;
        FileWriter fw = null;
        try {
            fw = new FileWriter(filePath+ filename, true);
            bw = new BufferedWriter(fw);
            bw.write("\n"+content);
            bw.close();
            fw.close();

            return "Successfully written to "+ filename + " in Server"+node;

        } catch (IOException e) {
            System.out.println("Found an exception in wrie: "+e);
            return "Error in writing to " + filename + " in Server"+node;
        }
    }

}
