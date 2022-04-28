import java.io.*;
import java.net.*;
import java.util.Arrays;

public class Server extends Thread
{
    private static final int opSize = 2;

    // read request opcode
    private static final byte RRQ = 1;
    // write request opcode
    private static final byte WRQ = 2;
    // data request opcode
    private static final byte DATA = 3;
    // acknowlege request opcode
    private static final byte ACK = 4;
    // error request opcode
    private static final byte ERROR = 5;

    // File not found opcode
    private static final byte FNF = 1;

    private static final int MAX_PACKET = 512;
    private static final int FULL_PACKET = 516;

    InetAddress ipAddress;

    DatagramSocket clientSocket = null;
    DatagramSocket firstSocket = null;
    int port = 10000;
    private static int clPort;

    public Server() throws SocketException {
        this("Server");
    }

    public Server(String name) throws SocketException {
        super(name);
        firstSocket = new DatagramSocket (11000);
    }

    @Override
    public void run()
    {
        byte []request = new byte[256];
        DatagramPacket packet = new DatagramPacket (request,request.length);

        try{
            while(true)
            {
                receivePacket(packet);
            }
        }catch(Exception e)
        {
            e.printStackTrace();
        }
        clientSocket.close();


    }

    public DatagramPacket createErrorPacket()
    {
        String errorMessage = "File not found";

        int lenght = opSize + 2 + errorMessage.getBytes ().length + 1;
        byte []errorArray = new byte[lenght];
        byte []messageArr = errorMessage.getBytes ();
        errorArray[1] = ERROR;
        errorArray[3] = FNF;

        for(int i  = 4; i < lenght;i++)
        {
            errorArray[i] = messageArr[i - 4];
        }

        DatagramPacket datagramPacket = new DatagramPacket (errorArray, errorArray.length,ipAddress,clPort);
        return datagramPacket;

    }

    public void receivePacket(DatagramPacket packet) throws IOException {
        firstSocket.receive (packet);
        ipAddress = packet.getAddress ();
        clPort = packet.getPort ();
        clientSocket = new DatagramSocket (port);

        byte[] receivedData = packet.getData ();
        byte first = 0;
        byte []opcode = new byte[2];
        opcode[1] = receivedData[1];


        if(receivedData[1] == RRQ)
        {
            String filename = getFilename(receivedData);
            System.out.println ("Read request");
            //TODO check file
            checkForFile(filename);
            File f = new File(filename);
            FileInputStream fis = new FileInputStream(f);
            int block = 1;
            byte []data = null;
            boolean endLoop = false;

            while(fis.available() > 0 && !endLoop)
            {
                if(fis.available() >= MAX_PACKET){
                    data = new byte[MAX_PACKET];
                    fis.read(data);
                }
                else{
                    data = new byte[fis.available()];
                    fis.read(data);
                }
                DatagramPacket dataPacket = createDATApacket(block,data);
                DatagramPacket ack = sendPacket(dataPacket);
                if (ack.getData()[3] != block){
                    endLoop = true;
                }
                block++;
            }


            
        }
        else if(receivedData[1] == WRQ)
        {
            System.out.println ("Write request");
            String filename = getFilename(receivedData);
            int block = 0;
            clientSocket.send(createACKpacket(block));
            block++;

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            boolean endLoop = false;
            DatagramPacket rec = new DatagramPacket(new byte[FULL_PACKET],FULL_PACKET);
            while(!endLoop)
            {
                rec = resentReceive(rec);
                if(rec.getData()[3] != block)
                {
                    byte[] data = Arrays.copyOfRange(rec.getData(),4,rec.getLength());

                    if(rec.getData().length == FULL_PACKET)
                    {
                        baos.write(data);
                    }
                    else{
                        endLoop = true;
                        baos.write(data);
                    }
                    clientSocket.send(createACKpacket(block));
                    block++;
                }
            }

            byte[] file = baos.toByteArray();
            File f = new File(filename);
            if(!f.exists()){
                f.createNewFile();
            }
            FileOutputStream fos = new FileOutputStream(f);
            fos.write(file);
            fos.close();

        }



    }

    public String getFilename(byte []data)
    {
        int val = 2;
        byte zero = 0;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        while(data[val] != zero)
        {
            baos.write(data[val]);
            val++;
        }
        byte []byteName = baos.toByteArray();
        String filename = new String(byteName);
        return filename;
    }


    public DatagramPacket createACKpacket(int blockNumber)
    {
        int length = opSize + 2;
        byte[]arr = new byte[length];

        arr[1] = ACK;
        arr[3] = (byte) blockNumber;
        DatagramPacket packet = new DatagramPacket (arr,arr.length,ipAddress,clPort);
        return packet;

    }
    
    public DatagramPacket createDATApacket(int block, byte[]data)
    {
        int lenght = opSize + 2 + data.length;
        byte []arr = new byte[lenght];
        arr[1] = DATA;
        arr[3] = (byte) block;
        
        for(int i  = 4; i < lenght;i++)
        {
            arr[i] = data[i - 4];
        }
        
        DatagramPacket packet = new DatagramPacket (arr,arr.length,ipAddress,clPort);
        return packet;
    }

    public DatagramPacket sendPacket(DatagramPacket packet) throws IOException {
        byte []buf = new byte[FULL_PACKET];
        DatagramPacket rec = new DatagramPacket(buf,buf.length);
        clientSocket.setSoTimeout(10000);
        clientSocket.send(packet);
        rec = resentReceive(packet);
        return rec;

    }


    public DatagramPacket resentReceive(DatagramPacket packet) throws IOException {
        byte[]buf = new byte[FULL_PACKET];
        DatagramPacket rec = new DatagramPacket(buf,FULL_PACKET);
        try{
            clientSocket.receive(rec);
            clPort = rec.getPort();

        }catch(SocketTimeoutException e)
        {
            sendPacket(packet);
        }
        return  rec;
    }

    public void checkForFile(String s) throws IOException {
        File f = new File(s);
        if(!f.exists()){
            clientSocket.send(createErrorPacket());
        }
    }

    public static void main(String[] args) throws SocketException {
        new Server (args[0]).run ();
        System.out.println("Start");
    }

}
