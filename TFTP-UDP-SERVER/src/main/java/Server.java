import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Date;

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

    DatagramSocket socket = null;
    DatagramSocket socket2 = null;
    int port = 10000;
    private static int clPort;

    @Override
    public void run()
    {
        byte []request = new byte[256];
        DatagramPacket packet = new DatagramPacket (request,request.length);


    }

    public void createErrorPacket()
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

        DatagramPacket datagramPacket = new DatagramPacket (errorArray, errorArray.length,ipAddress,port);

    }

    public void receivePacket(DatagramPacket packet)
    {
        socket2.receive (packet);
        ipAddress = packet.getAddress ();
        clPort = packet.getPort ();
        socket = new DatagramSocket (port);

        byte[] receivedData = packet.getData ();
        byte first = 0;

        if(receivedData[1] == RRQ)
        {
            System.out.println ("Read request");
        }
        else if(receivedData[1] == WRQ)
        {
            System.out.println ("Write request");
        }



    }

}
