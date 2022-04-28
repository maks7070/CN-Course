import javax.xml.crypto.Data;
import java.io.*;
import java.net.*;
import java.util.Arrays;
import java.util.Locale;
import java.util.Scanner;

/**
 * Client class, TFTP protocol on top of UDP according to RFC1350
 */
public class UDPClient
{
    //Read opcode
    private static final byte RRQ = 1;
    // Write opcode
    private static final byte WRQ = 2;
    //Data opcode
    private static final byte DATA = 3;
    //Acknowledgement opcode
    private static final byte ACK = 4;
    //Error message opcode
    private static final byte ERROR = 5;

    //Error File not found opcode
    private static final byte FILE_NOT_FOUND = 1;

    //Port number
    private static final int port = 9000;

    //Size of regular data packet
    private static final int packetSize = 512;

    //Maximum size of data packet
    private static final int maxPacketSize = 516;

    private static InetAddress ipAddress;
    private static DatagramSocket socket;
    private String filename;
    private static int clientPort;

    //First zero in packet
    private byte zero = 0;

    //Opcode size
    private int opSize = 2;

    //Mode
    private String mode = "octet";

    /**
     * Method responsible for getting parameters and commands
     * @throws IOException
     */
    public void menu() throws IOException {
        DatagramPacket packet;
        System.out.println ("Choose instruction and filename:");
        Scanner input = new Scanner (System.in);
        String command = input.nextLine ();
        String tmpFilename = input.nextLine();

        //Check if file exists
        boolean exists = false;
        while(exists == false)
        {
            File f = new File (tmpFilename);
            if(!f.exists ())
            {
                System.out.println ("File named " + tmpFilename +" does not exist, enter another name");
                Scanner sc = new Scanner (System.in);
                tmpFilename = sc.nextLine ();
            }
            else{
                System.out.println ("File located");
                filename = tmpFilename;
                exists = true;
            }
        }
        System.out.println (filename);
        if(command.equals ("read"))
        {
            //Send RRQ request
            packet = new DatagramPacket (rrqRequest (filename), rrqRequest (filename).length, ipAddress,port);
            socket.setSoTimeout (10000);
            socket.send (packet);

            byte[]buf = new byte[maxPacketSize];
            DatagramPacket rec = new DatagramPacket (buf,buf.length);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            rec = receivedResent (packet);
            boolean endOfData = false;
            int block = 1;
            if(checkIfNotErrorPacket (rec))
            {
                while(!endOfData)
                {
                    byte []data = rec.getData ();
                    byte[]number = new byte[2];
                    number[1] = data[3];
                    if(number[1] == (byte) block)
                    {
                        baos.write(data,4,rec.getLength() - 4);
                        createACKpacket(number);



                    }
                    System.out.println("Packet count: " + block);
                    rec = receivedResent(packet);
                    block++;
                    if(data.length < packetSize)
                    {
                        endOfData = true;
                        baos.write(data,4,rec.getLength() - 4);
                    }
                }
                FileOutputStream fos = new FileOutputStream(filename);
                byte[]file = baos.toByteArray();
                fos.write(file);
                fos.close();
            }
            else{
                System.out.println("File not found");
            }


        }
        else if(command.equals ("write"))
        {
            //Send write request
            packet = new DatagramPacket (wrqRequest (filename), wrqRequest (filename).length, ipAddress,port);
            socket.setSoTimeout (10000);
            socket.send (packet);
            if(isASKReceived(packet))
            {
                System.out.println ("Input data:");
                Scanner sc = new Scanner (System.in);
                String data = sc.nextLine ();
                // Write to file and send
                DatagramPacket packet1;
                DatagramPacket rec;
                FileWriter fw = new FileWriter (filename);
                fw.write (data);
                fw.close ();
                File f = new File (filename);
                FileInputStream fis = new FileInputStream (f);
                byte [] packetData = null;
                int blockNo = 1;
                while(fis.available () > 0)
                {
                    if(fis.available () >= packetSize)
                    {
                        packetData = new byte[packetSize];
                        fis.read (packetData);
                    }
                    else{
                        packetData = new byte[fis.available ()];
                        fis.read (packetData);
                    }

                    packet1 = new DatagramPacket (packetData,blockNo);
                    packet1.setPort (port);
                    rec = sendPack(packet);
                    blockNo++;
                }
            }
        }
        else{
            System.out.println ("Wrong command");
            menu ();
        }
    }

