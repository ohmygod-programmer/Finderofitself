import java.net.*;
import java.util.*;

public class Finderofitself {

    private static final int SIZE_OF_DATAGRAMS = 200;   //in bytes
    private static final int SOCKET_TIMEOUT = 1000;     //in milliseconds
    private static final int TIME_TO_RECEIVE = 1000;    //in milliseconds
    private static final int SIZE_OF_TIME_ERROR = 10000; //in milliseconds. Need because ping and some time lag
    private static final int ITERATIONS_TO_DELETE = 3;  // Determines how many iterations we consider the
                                                        // address alive without receiving messages from it

    public static void main(String[] args) {

            /*ARGS CHECKING*/

        String address;
        int port;
        if (args.length<2){
            System.out.println("Too few arguments. please give ip and port.");
            return;
        }
        address = args[0];

        try {
            port = Integer.parseInt(args[1]);
        }
        catch (Exception e){
            System.out.println(e);
            System.out.println("Problem with 2nd arg, please give port");
            return;
        }

        NetworkInterface networkInterface = null;
        if (args.length > 2){
            try {
                InetAddress inetAddress = InetAddress.getByName(args[2]);
                networkInterface = NetworkInterface.getByInetAddress(inetAddress);
                if (networkInterface == null){
                    System.out.println("Interface with ip " + args[2] + " not found. Running with other interface...");
                }
            }
            catch (Exception e) {
                System.out.println(e);
                System.out.println("Problem with 3rd arg, please give address of your interface");
                return;
            }
        }

            /*INITIALIZING*/

        MulticastSocket multicastSocket;
        InetSocketAddress multicastAddress;

        try {
            multicastAddress = new InetSocketAddress(InetAddress.getByName(address), port);
            multicastSocket = new MulticastSocket(port);
            multicastSocket.setSoTimeout(SOCKET_TIMEOUT);
            if (networkInterface != null) {
                multicastSocket.joinGroup(multicastAddress, networkInterface);
            }
            else {
                multicastSocket.joinGroup(multicastAddress.getAddress());
            }

            System.out.println("Socket is created");
        }
        catch (Exception e){
            System.out.println(e);
            return;
        }

        DatagramManager datagramManager = new DatagramManager(ITERATIONS_TO_DELETE);
        ArrayList<DatagramPacket> datagramArray = new ArrayList<>();

        DatagramPacket sendingDatagram = new DatagramPacket(new byte[SIZE_OF_DATAGRAMS], SIZE_OF_DATAGRAMS);
        sendingDatagram.setPort(port);
        sendingDatagram.setAddress(multicastAddress.getAddress());

        Sender sender = new Sender(multicastSocket, sendingDatagram);
        sender.setPeriod(TIME_TO_RECEIVE);
        Thread senderThread = new Thread(sender);
        long lastTime = System.currentTimeMillis();
        senderThread.start();

            /*MAIN CYCLE*/

        while (true){

            var datagram = new DatagramPacket(new byte[SIZE_OF_DATAGRAMS], SIZE_OF_DATAGRAMS);
            try {
                multicastSocket.receive(datagram);
                datagramArray.add(datagram);
            }
            catch (SocketTimeoutException e){}
            catch (Exception e) {
                System.out.println(e);
                return;
            }
            long curTime = System.currentTimeMillis();
            if (curTime - lastTime >= TIME_TO_RECEIVE){
                datagramManager.checkDatagramArray(datagramArray, lastTime-SIZE_OF_TIME_ERROR, curTime+SIZE_OF_TIME_ERROR);
                lastTime = curTime;
                datagramArray.clear();
            }

        }

    }
}