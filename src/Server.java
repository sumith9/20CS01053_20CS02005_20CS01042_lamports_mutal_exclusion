import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {
    private int port = 5555;
    private ServerSocket server;
    private Socket client;

    public Server() throws IOException {
        try {
            server = new ServerSocket(port);

            while (true) {
                client = server.accept();

                DataInputStream dis = new DataInputStream(client.getInputStream());
                DataOutputStream dout = new DataOutputStream(client.getOutputStream());

                Thread t = new ClientHandler(client, dis, dout);

                t.start();
            }
        } catch (Exception e) {
            client.close();
            System.out.println("Exception occurred in Server: " + e);
        }
    }

    public static void main(String[] args) throws IOException {
        new Server(); // No need for arguments
    }
}



class ClientHandler extends Thread {
    final DataInputStream dis;
    final DataOutputStream dout;
    final Socket s;
    private String filePath;

    public ClientHandler(Socket s, DataInputStream dis, DataOutputStream dout) {
        this.s = s;
        this.dis = dis;
        this.dout = dout;
        this.filePath = "C:\\Users\\SUMITH\\Desktop\\java\\LamportsMutualExclusionAlgorithm-master\\files"; // Replace with actual file path
    }

    @Override
    public void run() {
        String received;
        String toSend = "";

        try {
            received = (String) dis.readUTF();
            while (received != null && received.length() != 0) {
                String msgs[] = received.split(",");
                System.out.println("Request received: " + received);

                if (msgs[0].equals("enquire")) {
                    toSend = enquire();
                } else if (msgs[0].equals("read")) {
                    toSend = read(msgs[1]);
                } else if (msgs[0].equals("write")) {
                    toSend = write(msgs[1], msgs[2] + msgs[3]);
                }
                dout.writeUTF(toSend);
                dout.flush();

                if(dis.available()>0){
                    received = (String) dis.readUTF();
                }else {
                    received = null;
                }
            }

            dout.close();
            dis.close();

        }catch (Exception e){
            System.out.println("Exception occurred in ClientHandler: "+ e);
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
            System.out.println("Exception occurred in enquire: "+ e);
            return null;
        }
    }


    public String read(String filename){
        try{
            File file = new File(filePath+filename);

            BufferedReader br = new BufferedReader(new FileReader(file));

            String st = "";
            String lastLine = "";
            while ((st = br.readLine()) != null){
                lastLine = st;
            }

            if(lastLine.equals("")){
                lastLine = filename+ " is empty";
            }

            return lastLine;
        }catch(Exception e){
            System.out.println("Exception occurred in enquire: "+e);
            return "Error in reading from " + filename;
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

            return "Successfully written to "+ filename;

        } catch (IOException e) {
            System.out.println("Exception occurred in wrie: "+e);
            return "Error in writing to " + filename;
        }
    }
}