    public DatagramPacket receivedResent(DatagramPacket packet) throws SocketException {

            byte []buf = new byte[maxPacketSize];
            DatagramPacket rec = new DatagramPacket (buf,buf.length);
        try{
            socket.receive (rec);
            clientPort = rec.getPort ();
        }
        catch(IOException e)
        {
            sendPack (packet);
        }
        return rec;
    }

    public byte[] rrqRequest(String filename)
    {
        // Size of array: "0" + opcode + lenght of filename + "0" + octet lenght + "0"
        byte[] rrqArray = new byte[opSize + filename.length () + 1 + mode.length () + 1];
        rrqArray[0] = zero;
        rrqArray[1] = RRQ;
        char[]filenameArr = new char[filename.length ()];
        filenameArr = filename.toCharArray ();
        int i = 2;
        for(char c : filenameArr)
        {
            rrqArray[i] = Byte.parseByte (String.valueOf (c));
            i++;
        }
        rrqArray[i] = zero;
        i++;
        char []modeArr = mode.toCharArray ();
        for(char c: modeArr)
        {
            rrqArray[i] = Byte.parseByte (String.valueOf (c));
            i++;
        }
        rrqArray[i] = zero;
        return rrqArray;
    }

    public byte[] wrqRequest(String filename)
    {
        // Size of array: "0" + opcode + lenght of filename + "0" + octet lenght + "0"
        byte[] rrqArray = new byte[opSize + filename.length () + 1 + mode.length () + 1];
        rrqArray[0] = zero;
        rrqArray[1] = WRQ;
        char[]filenameArr = new char[filename.length ()];
        filenameArr = filename.toCharArray ();
        int i = 2;
        for(char c : filenameArr)
        {
            rrqArray[i] = Byte.parseByte (String.valueOf (c));
            i++;
        }
        rrqArray[i] = zero;
        i++;
        char []modeArr = mode.toCharArray ();
        for(char c: modeArr)
        {
            rrqArray[i] = Byte.parseByte (String.valueOf (c));
            i++;
        }
        rrqArray[i] = zero;
        return rrqArray;
    }

    public boolean isASKReceived(DatagramPacket packet)
    {

        // Lenght of ackPacket is 4
        byte[] ackPacket = new byte[4];
        DatagramPacket ackReceived = new DatagramPacket (ackPacket, ackPacket.length);
        return true;

    }

    public DatagramPacket sendPack(DatagramPacket packet) throws SocketException {
        byte[] buf = new byte[maxPacketSize];
        DatagramPacket rec = new DatagramPacket (buf, buf.length);
        socket.setSoTimeout (10000);
        rec = receivedResent (packet);
        return rec;
    }


    public DatagramPacket createDataPacket(byte[]data, int block)
    {
        int lenght = opSize + 2 + data.length;
        byte [] packetArr = new byte[lenght];
        packetArr[1] = DATA;
        packetArr[3] = (byte) block;
        for(int i = 4; i < packetArr.length;i++)
        {
            packetArr[i] = data[i -4];
        }

        DatagramPacket packet = new DatagramPacket (packetArr,packetArr.length,ipAddress,port);
        return packet;



    }



    public void createACKpacket(byte[] blockNumber) throws IOException {
        byte []send = new byte[4];
        send[1] = ACK;
        send[3] = blockNumber[1];
        DatagramPacket ack = new DatagramPacket(send,send.length,ipAddress,clientPort);
        socket.send(ack);
    }


    public boolean checkIfNotErrorPacket(DatagramPacket packet)
    {
        byte []data = packet.getData ();
        if(data[1] == ERROR)
        {
            return false;
        }
        else{
            return true;
        }

    }










    public static void main(String[] args) throws IOException {
        if(args.length != 1)
        {
            System.out.println ("Invalid arguments");
            return;
        }
        UDPClient client = new UDPClient ();
        clientPort = 3000;
        ipAddress = InetAddress.getByName (args[0]);
        socket = new DatagramSocket (clientPort,ipAddress);
        client.menu ();


    }


}


