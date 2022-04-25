package ca.utoronto.utm.mcs;

import java.io.IOException;
import java.io.OutputStream;

import org.json.*;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class Road implements HttpHandler{
	private static Database db;
	
	public Road(Database database) {
		// Get the database class
		db = database;
	}
	
	// Main function to handle different URIs
	public void handle(HttpExchange r) throws IOException{
		try {
            if (r.getRequestURI().toString().equals("/Road") && r.getRequestMethod().equals("PUT")) {
            	addRoad(r);
            } else if (r.getRequestURI().toString().equals("/has_route") && r.getRequestMethod().equals("POST")) {
            	addRoute(r);
            } else if (r.getRequestURI().toString().equals("/route") && r.getRequestMethod().equals("DELETE")) {
            	deleteRoute(r);
            } else if (r.getRequestURI().toString().equals("/Navigation") && r.getRequestMethod().equals("POST")) {
            	navigation(r);
            }
            // Invalid URI
            errorResponse(r, 400, false);
        } catch (Exception e) {
        	errorResponse(r, 500, false);
        }
	}
	
	// Handle the put road uri
	public void addRoad(HttpExchange r) throws IOException, JSONException {
		String body = Utils.convert(r.getRequestBody());
		JSONObject json_body = null;
		
		// Try convert request string to JSON type
		try {
			json_body = new JSONObject(body);
		} catch (JSONException ex) {
			errorResponse(r, 400, false);
			return;
		}
		
		String name;
		boolean has_traffic;
		// Check if the required parameters are in the JSON object
	    if (json_body.has("RoadName") && json_body.has("HasTraffic")){
	    	name = json_body.getString("RoadName");
	    	has_traffic = json_body.getBoolean("HasTraffic");
		} else {
			errorResponse(r, 400, false);
			return;
		}
	    
	    // Check if the required parameters are not empty
	    if (name.isEmpty()) {
	    	errorResponse(r, 400, false);
			return;
		}
	    
	    // Get the response from database
	    String response = db.addRoad(name, has_traffic);
		if (response.length() == 3) {
			errorResponse(r, Integer.parseInt(response), false);
		} else {
			successResponse(r, response);
		}
	}
	
	// Handle add route uri
	public void addRoute(HttpExchange r) throws JSONException, IOException {
		String body = Utils.convert(r.getRequestBody());
		JSONObject json_body = null;
		
		// Try convert request string to JSON type
		try {
			json_body = new JSONObject(body);
		} catch (JSONException ex) {
			errorResponse(r, 400, false);
			return;
		}
		
		String name1;
		String name2;
		boolean is_traffic;
		int time;
		// Check if the required parameters are in the JSON body
	    if (json_body.has("roadName1") && json_body.has("roadName2") && json_body.has("is_traffic") && json_body.has("time")){
	    	name1 = json_body.getString("roadName1");
	    	name2 = json_body.getString("roadName2");
	    	is_traffic = json_body.getBoolean("is_traffic");
	    	time = json_body.getInt("time");
		} else {
			errorResponse(r, 400, false);
			return;
		}
	    
	    // Check if the required parameters are not empty
	    if (name1.isEmpty() || name2.isEmpty() || time==0) {
	    	errorResponse(r, 400, false);
			return;
		}
	    
	    // Get the response from database
	    String response = db.addRoute(name1, name2, is_traffic, time);
		if (response.length() == 3) {
			errorResponse(r, Integer.parseInt(response), false);
		} else {
			successResponse(r, response);
		}
	}
	
	// Handle delete route uri
	public void deleteRoute(HttpExchange r) throws IOException, JSONException {
		String body = Utils.convert(r.getRequestBody());
		JSONObject json_body = null;
		
		// Try convert request string to JSON type
		try {
			json_body = new JSONObject(body);
		} catch (JSONException ex) {
			errorResponse(r, 400, false);
			return;
		}
		
		String name1;
		String name2;
		// Check if the required parameters are in the JSON body
	    if (json_body.has("roadName1") && json_body.has("roadName2")){
	    	name1 = json_body.getString("roadName1");
	    	name2 = json_body.getString("roadName2");
		} else {
			errorResponse(r, 400, false);
			return;
		}
	    
	    // Check if the required parameters are not empty
	    if (name1.isEmpty() || name2.isEmpty()) {
	    	errorResponse(r, 400, false);
			return;
		}
	    
	    // Get the response from database
	    String response = db.deleteRoute(name1, name2);
		if (response.length() == 3) {
			errorResponse(r, Integer.parseInt(response), false);
		} else {
			successResponse(r, response);
		}
	}
	
	// Handle delete route uri
	public void navigation(HttpExchange r) throws IOException, JSONException {
		String body = Utils.convert(r.getRequestBody());
		JSONObject json_body = null;
		
		// Try convert request string to JSON type
		try {
			json_body = new JSONObject(body);
		} catch (JSONException ex) {
			errorResponse(r, 400, true);
			return;
		}
		
		String driveruid;
		String passengeruid;
		// Check if the required parameters are in the JSON body
	    if (json_body.has("driveruid") && json_body.has("passengeruid")){
	    	driveruid = json_body.getString("driveruid");
	    	passengeruid = json_body.getString("passengeruid");
		} else {
			errorResponse(r, 400, true);
			return;
		}
	    
	    // Check if the required parameters are not empty
	    if (driveruid.isEmpty() || passengeruid.isEmpty()) {
	    	errorResponse(r, 400, true);
			return;
		}
	    
	    // Get the response from database
	    String response = db.navigation(driveruid, passengeruid);
		if (response.length() == 3) {
			errorResponse(r, Integer.parseInt(response), true);
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
		} else if (type == 409) {
			status = "CONFLICT";
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
