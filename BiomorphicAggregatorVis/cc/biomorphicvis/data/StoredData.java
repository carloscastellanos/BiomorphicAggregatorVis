/**
 * 
 */
package cc.biomorphicvis.data;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.TimeZone;

/**
 * @author carlos
 *
 */
public class StoredData
{
	private String dbPath = null;
	private String driver = null;
	private Connection connection = null;
	private PreparedStatement pstatement = null;
	private boolean connected = false;
	private static final String psql = "SELECT image_url, title, search_term, file_path FROM searchfeeds where " +
			"search_term=? ORDER BY date_time desc limit ?";
	
	public StoredData(String dbPath, String driver) {
		this.dbPath = dbPath;
		this.driver = driver;
	}
	
	public StoredData() {
	}
	
	public void setDriver(String dr) {
		this.driver = dr;
	}
	
	public void setDBLocation(String path) {
		this.dbPath = path;
	}
	
	private void getConnection() {
		try {
			// JDBC Driver to Use
			Class.forName(driver);
    
			// Create Connection Object to SQLite Database
			// If you want to only create a database in memory, exclude the +fileName
			connection = DriverManager.getConnection("jdbc:sqlite:"+dbPath);
			connected = true;
		} catch(SQLException e) {
			// Print some generic debug info
			System.out.println(e.getMessage());
			e.printStackTrace();
			connected = false;
		} catch(ClassNotFoundException cnfe) {
			// Print some generic debug info
			System.out.println(cnfe.getMessage());
			connected = false;
			
		}
	}
	
	public boolean isConnected() {
		return connected;
	}
	
	private void createPreparedStatement(String sql) {
		if(connection == null || connected == false) {
			getConnection();
		}
		try {
			this.pstatement = connection.prepareStatement(sql);
		} catch(SQLException sqle) {
			// Print some generic debug info
			System.out.println(sqle.getMessage());
			sqle.printStackTrace();
		}
	}
	
	// get everything (not receommended)
	public ArrayList<HashMap<String,String>> retrieveAll() {
		ArrayList<HashMap<String,String>> elems = new ArrayList<HashMap<String,String>>();
		try {
			if(connection == null || connected == false) {
				getConnection();
			}
			Statement statement = connection.createStatement();
			ResultSet rs = statement.executeQuery("SELECT * FROM searchfeeds");
			// add the results to the ArrayList
			while(rs.next()) {
				HashMap<String, String> hash = new HashMap<String, String>(5);
				hash.put("imageUrl", rs.getString("image_url"));
				hash.put("title", rs.getString("title"));
				hash.put("searchTerm", rs.getString("search_term"));
				hash.put("fileName", rs.getString("file_path"));
				// get the date
				java.sql.Date sqlDate = rs.getDate("date_time");
				TimeZone tz = TimeZone.getTimeZone("GMT");
				DateFormat df = DateFormat.getDateTimeInstance();
				df.setTimeZone(tz);
				// add date to the hashmap
				hash.put("timestamp", df.format(sqlDate));
				elems.add(hash);
			}
		} catch(SQLException se) {
			System.out.println(se.getMessage());
			se.printStackTrace();
		}
		return elems;
	}
	
	// use a prepared statement to access the data
	public ArrayList<HashMap<String,String>> retrieve(String[] terms, int limit) {
		int num = limit*100; // get more than we need in order to add randomness
		ArrayList<HashMap<String,String>> finalelems = new ArrayList<HashMap<String,String>>(limit*terms.length);
		if(pstatement == null)
			createPreparedStatement(psql);
		for(int i=0; i<terms.length; i++) {
			ArrayList<HashMap<String,String>> elems = new ArrayList<HashMap<String,String>>(num);
			try {
				// add the provided search term and limit to the prepared statement
				pstatement.setString(1, terms[i]);
				pstatement.setInt(2, num); 
				ResultSet rs = pstatement.executeQuery();
				// add the results to the ArrayList
				while(rs.next()) { // iterate throug each search term result
					HashMap<String, String> hash = new HashMap<String, String>(4);
					hash.put("imageUrl", rs.getString("image_url"));
					hash.put("title", rs.getString("title"));
					hash.put("searchTerm", rs.getString("search_term"));
					hash.put("fileName", rs.getString("file_path"));
					elems.add(hash);
				}
				Collections.shuffle(elems); // randomize it now that its filled
				for(int x=0; x<limit; x++)
					finalelems.add(elems.get(x));
			} catch(SQLException s) {
				System.out.println(s.getMessage());
				s.printStackTrace();
			}
		}

		return finalelems;
	}
}
