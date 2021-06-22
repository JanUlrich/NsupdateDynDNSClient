import java.io.IOException;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class NsupdateClient {

    private String hostAddress = "domain";
    private String password = "pw";
    private InetAddress lastUpdatedIPv4 = null;
    private InetAddress lastUpdatedIPv6 = null;

    public NsupdateClient() throws InterruptedException {
        while(true){
            checkIP();
            Thread.sleep(30000);
        }
    }
    private boolean testNewAddress(InetAddress oldIP,InetAddress newIP){
        System.out.println("OldIP: "+oldIP+" NewIP: "+newIP);
        return oldIP == null || (newIP != null && oldIP.hashCode() != newIP.hashCode());
    }
    private void updatefromDNS(){
        InetAddress iplist[];
        try{
            iplist=Inet6Address.getAllByName(hostAddress);
        }
        catch(UnknownHostException e){
            iplist=new InetAddress[0];
        }
        for(InetAddress t : iplist)
            if(t instanceof Inet6Address)
                lastUpdatedIPv6 = t;
            else if(t instanceof Inet4Address)
                lastUpdatedIPv4 = t;
    }

    private void checkIP(){ //Call this method in intervals
        if(lastUpdatedIPv4 == null || lastUpdatedIPv6 == null){ //on first start or error, get IP from DNS
            updatefromDNS();
        }
        Inet4Address currentIP4 = null;
        Inet6Address currentIP6 = null;
        try {
            InetAddress a = getCurrentIP('4');
            currentIP4 = (Inet4Address) a;
            a = getCurrentIP('6');
            currentIP6 = (Inet6Address) a;
        } catch (ClassCastException e)
        {
            System.err.println("Wrong IP version! This should not happen.");
            System.exit(1);
        }
        //is update needed?
        if( testNewAddress(lastUpdatedIPv4, currentIP4) ){
            //update (only when currentIP != null and lastUpdatedIPv4 != null)
            lastUpdatedIPv4 = updateIP(this.hostAddress, this.password,currentIP4,'4');
        }
        if(testNewAddress(lastUpdatedIPv6, currentIP6)){
            lastUpdatedIPv6 = updateIP(this.hostAddress, this.password,currentIP6,'6');
        }
    }

    private InetAddress getCurrentIP(char version) {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://ipv"+version+".nsupdate.info/myip"))
                .build();
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            //System.out.println(response.statusCode());
            //System.out.println(response.body());
            return InetAddress.getByName(response.body());
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } /*catch (UnknownHostException e) {
           System.err.println("\""+"response.body()"+"\""+"Is no IPv"+version+" address");
        }*/
        return null;
    }

    private InetAddress updateIP(String of, String password,InetAddress addr,char version) {
        HttpClient client = HttpClient.newBuilder()
                .authenticator(new Authenticator() {
                    public PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(of, password.toCharArray());
                    }
                })
                .build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://ipv"+version+".nsupdate.info/nic/update"))
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            //System.out.println(response.statusCode());
            //System.out.println(response.body());
            String responseString = response.body();
            if(response.statusCode() == 200 && responseString.startsWith("good ")) {
                System.out.println("Updated with: " + responseString);
                return Inet6Address.getByName(responseString.substring(5));
            }else if(response.statusCode() == 200 && responseString.startsWith("nochg ")){
                System.out.println("Updated with: " + responseString);
                return Inet6Address.getByName(responseString.substring(6));
            }else{
                System.err.println("Error: " + responseString);
                return null;
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void main(String[] args) throws InterruptedException {
        new NsupdateClient();
    }
}
