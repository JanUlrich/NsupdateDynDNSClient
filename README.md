# Client for nsupdate.info

## Configure
* In line 9 and 10 set in your hostname and secret for the private fields
```
    private String hostAddress = "yourHostname.nsupdate.info";
    private String password = "yourSecret";
```

## Run
* requires Java 11
```
javac NsupdateClient.java
java NsupdateClient
```
