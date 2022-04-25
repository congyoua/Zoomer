package ca.utoronto.utm.mcs;

import static org.neo4j.driver.Values.parameters;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.Transaction;
import org.neo4j.driver.Value;
import org.neo4j.driver.types.*;

import com.sun.net.httpserver.HttpExchange;

public class Database {
	private Driver driver;
	private String uriDb;
	
	// Setup the database
	public Database() {
		uriDb = "bolt://host.docker.internal:7687";
		driver = GraphDatabase.driver(uriDb, AuthTokens.basic("neo4j", "123456"));
	}
	
	// Check whether a specific user exists
	public boolean findUID(Transaction tx, String uid) {
		Result userExist = tx.run("MATCH (a:user{uid:$x}) Return a.uid", 
				parameters("x", uid));
		return userExist.hasNext();
	}
	
	// Check whether a specific road exists
	public boolean findRoad(Transaction tx, String name) {
		Result roadExist = tx.run("MATCH (a:road{name:$x}) Return a.name", 
				parameters("x", name));
		return roadExist.hasNext();
	}
	
	// Check whether a specific route exists
	public boolean findRoute(Transaction tx, String name1, String name2) {
		Result routeExist = tx.run("MATCH (a:road{name:$x})-[:ROUTE_TO]->(:road {name:$y}) "
				+ "Return a.name", parameters("x", name1, "y", name2));
		return routeExist.hasNext();
	}
	
	// Get the is_traffic attribute from the road
	public boolean getTraffic(Transaction tx, String road) {
		Result result = tx.run("MATCH (a:road{name:$x}) Return a.is_traffic",
				parameters("x", road));
		Record record = result.next();
		return record.get("a.is_traffic").asBoolean();
	}
	
	// Insert new user to database
	public int addUser(String uid, boolean is_driver) {
		try (Session session = driver.session()){
			try (Transaction tx = session.beginTransaction()) {
				// Check if the user exists
				if (findUID(tx, uid)) {
					session.close();
					return 400;
				} else {
					tx.run("CREATE (:user {uid:$x, longitude:0, latitude:0, street_at:\"\", is_driver: $y}) ", 
							parameters("x", uid, "y", is_driver));
					tx.commit();
					session.close();
					return 200;
				}
			} catch(Exception e) {
				session.close();
				return 500;
			}
		} catch(Exception e) {
			return 500;
		}
	}
	
	// Delete a existing user from database
	public String deleteUser(String uid) {
		try (Session session = driver.session()){
			try (Transaction tx = session.beginTransaction()) {
				// Check if the user exists
				if (!findUID(tx, uid)) {
					session.close();
					return "404";
				}
				
				tx.run("MATCH (a:user{uid:$x}) DELETE a", 
						parameters("x", uid));
				tx.commit();
				// Generate response message
				String response = "{\"status\": \"OK\"}";
				session.close();
				return response;
			} catch(Exception e) {
				session.close();
				return "500";
			}
		} catch(Exception e) {
			return "500";
		}
	}
	
	// Get the location of user from database
	public String getLocation(String uid) {
		try (Session session = driver.session()){
			try (Transaction tx = session.beginTransaction()) {
				// Check if the user exists
				if (!findUID(tx, uid)) {
					session.close();
					return "404";
				}
				// Run the match query and extract the location of user with their id
				Result result = tx.run("MATCH (a:user{uid:$x}) Return a.longitude, a.latitude, a.street_at", 
						parameters("x", uid));
				Record record = result.next();
				float longitude=record.get("a.longitude").asFloat();
				float latitude=record.get("a.latitude").asFloat();
				String street = record.get("a.street_at").asString();
				// Combine all the info and create the response body format with it
				String response = "{\"status\": \"OK\", \"data\":{\"longitude\": "+ longitude +", \"latitude\": "+ latitude +", \"street_at\": \""+ street +"\"}}";
				session.close();
				return response;
			} catch(Exception e) {
				session.close();
				return "500";
			}
		} catch(Exception e) {
			return "500";
		}
	}
	
