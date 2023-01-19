import java.io.IOException;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class NsupdateClient {

    private String hostAddress = "domain";
    private String password = "pw";
    private InetAddress lastUpdatedIPv4 = null;
    private InetAddress lastUpdatedIPv6 = null;
    private final long HTTP_REQUEST_TIMEOUT = 15; //in seconds

    public NsupdateClient() {
        checkIP();
        while(true){
            try {
                Thread.sleep(30000);
                checkIP();
            } catch (InterruptedException e) {
                System.out.println("Thread got interrupted"); //but wee keep going
                e.printStackTrace();
            }
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
            //update (only when currentIP != null and lastUpdatedIPv4 != currentIP4)
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
            CompletableFuture<HttpResponse<String>> requestThread = client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
            HttpResponse<String> response = requestThread.get(HTTP_REQUEST_TIMEOUT, TimeUnit.SECONDS);
            return InetAddress.getByName(response.body());
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        }
        // in case of error, this function return null, which will not trigger an updateIP
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
            CompletableFuture<HttpResponse<String>> requestThread = client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
            HttpResponse<String> response = requestThread.get(HTTP_REQUEST_TIMEOUT, TimeUnit.SECONDS);

            String responseString = response.body();
            if(response.statusCode() == 200 && responseString.startsWith("good ")) {
                System.out.println("Updated with: " + responseString);
                return Inet6Address.getByName(responseString.substring(5));
            }else if(response.statusCode() == 200 && responseString.startsWith("nochg ")){
                System.out.println("Updated with: " + responseString);
                return Inet6Address.getByName(responseString.substring(6));
            }else if(response.statusCode() == 200 && responseString.startsWith("notfqdn")){
                System.out.println("Error: " + responseString);
                System.out.println("Wrong Hostname, check your config");
                System.exit(-1);
            }else if(response.statusCode() == 200 && responseString.startsWith("badauth")){
                System.out.println("Error: " + responseString);
                System.out.println("Wrong Username/Password, check your config");
                System.exit(-1);
            }else if(response.statusCode() == 200 && responseString.startsWith("conflict")){
                System.out.println("Error: " + responseString);
                System.out.println("Problem with your DNS record. Check DNS config");
                System.exit(-1);
            }else if(response.statusCode() == 200 && responseString.startsWith("911")){
                System.out.println("Error: " + responseString);
                //Lets wait 5 minutes before we continue:
                try {
                    Thread.sleep(5*60000);
                } catch (InterruptedException e) {
                    System.out.println("Thread got interrupted"); //but wee keep going
                    e.printStackTrace();
                }
            }else if(response.statusCode() == 200 &&
                    (responseString.startsWith("abuse") || responseString.startsWith("badagent"))){
                System.out.println("Error: " + responseString);
                System.out.println("Major Error!");
                System.exit(-1);
            }else{
                System.err.println("Error: " + responseString);
                return null;
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void main(String[] args) {
        new NsupdateClient();
    }
}
