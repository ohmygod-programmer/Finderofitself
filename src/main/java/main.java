import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.net.*;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class main {

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

    public static final int SOCKET_TIMEOUT = 1000; //in milliseconds
    public static final int TIME_TO_RECEIVE = 1000; //in milliseconds


    public static String encrypt(String input, SecretKey key) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException,
            BadPaddingException, IllegalBlockSizeException {

        Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, key);
        byte[] cipherText = cipher.doFinal(input.getBytes());
        return Base64.getEncoder()
                .encodeToString(cipherText);
    }
    public static String decrypt(String algorithm, String cipherText, SecretKey key) throws NoSuchPaddingException, NoSuchAlgorithmException,
            InvalidKeyException,
            BadPaddingException, IllegalBlockSizeException {

        Cipher cipher = Cipher.getInstance(algorithm);
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
        public void run() {
            for (int i = 0; i < 8; i++){
                Long curTime = System.currentTimeMillis();
                String s = curTime.toString();
                datagramPacket.setData(s.getBytes());
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
        HashSet<InetAddress>  enabledAdresses1 = new HashSet<>();
        HashSet<InetAddress>  enabledAdresses2 = new HashSet<>();
        HashSet<InetAddress>  newEnabledAdresses = new HashSet<>();
        for (int i = 0; i<ITERATIONS_TO_DELETE; i++){
            enabledAdresses1.addAll(pastAdresses.get(i));
            if (i > 0){
                enabledAdresses2.addAll(pastAdresses.get(i));
            }
        }

        System.out.println(array.size());

        for (DatagramPacket p : array){
            byte[] data = p.getData();
            /*String data  = new String(Arrays.copyOfRange(bytes, 0, Arrays.));
            data.
            ArrayList<Character> characters = new ArrayList<>();

            System.out.println("String end");
            */
            String str = new String(data);
            str = str.substring(0, str.indexOf(0));
            /*for (int i = 0; i < str.length(); i++){
                System.out.println("simb " + str.substring(i, i+1));
            }*/
            long time;
            try {
                time = Long.parseLong(str);
            }
            catch (Exception e){
                System.out.println(e.toString());
                continue;
            }
            /*System.out.println(mintime);
            System.out.println("Time from packet " + Long.toString(time));
            System.out.println(maxtime);*/
            if (time>=mintime && time<=maxtime){
                newEnabledAdresses.add(p.getAddress());
                //System.out.println(enableAdresses);
            }
            enabledAdresses2.addAll(newEnabledAdresses);
        }
        if (!enabledAdresses1.containsAll(enabledAdresses2) ||
                !enabledAdresses2.containsAll(enabledAdresses1)){
            System.out.println(enabledAdresses2.toString());
        }
        pastAdresses.remove(0);
        pastAdresses.add(newEnabledAdresses);
        return enabledAdresses2.size();
    }

    public static void main(String[] args) {
        String address;
        int port;
        try {
            address = args[0];
        }
        catch (Exception e){
            System.out.println(e.toString());
            System.out.println("Problem with 1st arg, please give ip address");
            return;
        }
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



        MulticastSocket multicastSocket;
        InetAddress multicastAddress;
        try {
            multicastAddress = InetAddress.getByName(address);
            multicastSocket = new MulticastSocket(port);
            multicastSocket.setSoTimeout(SOCKET_TIMEOUT);
            multicastSocket.joinGroup(multicastAddress);

            System.out.println("Socket is created");
        }
        catch (Exception e){
            System.out.println(e.toString());
            return;
        }

        DatagramPacket sendingDatagram = new DatagramPacket(new byte[400], 400);
        sendingDatagram.setData("ZDAROVA".getBytes());
        sendingDatagram.setPort(port);
        sendingDatagram.setAddress(multicastAddress);

        Thread sender = new SenderThread(multicastSocket, sendingDatagram);
        sender.start();
        ArrayList<DatagramPacket> datagramArray = new ArrayList<>();
        long lastTime = System.currentTimeMillis();

        for(int itime = 0; itime<1000; itime++){

            var datagram = new DatagramPacket(new byte[400], 400);
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
                checkDatagramArray(datagramArray, lastTime-TIME_TO_RECEIVE, curTime);
                //System.out.println(checkDatagramArray(datagramArray, lastTime-TIME_TO_RECEIVE, curTime));
                lastTime = curTime;
                datagramArray.clear();
            }



        }

        try {
            multicastSocket.leaveGroup(InetAddress.getByName(address));
        }
        catch (Exception e){
            System.out.println(e.toString());
            return;
        }


    }
}