	// Update the location of user in the database
	public String updateLocation(String uid, float longitude, float latitude, String street) {
		try (Session session = driver.session()){
			try (Transaction tx = session.beginTransaction()) {
				// Check if the user exists
				if (!findUID(tx, uid)) {
					session.close();
					return "404";
				}
				// Run the match query and set the location
				tx.run("MATCH (a:user{uid:$x}) SET a.longitude=$y, a.latitude=$z, a.street_at=$w", 
						parameters("x", uid, "y", longitude, "z", latitude, "w", street));
				tx.commit();
				// Generate response message
				String response = "{\"status\": \"OK\"}";
				session.close();
				return response;
			} catch(Exception e) {
				session.close();
				return "500";
			}
		} catch(Exception e) {
			return "500";
		}
	}
	
	// Get the nearby driver of a user from database
	public String nearbyDriver(String uid, float radius) {
		try (Session session = driver.session()){
			try (Transaction tx = session.beginTransaction()) {
				// Check if the user exists
				if (!findUID(tx, uid)) {
					session.close();
					return "404";
				}
				Result result;
				Record record;
				// Run the match query to check if user is passenger
				result = tx.run("MATCH (a:user{uid:$x,is_driver:True}) Return a.uid",
						parameters("x", uid));
				if (result.hasNext()) {
					session.close();
					return "400";
				}
				String driverList = "";
				// Run the match query and extract the driver nearby
				result = tx.run("MATCH (a:user{uid:$x}), (d:user) WHERE d.is_driver=true AND sqrt((a.longitude-d.longitude)^2 + (a.latitude-d.latitude)^2)<=$y Return d.uid, d.longitude, d.latitude, d.street_at", 
						parameters("x", uid, "y", radius));
				
				// Create the list of driver
				while (result.hasNext()) {
					record = result.next();
					if(result.hasNext()){
						driverList += "\"" + record.get("d.uid").asString() + "\":{\"longitude\": " + record.get("d.longitude").asFloat() + ", \"latitude\": " + record.get("d.latitude").asFloat() + ", \"street_at\": \"" + record.get("d.street_at").asString() + "\"}, ";
					} else{
						driverList += "\"" + record.get("d.uid").asString() + "\":{\"longitude\": " + record.get("d.longitude").asFloat() + ", \"latitude\": " + record.get("d.latitude").asFloat() + ", \"street_at\": \"" + record.get("d.street_at").asString() + "\"}";
					}
				}
				
				// Check if any drivers are nearby
				if (driverList=="") return "404";
				
				// Combine all the info and create the response body format with it
				String response = "{\"status\": \"OK\", \"data\": {" + driverList +"}}";
				session.close();
				return response;
			} catch(Exception e) {
				session.close();
				return "500";
			}
		} catch(Exception e) {
			return "500";
		}
	}
	
	// Insert a new road to database
	public String addRoad(String name, boolean has_traffic) {
		try (Session session = driver.session()){
			try (Transaction tx = session.beginTransaction()) {
				String response;
				// Check if the road exists and update it, else create the road
				if (findRoad(tx, name)) {
					tx.run("MATCH (a:road{name:$x}) SET a.is_traffic=$y", 
							parameters("x", name, "y", has_traffic));
					response = "{\"status\": \"Updated\"}";
				} else {
					tx.run("CREATE (:road {name:$x, is_traffic:$y}) ", 
							parameters("x", name, "y", has_traffic));
					response = "{\"status\": \"OK\"}";
				}
				tx.commit();
				session.close();
				return response;
			} catch(Exception e) {
				session.close();
				return "500";
			}
		} catch(Exception e) {
			return "500";
		}
	}
	
	// Insert a new route to databse
	public String addRoute(String name1, String name2, boolean is_traffic, int time) {
		try (Session session = driver.session()){
			try (Transaction tx = session.beginTransaction()) {
				// Check if two roads exist
				if (!findRoad(tx, name1) || !findRoad(tx, name2)) {
					session.close();
					return "404";
				} 
				
				// Check if the route already exists
				if (findRoute(tx, name1, name2)) {
					session.close();
					return "409";
				}
				
				// Create the route
				tx.run("MATCH (a:road), (b:road) Where a.name=$x AND b.name=$y "
						+ "CREATE (a)-[:ROUTE_TO {travel_time:$z, is_traffic:$w}]->(b)", 
						parameters("x", name1, "y", name2, "z", time, "w", is_traffic));
				tx.commit();
				session.close();
				String response = "{\"status\": \"OK\"}";
				return response;
			} catch(Exception e) {
				session.close();
				return "500";
			}
		} catch(Exception e) {
			return "500";
		}
	}
	
