package ca.utoronto.utm.mcs;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import org.json.JSONObject;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class User implements HttpHandler{
	private static Database db;
	
	public User(Database database) {
		// Get the database class
		db = database;
	}
	
	// Main function to handle different URIs
	public void handle(HttpExchange r) throws IOException{
		try {
            if (r.getRequestURI().toString().contains("/trips/passenger/") && r.getRequestMethod().equals("GET")) {
            	String uid = r.getRequestURI().getPath().replace("/trips/passenger/","");
            	// Check uid parameter
    			if(!(uid.contains("/")||uid.isEmpty())) {
    				passengerTrip(r, uid);
                	return;
    			}
            } else if (r.getRequestURI().toString().contains("/trips/driver/") && r.getRequestMethod().equals("GET")) {
            	String uid = r.getRequestURI().getPath().replace("/trips/driver/","");
            	// Check uid parameter
    			if(!(uid.contains("/")||uid.isEmpty())) {
    				driverTrip(r, uid);
                	return;
    			}
            } else if (r.getRequestURI().toString().contains("/trip/DriverTime/") && r.getRequestMethod().equals("GET")) {
            	String id = r.getRequestURI().getPath().replace("/trip/DriverTime/","");
            	// Check uid parameter
    			if(!(id.contains("/")||id.isEmpty())) {
    				arriveTime(r, id);
                	return;
    			}
            }
            // Invalid URI
            errorResponse(r, 400, 0);
        } catch (Exception e) {
        	errorResponse(r, 500, 0);
        }
	}
	
	// Get all driver trips from database
	public void driverTrip(HttpExchange r, String uid) throws IOException {
	    String response = db.driverTrip(uid);
	    if (response.length() == 3) {
			errorResponse(r, Integer.parseInt(response), 1);
		} else {
			successResponse(r, response);
		}
	}
	
	// Get all passenger trips from database
	public void passengerTrip(HttpExchange r, String uid) throws IOException {
	    String response = db.passengerTrip(uid);
	    if (response.length() == 3) {
			errorResponse(r, Integer.parseInt(response), 1);
		} else {
			successResponse(r, response);
		}
	}
	
	// Get the driver arrival_time from another microservice
	public void arriveTime(HttpExchange r, String id) throws IOException {
		// Get the driver and passenger from database
		String[] uid = db.getTrip(id);
		if (uid[0] == "empty") errorResponse(r, 404, 0);
		
		// Generate the url and prepare to connect other microservices
		URL url = new URL("http://localhost:8000/Navigation");
		HttpURLConnection connection = (HttpURLConnection)url.openConnection();
		connection.setRequestMethod("POST");
		connection.setRequestProperty("Content-Type", "application/json; utf-8");
		connection.setRequestProperty("Accept", "application/json");
		connection.setDoOutput(true);
		// Json body that sends to microservice
		String json = "{\"driveruid\": \"" + uid[0] + "\", \"passengeruid\": \"" + uid[1] + "\"}";
		
		// Send the json body
		try(OutputStream os = connection.getOutputStream()) {
		    byte[] input = json.getBytes("utf-8");
		    os.write(input, 0, input.length);			
		} catch (Exception e) {
        	errorResponse(r, 500, 0);
        }
		
		try(BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), "utf-8"))) {
			StringBuilder response = new StringBuilder();
			String responseLine = null;
			// Get the response from another microservice
			while ((responseLine = br.readLine()) != null) {
				response.append(responseLine.trim());
			}
			// Convert string to json
			JSONObject json_response = new JSONObject(response.toString());
			// Process different response status
			if (json_response.getString("status").equals("OK")) {
				int time = json_response.getJSONObject("data").getInt("total_time");
				String res = "{\"status\": \"OK\", \"data\": {\"arrival_time\": " + time + "}}";
				successResponse(r, res);
			} else if (json_response.getString("status").equals("BAD_REQUEST")) {
				errorResponse(r, 400, 0);
			} else if (json_response.getString("status").equals("NOT_FOUND")) {
				errorResponse(r, 404, 0);
			} else {
				errorResponse(r, 500, 0);
			}
		} catch (Exception e) {
        	errorResponse(r, 500, 0);
        }
	}
	
	// Error response generator
	public void errorResponse(HttpExchange r, int type, int resType) throws IOException {
		String status, response;
		
		// Determine error type
		if (type == 500) {
			status = "INTERNAL_SERVER_ERROR";
		} else if (type == 404) {
			status = "NOT_FOUND";
		} else {
			status = "BAD_REQUEST";
		}
		
		// Generate response
		if (resType == 0) {
			response = "{\"status\": \""+ status + "\", \"data\":{} }";
		} else {
			response = "{\"status\": \""+ status + "\", \"data\":{\"trips\":[]} }";
		}
		
		// Send back to user
		r.getResponseHeaders().set("Content-Type", "appication/json");
		r.sendResponseHeaders(type, response.length());
		OutputStream os = r.getResponseBody();
        os.write(response.getBytes());
        os.close();
	}
	
	// Send success response to user
	public void successResponse(HttpExchange r, String res) throws IOException {
		r.getResponseHeaders().set("Content-Type", "appication/json");
        r.sendResponseHeaders(200, res.length());
        OutputStream os = r.getResponseBody();
        os.write(res.getBytes());
        os.close();
	}
}
