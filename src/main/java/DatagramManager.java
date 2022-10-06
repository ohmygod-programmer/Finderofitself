import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashSet;



/**
 * The {@code DatagramManager} class stores the arrays of datagrams and
 * check it for changing the set of enabled adresses
 */
public class DatagramManager {
    private static final ArrayList<HashSet<InetAddress>> pastAdresses = new ArrayList<>();
    int iter_to_delete; // Determines how many iterations we consider the
                        // address alive without receiving messages from it
    DatagramManager(int iterations_to_delete){
        iter_to_delete = iterations_to_delete;
        for (int i = 0; i < iterations_to_delete; i++){
            pastAdresses.add(new HashSet<>());
        }
    }

    public int checkDatagramArray(ArrayList<DatagramPacket> array, long mintime, long maxtime){
        HashSet<InetAddress>  enabledAddresses1 = new HashSet<>();
        HashSet<InetAddress>  enabledAddresses2 = new HashSet<>();
        HashSet<InetAddress>  newEnabledAddresses = new HashSet<>();
        for (int i = 0; i<iter_to_delete; i++){
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
                str = CryptoManager.decrypt(encryptedStr);
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
}
