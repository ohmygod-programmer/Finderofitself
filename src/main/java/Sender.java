import java.net.DatagramPacket;
import java.net.MulticastSocket;

/**
 * The {@code Sender} class sending encrypted messages with current time every ({@code period}) milliseconds.
 */

public class Sender implements Runnable{
    MulticastSocket multicastSocket;
    DatagramPacket datagramPacket;
    long period = 1000; //in milliseconds
    Sender(MulticastSocket s, DatagramPacket p){
        multicastSocket = s;
        datagramPacket = p;
    }

    public void setPeriod(long period) {
        this.period = period;
    }


    public void run(){
        while (true){
            Long curTime = System.currentTimeMillis();
            String s = curTime.toString();
            String encryptedS;
            try {
                encryptedS = CryptoManager.encrypt(s);
                datagramPacket.setData(encryptedS.getBytes());

            }
            catch (Exception e){
                System.out.println(e);
                datagramPacket.setData(s.getBytes());
            }
            try {
                multicastSocket.send(datagramPacket);
                Thread.sleep(period);
            }
            catch (Exception e) {
                System.out.println(e);
                return;
            }
        }
    }

}
