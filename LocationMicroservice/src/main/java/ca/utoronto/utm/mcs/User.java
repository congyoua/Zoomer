package ca.utoronto.utm.mcs;

import java.io.IOException;
import java.io.OutputStream;

import org.json.*;

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
            if (r.getRequestURI().toString().equals("/user") && r.getRequestMethod().equals("PUT")) {
            	putUser(r);
            	return;
            } else if (r.getRequestURI().toString().equals("/user") && r.getRequestMethod().equals("DELETE")) {
            	deleteUser(r);
            	return;
            }
            // Invalid URI
            errorResponse(r, 400);
        } catch (Exception e) {
        	errorResponse(r, 500);
        }
	}
	
	// Handle the put user uri
	public void putUser(HttpExchange r) throws IOException, JSONException {
		String body = Utils.convert(r.getRequestBody());
		JSONObject json_body = null;
		
		// Try convert request string to JSON type
		try {
			json_body = new JSONObject(body);
		} catch (JSONException ex) {
			errorResponse(r, 400);
			return;
		}
		
		String uid;
		String response;
		boolean is_driver;
		// Check if the required parameters are in the JSON body
	    if (json_body.has("uid") && json_body.has("is_driver")){
	    	uid = json_body.getString("uid");
	    	is_driver = json_body.getBoolean("is_driver");
		} else {
			errorResponse(r, 400);
			return;
		}
	    
	    // Check if the required parameters are not empty
	    if (uid.isEmpty()) {
	    	errorResponse(r, 400);
			return;
		}
	    
	    // Get the response from database
	    int status = db.addUser(uid, is_driver);
		if (status == 200) {
			response = "{\"status\": \"OK\"}";
			successResponse(r, response);
		} else if (status == 400){
			response = "{\"status\": \"EXIST\"}";
			r.getResponseHeaders().set("Content-Type", "appication/json");
			r.sendResponseHeaders(status, response.length());
			OutputStream os = r.getResponseBody();
	        os.write(response.getBytes());
	        os.close();
		} else {
			errorResponse(r, status);
		}
	}
	
	// Handle the delete user uri
	public void deleteUser(HttpExchange r) throws IOException, JSONException {
		String body = Utils.convert(r.getRequestBody());
		JSONObject json_body = null;
		
		// Try convert request string to JSON type
		try {
			json_body = new JSONObject(body);
		} catch (JSONException ex) {
			errorResponse(r, 400);
			return;
		}
		
		String uid;
		// Check if the required parameters are in the JSON body
	    if (json_body.has("uid")){
	    	uid = json_body.getString("uid");
		} else {
			errorResponse(r, 400);
			return;
		}
	    
	    // Check if the required parameters are not empty
	    if (uid.isEmpty()) {
	    	errorResponse(r, 400);
			return;
		}
	    
	    // Get the response from database
	    String response = db.deleteUser(uid);
		if (response.length() == 3) {
			errorResponse(r, Integer.parseInt(response));
		} else {
			successResponse(r, response);
		}
	}
	
	// Error response generator
	public void errorResponse(HttpExchange r, int type) throws IOException {
		String status, response;
		
		// Determine error type
		if (type == 500) {
			status = "INTERNAL_SERVER_ERROR";
		} else if (type == 404) {
			status = "NOT_FOUND";
		} else {
			status = "BAD_REQUEST";
		}
		
		// Send back to user
		response = "{\"status\": \""+ status + "\"}";
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
