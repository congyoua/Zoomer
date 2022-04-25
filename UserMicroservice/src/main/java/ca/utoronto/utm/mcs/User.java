package ca.utoronto.utm.mcs;


import java.io.IOException;
import java.io.OutputStream;

import org.json.*;

import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;
import java.util.UUID;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class User implements HttpHandler{
	
	private Connection db;
	
	//connect to database
	public void connect(HttpExchange r) throws IOException {
		try {
			Class.forName("org.postgresql.Driver");
			db = DriverManager.getConnection("jdbc:postgresql://host.docker.internal:5432/root", "root", "123456");
		}catch (Exception e) {
			errorResponse(r, 500, false);
        }		
	}
	
	@Override
	public void handle(HttpExchange r) throws IOException {
		connect(r);
		try {
			String uri = r.getRequestURI().toString();
			
			//if the uri contains "/uesr/"
            if (uri.contains("/user/") && r.getRequestMethod().equals("GET")) {
            	String uid = r.getRequestURI().getPath().replace("/user/","");
    			if(uid.contains("/") || uid.isEmpty()) errorResponse(r, 400, false);
    			//call GET handler
            	getUser(r,uid);
            } else if (uri.contains("/user/") && r.getRequestMethod().equals("PATCH")) {
            	String uid = r.getRequestURI().getPath().replace("/user/","");
    			if(uid.contains("/") || uid.isEmpty()) errorResponse(r, 400, false);
    			//call PATCH handler
    			editUser(r,uid);
            } else if (uri.equals("/register") && r.getRequestMethod().equals("POST")) {
            	//call register handler
            	register(r);
            }else if (uri.equals("/login") && r.getRequestMethod().equals("POST")) {
            	//call login handler
            	login(r);
            }
            errorResponse(r, 400, false);
        } catch (Exception e) {
        	errorResponse(r, 500, false);
        }
	}
	
	//get user handler
	public void getUser(HttpExchange r, String uid) throws IOException{
		try {
			//select specific user
			PreparedStatement stm = db.prepareStatement("select * from Users where uid = ?");
			stm.setInt(1, Integer.parseInt(uid));
		    ResultSet rs = stm.executeQuery();
		    if(!rs.next()) {
		    	errorResponse(r, 404, true);
		    	return;
		    }
		    //retrieve the info from db
		    String name = rs.getString("prefer_name");
		    String email = rs.getString("email");
		    int rides  = rs.getInt("rides");
		    boolean isDriver = rs.getBoolean("isDriver");
		    String availableCoupons = listToString(Arrays.asList((Integer[])rs.getArray("availableCoupons").getArray()));
		    String redeemedCoupons = listToString(Arrays.asList((Integer[])rs.getArray("redeemedCoupons").getArray()));
		    String response = "{\"status\": \"OK\", \"Data\":{\"name\": "+ name +", \"email\": "+ email + ", \"rides\": " + rides + 
		    		", \"isDriver\": " + isDriver +", \"availableCoupons\": "+ availableCoupons + ", \"redeemedCoupons\": "+ redeemedCoupons +"}}";			
		    rs.close();
			stm.close();
			successResponse(r, response);
		}catch (Exception e) {
        	errorResponse(r, 500, false);
        }
	}
	
	//register handler
	public void register(HttpExchange r) throws IOException{
		String body = Utils.convert(r.getRequestBody());
		//if the request body is empty, return bad request
		if(body.isEmpty()) {
			errorResponse(r, 400, false);
			return;
		}
		JSONObject json_body = null;
		
		
		String name = "";
		String email = "";
		String password = "";
		
		//parse request body
		try {
			json_body = new JSONObject(body);
			//parameters check
			if (json_body.has("name") && json_body.has("email") && json_body.has("password")){
				name = json_body.getString("name");
				email = json_body.getString("email");
				password = json_body.getString("password");
			} else {
				errorResponse(r, 400, false);
				return;
			}
		} catch (JSONException e) {
			//if the request body is not in json from, return bad request 
			errorResponse(r, 400, false);
			return;
		}
		
		//insert into the db
		try {
			PreparedStatement stm = db.prepareStatement("INSERT INTO Users(email,password,prefer_name,rides,isDriver,availableCoupons,redeemedCoupons)"
					+ " VALUES (?, ?, ?, ?,?,?,?)");
			stm.setString(1, email);
			stm.setString(2, password);
			stm.setString(3, name);
			stm.setInt(4, 0);
			stm.setBoolean(5, false);
			stm.setArray(6, db.createArrayOf("integer", new Integer[] {}));
			stm.setArray(7, db.createArrayOf("integer", new Integer[] {}));
			try {
				stm.executeUpdate();
			}catch (SQLException e) {
				errorResponse(r, 400, false);
				return;
			}
			stm.close();
		    String response = "{\"status\": \"OK\"}";
			successResponse(r, response);
		}catch (Exception e) {
        	errorResponse(r, 500, false);
        }
	}
	
	//login handler
	public void login(HttpExchange r) throws IOException{
		String body = Utils.convert(r.getRequestBody());
		//if the request body is empty, return bad request
		if(body.isEmpty()) {
			errorResponse(r, 400, false);
			return;
		}
		JSONObject json_body = null;
		
		String email = "";
		String pwd = "";
		
		//parse request body
		try {
			json_body = new JSONObject(body);
			//parameters check
			if (json_body.has("email") && json_body.has("password")){
				email = json_body.getString("email");
				pwd = json_body.getString("password");
			} else {
				errorResponse(r, 400, false);
				return;
			}
		} catch (JSONException e) {
			//if the request body is not in json from, return bad request 
			errorResponse(r, 400, false);
			return;
		}
		
		//retrieve info from db
		try {
			PreparedStatement stm = db.prepareStatement("select * from Users where email = ?");
			stm.setString(1, email);
		    ResultSet rs = stm.executeQuery();
		    if(!rs.next()) {
		    	errorResponse(r, 404, false);
		    	return;
		    }
		    String password = rs.getString("password");
		    rs.close();
		    stm.close();
		    if(password.equals(pwd)) {
		    	String response = "{\"status\": \"OK\"}";
				successResponse(r, response);
		    }else {
		    	
		    	errorResponse(r, 400, false);
		    }
		}catch (Exception e) {
        	errorResponse(r, 500, false);
        }
	}
	
	//PATCH user handler
	public void editUser(HttpExchange r, String uid) throws IOException{
		String body = Utils.convert(r.getRequestBody());
		//if the request body is empty, return bad request
		if(body.isEmpty()) {
			errorResponse(r, 400, false);
			return;
		}
		JSONObject json_body = null;
		String email = "";
		String password = "";
		int rides = 0;
		Integer[] availableCoupons;
		Integer[] redeemedCoupons;
		Boolean isDriver;
		try {
			json_body = new JSONObject(body);
			if (!(json_body.has("rides") || json_body.has("email") || json_body.has("password") || json_body.has("availableCoupons") ||
					json_body.has("redeemedCoupons"))){
				errorResponse(r, 400, false);
				return;
			}
			//retrieve info from db
			PreparedStatement stm = db.prepareStatement("select * from Users where uid = ?");
			stm.setInt(1, Integer.parseInt(uid));
		    ResultSet rs = stm.executeQuery();
		    if(!rs.next()) {
		    	errorResponse(r, 404, false);
		    	return;
		    }
		    isDriver = rs.getBoolean("isDriver");
		    password = rs.getString("password");
		    email = rs.getString("email");
		    rides = rs.getInt("rides");
		    availableCoupons = (Integer[])rs.getArray("availableCoupons").getArray();
		    redeemedCoupons = (Integer[])rs.getArray("redeemedCoupons").getArray();
		    rs.close();
			stm.close();
			//change the values with the input
			if(json_body.has("isDriver")) isDriver = json_body.getBoolean("isDriver");
			if(json_body.has("rides")) rides = json_body.getInt("rides");
			if(json_body.has("email")) email = json_body.getString("email");
			if(json_body.has("password")) password = json_body.getString("password");
			if(json_body.has("availableCoupons")) {
				JSONArray array = json_body.getJSONArray("availableCoupons");
				availableCoupons = new Integer[array.length()];
				for (int i = 0; i < array.length(); i++) {
					availableCoupons[i] = array.getInt(i);
		        }
			}
			if(json_body.has("redeemedCoupons")) {
				JSONArray array = json_body.getJSONArray("redeemedCoupons");
				redeemedCoupons = new Integer[array.length()];
				for (int i = 0; i < array.length(); i++) {
					redeemedCoupons[i] = array.getInt(i);
		        }
			}
			//update the record
			stm = db.prepareStatement("UPDATE Users SET email = ? , password=?, rides = ?, availableCoupons = ?, "
					+ "redeemedCoupons = ?, isDriver = ? WHERE uid = ?");
			stm.setString(1, email);
			stm.setString(2, password);
			stm.setInt(3, rides);
			stm.setArray(4, db.createArrayOf("integer", availableCoupons));
			stm.setArray(5, db.createArrayOf("integer", redeemedCoupons));
			stm.setBoolean(6, isDriver);
			stm.setInt(7, Integer.parseInt(uid));
			try {
				stm.executeUpdate();
			}catch (SQLException e) {
				errorResponse(r, 400, false);
				return;
			}
			stm.close();
		    String response = "{\"status\": \"OK\"}";
			successResponse(r, response);
		}catch (Exception e) {
        	errorResponse(r, 500, false);
        }
	}
	
	//helper that send success result
	public void successResponse(HttpExchange r, String res) throws IOException {
		r.getResponseHeaders().set("Content-Type", "appication/json");
        r.sendResponseHeaders(200, res.length());
        OutputStream os = r.getResponseBody();
        os.write(res.getBytes());
        os.close();
	}
	
	//helper that send error messages
	public void errorResponse(HttpExchange r, int type, boolean data) throws IOException {
		String status, response;
		
		if (type == 500) {
			status = "INTERNAL_SERVER_ERROR";
		} else if (type == 404) {
			status = "NOT_FOUND";
		} else {
			status = "BAD_REQUEST";
		}
		
		if (data) {
			response = "{\"status\": \""+ status + "\", \"data\":{} }";
		}else {
			response = "{\"status\": \""+ status + "\" }";
		}
		
		r.getResponseHeaders().set("Content-Type", "appication/json");
		r.sendResponseHeaders(type, response.length());
		OutputStream os = r.getResponseBody();
        os.write(response.getBytes());
        os.close();
	}
	
	//helper that transform a list to a string that be used in json
	public String listToString(List<Integer> list){
		String str = "[";
		ListIterator<Integer> iter = list.listIterator();
		while(iter.hasNext()) {
			iter.next();
			str += iter.toString();
		}
		str += "]";
		return str;
	}
}
