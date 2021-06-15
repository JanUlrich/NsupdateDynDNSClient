import java.io.IOException;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class NsupdateClient {

    private String hostAddress = "yourHostname.nsupdate.info";
    private String password = "yourSecret";
    private String lastUpdatedIPv4 = null;

    public NsupdateClient() throws InterruptedException {
        while(true){
            try{
                checkIPv4();
            }catch(Exception exc){
                System.err.println("Error: " + exc.getMessage());
            }
            Thread.sleep(30000);
        }
    }

    private void checkIPv4(){ //Call this method in intervals
        if(lastUpdatedIPv4 == null){ //on first start or error, get IP from DNS
            lastUpdatedIPv4 = getIPv4Address(hostAddress);
        }
        String currentIP = getCurrentIPv4();
        //is update needed?
        if(lastUpdatedIPv4 != null && currentIP != null && ! lastUpdatedIPv4.equals(currentIP)){
            //update (only when currentIP != null and lastUpdatedIPv4 != null)
            lastUpdatedIPv4 = updateIPv4(this.hostAddress, this.password);
        }
    }

    private String getIPv4Address(String of){
        try {
            InetAddress address = InetAddress.getByName(of);
            String ip = address.toString();
            return ip.substring(ip.indexOf("/")+1,ip.length());
        } catch (UnknownHostException e) {
            e.printStackTrace();
            return null;
        }
    }

    private String getCurrentIPv4() {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://ipv4.nsupdate.info/myip"))
                .build();
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            //System.out.println(response.statusCode());
            //System.out.println(response.body());
            return response.body();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }

    private String updateIPv4(String of, String password) {
        HttpClient client = HttpClient.newBuilder()
                .authenticator(new Authenticator() {
                    public PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(of, password.toCharArray());
                    }
                })
                .build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://ipv4.nsupdate.info/nic/update"))
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            //System.out.println(response.statusCode());
            //System.out.println(response.body());
            String responseString = response.body();
            if(response.statusCode() == 200 && responseString.startsWith("good ")) {
                System.out.println("Updated with: " + responseString);
                return responseString.substring(5);
            }else if(response.statusCode() == 200 && responseString.startsWith("nochg ")){
                System.out.println("Updated with: " + responseString);
                return responseString.substring(6);
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
