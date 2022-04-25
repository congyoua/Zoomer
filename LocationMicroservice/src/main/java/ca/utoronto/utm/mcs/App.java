package ca.utoronto.utm.mcs;

import java.io.IOException;
import java.net.InetSocketAddress;
import com.sun.net.httpserver.HttpServer;

public class App {
    static int PORT = 8000;

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", PORT), 0);
        
        // Create new Location, User, Road class and send database class to it
        Database db = new Database();
        Location location = new Location(db);
        User user = new User(db);
        Road road = new Road(db);
        
        // Mapping URI to different HTTP Context path
        server.createContext("/user", user);
        server.createContext("/location", location);
        server.createContext("/NearbyDriver", location);
        server.createContext("/Road", road);
        server.createContext("/has_route", road);
        server.createContext("/route", road);
        server.createContext("/Navigation", road);
        server.start();
        System.out.printf("Server started on port %d...\n", PORT);
    }
}