	// Delete a existing route from database
	public String deleteRoute(String name1, String name2) {
		try (Session session = driver.session()){
			try (Transaction tx = session.beginTransaction()) {
				// Check if the two roads exist
				if (!findRoad(tx, name1) || !findRoad(tx, name2)) {
					session.close();
					return "404";
				} 
				
				// Check if the route already exists
				if (!findRoute(tx, name1, name2)) {
					session.close();
					return "404";
				}
				
				// Delete route query
				tx.run("MATCH (a:road{name:$x})-[r:ROUTE_TO]->(b:road{name:$y}) DELETE r", 
						parameters("x", name1, "y", name2));
				tx.commit();
				session.close();
				String response = "{\"status\": \"OK\"}";
				return response;
			} catch(Exception e) {
				session.close();
				return "500";
			}
		} catch(Exception e) {
			return "500";
		}
	}
	
	// Find the navigation from the database
	public String navigation(String driveruid, String passengeruid) {
		try (Session session = driver.session()){
			try (Transaction tx = session.beginTransaction()) {
				// Check if the driver and passenger exist
				if (!findUID(tx, driveruid) || !findUID(tx, passengeruid)) {
					session.close();
					return "404";
				}
				Result result;
				Record record;
				// Find the street_at of driver
				result = tx.run("MATCH (a:user{uid:$x,is_driver:True}) Return a.street_at",
						parameters("x", driveruid));
				if (!result.hasNext()) {
					session.close();
					return "400";
				}
				record = result.next();
				String street_d = record.get("a.street_at").asString();
				
				// Find the street_at of passenger
				result = tx.run("MATCH (a:user{uid:$x,is_driver:False}) Return a.street_at",
						parameters("x", passengeruid));
				if (!result.hasNext()) {
					session.close();
					return "400";
				}
				record = result.next();
				String street_p = record.get("a.street_at").asString();
				
				// Get the traffic of the road driver is at
				boolean traffic_d = getTraffic(tx, street_d);
				
				String navList = "";
				// Get all the paths from driver to user
				result = tx.run("MATCH (s:road{name:$x})-[r*]-(t:road{name:$y}) \r\n" + 
								"RETURN r AS route, reduce(time = 0, n IN r | time + n.travel_time) AS time", 
						parameters("x", street_d, "y", street_p));
				List<Record> recordList = result.list();
				if (recordList.isEmpty()) return "404";
				// Get the shortest path
				Record best = Collections.min(recordList, Comparator.comparing(t->t.get("time").asInt()));
				int totalTime = best.get("time").asInt();
				List<Object> route = best.get("route").asList();
				
				String road;
				boolean traffic;
				int time;
				// Get the road information from the path
				for (int i = 0; i < route.size(); i++) {
					road = tx.run("MATCH (n) WHERE id(n) = $x RETURN n.name", parameters("x", ((Relationship)route.get(i)).endNodeId())).next().get("n.name").asString();
					time = ((Relationship)route.get(i)).get("travel_time").asInt();
					traffic = getTraffic(tx, road);
					
					if(i < route.size()-1){
						navList += "{\"street\": \"" + road + "\", \"time\": " + time + ", \"is_traffic\": " + traffic + "}, ";
					} else{
						navList += "{\"street\": \"" + road + "\", \"time\": " + time + ", \"is_traffic\": " + traffic + "}";
					}
				}
				
				// Combine all the info and create the response body format with it
				String response = "{\"status\": \"OK\", \"data\": {\"total_time\": " + totalTime + ", \"Route\": [{\"street\": \"" + street_d + "\", \"time\": 0, \"is_traffic\": "+ traffic_d + "}, " + navList +"]}}";
				session.close();
				return response;
			} catch(Exception e) {
				session.close();
				return "500";
			}
		} catch(Exception e) {
			return "500";
		}
	}
}
