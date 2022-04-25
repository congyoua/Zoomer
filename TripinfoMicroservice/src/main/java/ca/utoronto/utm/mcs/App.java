package ca.utoronto.utm.mcs;

import java.io.IOException;
import java.net.InetSocketAddress;
import com.sun.net.httpserver.HttpServer;

public class App {
    static int PORT = 8000;

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", PORT), 0);
        
        // Create new Trip, User class and send database class to it
        Database db = new Database();
        User user = new User(db);
        Trip trip = new Trip(db);
        
        // Mapping URI to different HTTP Context path
        server.createContext("/trip/request", trip);
        server.createContext("/trip/confirm", trip);
        server.createContext("/trip", trip);
        server.createContext("/trips/passenger", user);
        server.createContext("/trips/driver", user);
        server.createContext("/trip/DriverTime", user);
        server.start();
        System.out.printf("Server started on port %d...\n", PORT);
    }
}
