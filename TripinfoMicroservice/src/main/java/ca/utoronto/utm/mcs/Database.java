package ca.utoronto.utm.mcs;

import com.mongodb.client.model.*;
import com.mongodb.client.model.Filters;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;

import org.bson.Document;
import org.bson.types.ObjectId;

import com.mongodb.BasicDBObject;
import com.mongodb.WriteResult;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;

public class Database {
	
	private MongoClient db;
	private MongoDatabase mongodb;
	
	// Setup the database
	public Database() {
		db = MongoClients.create("mongodb://host.docker.internal:27017/");
		mongodb = db.getDatabase("trip");
	}
	
	// Check whether a specific trip exists
	public boolean findTrip(String id) {
		BasicDBObject query = new BasicDBObject();
		MongoCursor<Document> result;
		query.put("_id", new ObjectId(id));
		result = mongodb.getCollection("trips").find(query).iterator();
		return result.hasNext();
	}
	
	// Get the trip driver and passenger
	public String[] getTrip(String id) {
		String[] uid = {"empty", "empty"};
		BasicDBObject query = new BasicDBObject();
		query.put("_id", new ObjectId(id));
		MongoCursor<Document> result = mongodb.getCollection("trips").find(query).iterator();
		if (result.hasNext()) {
			Document doc = result.next();
			uid[0] = doc.get("driver").toString();
			uid[1] = doc.get("passenger").toString();
		}
		return uid;
	}
	
	// Insert new trip to database
	public String addTrip(String driver, String passenger, long time) {
		Document doc = new Document()
				.append("distance", null).append("totalCost", null)
				.append("startTime", time).append("endTime", null)
				.append("timeElapsed", null).append("driver", driver)
				.append("driverPayout", null).append("passenger", passenger)
				.append("discount", null);
		mongodb.getCollection("trips").insertOne(doc);
		ObjectId id = (ObjectId) doc.get("_id");
		
		// Check if the insert success
		if (id == null) return "500";
		
		String response = "{\"status\": \"OK\", \"data\": \"" + id.toString() + "\"}";
		return response;
	}
	
	// Update the trip in database
	public String finishTrip(String id, int distance, long endTime, String time, int discount, float cost, float payout) {
		
		// Check if the id is valid
		if(!ObjectId.isValid(id)) return "400";
		
		// Check if the trip exists
		if (!findTrip(id)) {
			return "404";
		}
		
		BasicDBObject query = new BasicDBObject();
		query.put("_id", new ObjectId(id));
		
		Document doc = new Document()
				.append("distance", distance).append("totalCost", cost)
				.append("endTime", endTime).append("timeElapsed", time)
				.append("driverPayout", payout).append("discount", discount);
		UpdateResult result = mongodb.getCollection("trips").updateOne(query, new Document("$set", doc));
		
		// Check if the update success
		if (result.getModifiedCount()==0) return "500";
				
		String response = "{\"status\": \"OK\"}";
		return response;
	}
	
	// Get all the driver trips from database
	public String driverTrip(String uid) {
		BasicDBObject query = new BasicDBObject();
		String temp="";
		
		query.put("driver", uid);
		FindIterable<Document> result = mongodb.getCollection("trips").find(query);
		
		// Generate the trip lists
		for (Document doc : result) {
			temp += "{\"_id\":\"" + doc.get("_id") + "\", \"distance\": " + doc.get("distance") + ", \"driverPayout\": " + doc.get("driverPayout") + ", "
					+ "\"startTime\": " + doc.get("startTime") + ", \"endTime\": " + doc.get("endTime") + ", \"timeElapsed\": \"" 
					+ doc.get("timeElapsed") + "\", \"passenger\": \"" + doc.get("passenger") + "\"}, ";
		}
		
		// No trips found
		if (temp=="") return "404";
		
		temp = temp.substring(0, temp.length()-2);
		String response = "{\"status\": \"OK\", \"data\": {\"trips\":[" + temp +"]}}";
		
		return response;
	}
	
	// Get all the passenger trips from database
	public String passengerTrip(String uid) {
		BasicDBObject query = new BasicDBObject();
		String temp="";
		
		query.put("passenger", uid);
		FindIterable<Document> result = mongodb.getCollection("trips").find(query);
		
		// Generate the trip lists
		for (Document doc : result) {
			temp += "{\"_id\":\"" + doc.get("_id") + "\", \"distance\": " + doc.get("distance") + ", \"totalCost\": " + doc.get("totalCost") + ", "
					+ "\"discount\": " + doc.get("discount") + ", \"startTime\": " + doc.get("startTime") + ", \"endTime\": " + doc.get("endTime") 
					+ ", \"timeElapsed\": \"" + doc.get("timeElapsed") + "\", \"driver\": \"" + doc.get("driver") + "\"}, ";
		}
		
		// No trips found
		if (temp=="") return "404";
		
		temp = temp.substring(0, temp.length()-2);
		String response = "{\"status\": \"OK\", \"data\": {\"trips\":[" + temp +"]}}";
		
		return response;
	}
}
