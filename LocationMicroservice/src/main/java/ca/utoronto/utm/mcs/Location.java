package ca.utoronto.utm.mcs;

import java.io.IOException;
import java.io.OutputStream;

import org.json.*;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class Location implements HttpHandler{
	private static Database db;
	
	public Location(Database database) {
		// Get the database class
		db = database;
	}
	
	// Main function to handle different URIs
	public void handle(HttpExchange r) throws IOException{
		boolean data = false;
		try {
            if (r.getRequestURI().toString().contains("/location/") && r.getRequestMethod().equals("GET")) {
            	// Check uid parameter
            	String uid = r.getRequestURI().getPath().replace("/location/","");
            	data = true;
    			if(!(uid.contains("/")||uid.isEmpty())) {
    				getLocation(r, uid);
                	return;
    			}
            } else if (r.getRequestURI().toString().contains("/location/") && r.getRequestMethod().equals("PATCH")) {
            	// Check uid parameter
            	String uid = r.getRequestURI().getPath().replace("/location/","");
    			if(!(uid.contains("/")||uid.isEmpty())) {
    				patchLocation(r,uid);
                	return;
    			}
            } else if (r.getRequestURI().toString().contains("/NearbyDriver/") && r.getRequestMethod().equals("GET")) {
            	// Check uid parameter
            	String uid = r.getRequestURI().getPath().replace("/NearbyDriver/","");
            	data = true;
    			if(!(uid.contains("/")||uid.isEmpty())) {
    				nearbyDriver(r, uid);
                	return;
    			}
            }
            // Invalid URI
            errorResponse(r, 400, data);
        } catch (Exception e) {
        	errorResponse(r, 500, data);
        }
	}
	
	// Handle the get location uri
	public void getLocation(HttpExchange r, String uid) throws IOException{
	    String response = db.getLocation(uid);
	    // Process response from database
		if (response.length() == 3) {
			errorResponse(r, Integer.parseInt(response), true);
		} else {
			successResponse(r, response);
		}
	}
	
	// Handle the update location uri
	public void patchLocation(HttpExchange r, String uid) throws IOException, NumberFormatException, JSONException{
		String body = Utils.convert(r.getRequestBody());
		JSONObject json_body = null;
	    
		// Try convert request string to JSON type
	    try {
			json_body = new JSONObject(body);
		} catch (JSONException ex) {
			errorResponse(r, 400, false);
			return;
		}
	    
	    String street;
	    float longitude, latitude;
	    // Check if the required parameters are in the JSON body
	    if (json_body.has("longitude") && json_body.has("latitude") && json_body.has("street_at")){
	    	longitude = Float.parseFloat(json_body.getString("longitude"));
	    	latitude = Float.parseFloat(json_body.getString("latitude"));
	    	street = json_body.getString("street_at");
		} else {
			errorResponse(r, 400, false);
			return;
		}
	    
	    // Check if the required parameters are not empty
	    if (street.isEmpty()) {
	    	errorResponse(r, 400, false);
			return;
		}
	    
	    // Get the response from database
	    String response = db.updateLocation(uid, longitude, latitude, street);
		if (response.length() == 3) {
			errorResponse(r, Integer.parseInt(response), false);
		} else {
			successResponse(r, response);
		}
	}
	
	// Handle get nearby driver uri
	public void nearbyDriver(HttpExchange r, String uid) throws IOException, NumberFormatException, JSONException{	    
	    float radius = -1;
	    String query = r.getRequestURI().getQuery();
	    
	    // Retrieve the url parameter
	    if (query != null) {
	      for (String param : query.split("&")) {
	        String[] entry = param.split("=");
	        if (entry.length > 1 && entry[0].equals("radius")) {
	          radius = Float.parseFloat(entry[1]);
	        }
	      }
	    }
	    
	    // Check if the required url parameter exists
	    if (radius == -1) {
	    	errorResponse(r, 400, true);
	    	return;
	    }
	    
	    // Get the response from database
	    String response = db.nearbyDriver(uid, radius);
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
