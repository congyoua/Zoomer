package ca.utoronto.utm.mcs;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class Trip implements HttpHandler{
	private static Database db;
	
	public Trip(Database database) {
		// Get the database class
		db = database;
	}
	
	// Main function to handle different URIs
	public void handle(HttpExchange r) throws IOException{
		boolean data = true;
		try {
            if (r.getRequestURI().toString().equals("/trip/request") && r.getRequestMethod().equals("POST")) {
            	requestTrip(r);
            	return;
            } else if (r.getRequestURI().toString().equals("/trip/confirm") && r.getRequestMethod().equals("POST")) {
            	addTrip(r);
            	return;
            } else if (r.getRequestURI().toString().contains("/trip/") && r.getRequestMethod().equals("PATCH")) {
            	// Check uid parameter
            	String uid = r.getRequestURI().getPath().replace("/trip/","");
            	data = false;
    			if(!(uid.contains("/") || uid.isEmpty())) {
    				finishTrip(r, uid);
                	return;
    			}
            }
            // Invalid URI
            errorResponse(r, 400, data);
        } catch (Exception e) {
        	errorResponse(r, 500, data);
        }
	}
	
	// Handle the request trip uri
	public void requestTrip(HttpExchange r) throws NumberFormatException, JSONException, IOException {
		String body = Utils.convert(r.getRequestBody());
		JSONObject json_body = null;
	    
		// Try convert request string to JSON type
	    try {
			json_body = new JSONObject(body);
		} catch (JSONException ex) {
			errorResponse(r, 400, true);
			return;
		}
	    
	    String uid;
	    float radius;
	    // Check if the required parameters are in the JSON body
	    if (json_body.has("uid") && json_body.has("radius")){
	    	uid = json_body.getString("uid");
	    	radius = Float.parseFloat(json_body.getString("radius"));
		} else {
			errorResponse(r, 400, true);
			return;
		}
	    
	    // Generate the url and prepare to connect other microservices
	    URL url = new URL("http://localhost:8000/NearbyDriver/" + uid + "?radius=" + radius);
		HttpURLConnection connection = (HttpURLConnection)url.openConnection();
		connection.setRequestMethod("GET");
		connection.setRequestProperty("Content-Type", "application/json; utf-8");
		connection.setRequestProperty("Accept", "application/json");
		
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
				Iterator<String> driverList = json_response.getJSONObject("data").keys();
				String driver="";
				// Create the driver list
				while (driverList.hasNext()) {
					driver += "\""+driverList.next()+"\", ";
				}
				driver = driver.substring(0, driver.length()-2);
				String res = "{\"status\": \"OK\", \"data\": [" + driver + "]}";
				successResponse(r, res);
			} else if (json_response.getString("status").equals("BAD_REQUEST")) {
				errorResponse(r, 400, true);
			} else if (json_response.getString("status").equals("NOT_FOUND")) {
				errorResponse(r, 404, true);
			} else {
				errorResponse(r, 500, true);
			}
		} catch (Exception e) {
        	errorResponse(r, 500, true);
        }
	}
	
	// Insert a trip to database
	public void addTrip(HttpExchange r) throws IOException, JSONException {
		String body = Utils.convert(r.getRequestBody());
		JSONObject json_body = null;
	    
		// Try convert request string to JSON type
	    try {
			json_body = new JSONObject(body);
		} catch (JSONException ex) {
			errorResponse(r, 400, true);
			return;
		}
	    
	    String driver, passenger;
	    long time;
	    // Check if the required parameters are in the JSON body
	    if (json_body.has("driver") && json_body.has("passenger") && json_body.has("startTime")){
	    	driver = json_body.getString("driver");
	    	passenger = json_body.getString("passenger");
	    	time = json_body.getLong("startTime");
		} else {
			errorResponse(r, 400, true);
			return;
		}
	    
	    // Check if the required parameters are not empty
	    if (driver.isEmpty() || passenger.isEmpty()) {
	    	errorResponse(r, 400, true);
			return;
		}
	    
	    // Get the response from database
	    String response = db.addTrip(driver, passenger, time);
		successResponse(r, response);
	}
	
	// Update trip information to database
	public void finishTrip(HttpExchange r, String id) throws IOException, JSONException , NumberFormatException{
		String body = Utils.convert(r.getRequestBody());
		JSONObject json_body = null;
	    
		// Try convert request string to JSON type
	    try {
			json_body = new JSONObject(body);
		} catch (JSONException ex) {
			errorResponse(r, 400, false);
			return;
		}
	    
	    int distance, discount;
	    float cost,payout;
	    long endTime;
	    String time;
	    // Check if the required parameters are in the JSON body
	    if (json_body.has("distance") && json_body.has("endTime") && json_body.has("timeElapsed") && json_body.has("discount") && json_body.has("totalCost") && json_body.has("driverPayout")){
	    	distance = json_body.getInt("distance");
	    	endTime = json_body.getLong("endTime");
	    	time = json_body.getString("timeElapsed");
	    	discount = json_body.getInt("discount");
	    	cost = Float.parseFloat(json_body.getString("totalCost"));
	    	payout = Float.parseFloat(json_body.getString("driverPayout"));
		} else {
			errorResponse(r, 400, false);
			return;
		}
	    
	    // Check if the required parameters are not empty
	    if (time.isEmpty()) {
	    	errorResponse(r, 400, false);
			return;
		}
	    
	    // Get the response from database
	    String response = db.finishTrip(id, distance, endTime, time, discount, cost, payout);
		if (response.length() == 3) {
			errorResponse(r, Integer.parseInt(response), false);
		} else {
			successResponse(r, response);
		}
	}
	
	// Error response generator
	public void errorResponse(HttpExchange r, int type, boolean data) throws IOException {
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
		if (data) {
			response = "{\"status\": \""+ status + "\", \"data\":{} }";
		} else {
			response = "{\"status\": \""+ status + "\"}";
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
