import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.net.*;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class finderofitself {

    private static final String ENCRYPTION_ALGORITHM = "AES";
    private static final String ENCODED_SECRET_KEY = "+CjoihtUUH39pRj6ZjfmjfBaTG8NXVAs2wSlIT6IfoY=";
    private static SecretKey convertStringToSecretKey(String encodedKey) {
        byte[] decodedKey = Base64.getDecoder().decode(encodedKey);
        SecretKey originalKey = new SecretKeySpec(decodedKey, 0, decodedKey.length, ENCRYPTION_ALGORITHM);
        return originalKey;
    }
    private static final SecretKey SECRET_KEY = convertStringToSecretKey(ENCODED_SECRET_KEY);
    private static final int ITERATIONS_TO_DELETE = 3;
    private static final ArrayList<HashSet<InetAddress>> pastAdresses = new ArrayList<>();

    public static final int SIZE_OF_DATAGRAMS = 200; //in bytes
    public static final int SOCKET_TIMEOUT = 1000; //in milliseconds
    public static final int TIME_TO_RECEIVE = 1000; //in milliseconds
    public static final int SIZE_OF_TIME_ERROR = 1000; //in milliseconds. Need because ping

    public static String encrypt(String input, SecretKey key) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException,
            BadPaddingException, IllegalBlockSizeException {

        Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, key);
        byte[] cipherText = cipher.doFinal(input.getBytes());
        return Base64.getEncoder()
                .encodeToString(cipherText);
    }

    public static String decrypt(String cipherText, SecretKey key) throws NoSuchPaddingException, NoSuchAlgorithmException,
            InvalidKeyException,
            BadPaddingException, IllegalBlockSizeException {

        Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, key);
        byte[] plainText = cipher.doFinal(Base64.getDecoder()
                .decode(cipherText));
        return new String(plainText);
    }

    public static class SenderThread extends Thread {
        MulticastSocket multicastSocket;
        DatagramPacket datagramPacket;
        SenderThread(MulticastSocket s, DatagramPacket p){
            multicastSocket = s;
            datagramPacket = p;
        }
        @Override
        public void run(){
            while (true){
                Long curTime = System.currentTimeMillis();
                String s = curTime.toString();
                String encryptedS;
                try {
                    encryptedS = encrypt(s, SECRET_KEY);
                    datagramPacket.setData(encryptedS.getBytes());

                }
                catch (Exception e){
                    System.out.println(e);
                    datagramPacket.setData(s.getBytes());
                }
                try {
                    multicastSocket.send(datagramPacket);
                    Thread.sleep(TIME_TO_RECEIVE);
                }
                catch (Exception e) {
                    System.out.println(e);
                    return;
                }
            }
        }
    }


    public static int checkDatagramArray(ArrayList<DatagramPacket> array, long mintime, long maxtime){
        HashSet<InetAddress>  enabledAddresses1 = new HashSet<>();
        HashSet<InetAddress>  enabledAddresses2 = new HashSet<>();
        HashSet<InetAddress>  newEnabledAddresses = new HashSet<>();
        for (int i = 0; i<ITERATIONS_TO_DELETE; i++){
            enabledAddresses1.addAll(pastAdresses.get(i));
            if (i > 0){
                enabledAddresses2.addAll(pastAdresses.get(i));
            }
        }

        for (DatagramPacket p : array){
            byte[] data = p.getData();

            String encryptedStr = new String(data);
            encryptedStr = encryptedStr.substring(0, encryptedStr.indexOf(0));
            String str;
            try{
                str = decrypt(encryptedStr, SECRET_KEY);
            }
            catch (Exception e){
                str = encryptedStr;
            }

            long time;
            try {
                time = Long.parseLong(str);
            }
            catch (Exception e){
                continue;
            }
            if (time>=mintime && time<=maxtime){
                newEnabledAddresses.add(p.getAddress());
            }
            enabledAddresses2.addAll(newEnabledAddresses);
        }

        if (!enabledAddresses1.containsAll(enabledAddresses2) ||
                !enabledAddresses2.containsAll(enabledAddresses1)){
            System.out.println("###NEW CHANGES###");
            System.out.println("Enabled addresses(" + enabledAddresses2.size() + "):");
            for (InetAddress address: enabledAddresses2){
                System.out.println(address.toString());
            }

            HashSet<InetAddress> newAddrs = new HashSet<>();
            newAddrs.addAll(enabledAddresses2);
            newAddrs.removeAll(enabledAddresses1);
            System.out.println("New addresses(" + newAddrs.size() + "):");
            for (InetAddress address: newAddrs) {
                System.out.println(address.toString());
            }

            HashSet<InetAddress> disAddrs = new HashSet<>();
            disAddrs.addAll(enabledAddresses1);
            disAddrs.removeAll(enabledAddresses2);
            System.out.println("Just disabled addresses(" + disAddrs.size() + "):");
            for (InetAddress address: disAddrs) {
                System.out.println(address.toString());
            }
            System.out.println();

        }
        pastAdresses.remove(0);
        pastAdresses.add(newEnabledAddresses);
        return enabledAddresses2.size();
    }



    public static void main(String[] args) {
        String address;
        int port;
        if (args.length<2){
            System.out.println("Too few arguments. please give ip and port.");
            return;
        }
        address = args[0];

        try {
            port = Integer.valueOf(args[1]);
        }
        catch (Exception e){
            System.out.println(e.toString());
            System.out.println("Problem with 2nd arg, please give port");
            return;
        }
        for (int i = 0; i < ITERATIONS_TO_DELETE; i++){
            pastAdresses.add(new HashSet<>());
        }
        NetworkInterface networkInterface = null;
        if (args.length > 2){
            try {
                networkInterface = NetworkInterface.getByName(args[2]);
            }
            catch (Exception e) {
                System.out.println(e);
            }
        }

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
            System.out.println(e.toString());
            return;
        }

        DatagramPacket sendingDatagram = new DatagramPacket(new byte[SIZE_OF_DATAGRAMS], SIZE_OF_DATAGRAMS);
        sendingDatagram.setPort(port);
        sendingDatagram.setAddress(multicastAddress.getAddress());

        Thread sender = new SenderThread(multicastSocket, sendingDatagram);
        sender.start();

        ArrayList<DatagramPacket> datagramArray = new ArrayList<>();
        long lastTime = System.currentTimeMillis();

        while (true){

            var datagram = new DatagramPacket(new byte[SIZE_OF_DATAGRAMS], SIZE_OF_DATAGRAMS);
            try {
                multicastSocket.receive(datagram);

                datagramArray.add(datagram);
            }
            catch (SocketTimeoutException e){}
            catch (Exception e) {
                System.out.println(e.toString());
                return;
            }
            long curTime = System.currentTimeMillis();
            if (curTime - lastTime >= TIME_TO_RECEIVE){
                checkDatagramArray(datagramArray, lastTime-SIZE_OF_TIME_ERROR, curTime+SIZE_OF_TIME_ERROR);
                lastTime = curTime;
                datagramArray.clear();
            }

        }

    }
}