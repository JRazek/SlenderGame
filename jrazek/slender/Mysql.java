package jrazek.slender;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import org.bukkit.entity.Player;

import com.mysql.jdbc.Statement;





public class Mysql{
	Mysql(String host, String database, String username, String password, int port){
		this.host = host;
		this.database = database;
		this.username = username;
		this.password = password;
		this.port = port;
		try {
			String test = ("jdbc:mysql://" + this.host + ":" + this.port + "/" +
					this.database);
			System.out.println(test);
			synchronized(this) {
				Class.forName("com.mysql.jdbc.Driver");
				conn = DriverManager.getConnection("jdbc:mysql://" + this.host + ":" + this.port + "/" +
				this.database, this.username, this.password);
			}
		}catch(SQLException e) {
			e.printStackTrace();
		}catch(IllegalStateException e) {
			e.printStackTrace();
		}catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}
	java.sql.Connection getConnection() {
		return conn;
	}
	private java.sql.Connection conn;
	private String host, database, username, password;
	private int port;
	
	public int newGame(Game g) {
		try {
			String query="INSERT INTO `games` VALUES (NULL, ?, ?, NULl)";
			PreparedStatement s;
			long time = System.currentTimeMillis()/1000;
            s = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
			s.setString(1, g.getArena().getName());
			s.setLong(2, time);
			s.execute();
			ResultSet rs = s.getGeneratedKeys();
            if(rs.next()){
                return rs.getInt(1);
            }
		}catch(SQLException e) {
			e.printStackTrace();
			return -1;
		}
		return -1;
	}
	public boolean endGame(Game g, int winner) {
		try {
			long time = System.currentTimeMillis()/1000;
			PreparedStatement s = conn.prepareStatement("UPDATE `games` SET `endTime` = ? WHERE `id` = ?");
			s.setLong(1, time);
			s.setLong(2, g.getDatabaseID());
			s.execute();
			if(winner == 0) {
				Player p = g.getSlender();
    			PreparedStatement st = conn.prepareStatement("INSERT INTO `pages` VALUES (?, ?, ?)");
    			st.setString(1, p.getUniqueId().toString());
    			st.setInt(2, g.getDatabaseID());
    			st.setInt(3, g.getPagesCollectedInt());
    			st.execute();
			}
			else if(winner == 1) {
	        	List<Player> players = g.getPlayers();
	        	for(Player p : players) {
	        		int pages = 0;
	        		if(g.getPagesCollected().get(p) != null) {
	        			pages = g.getPagesCollected().get(p).size();
	        		}
	    			PreparedStatement st = conn.prepareStatement("INSERT INTO `pages` VALUES (?, ?, ?)");
	    			st.setString(1, p.getUniqueId().toString());
	    			st.setInt(2, g.getDatabaseID());
	    			st.setInt(3, pages);
	    			st.execute();
	        	}
        	}
			return true;
		}catch(SQLException e) {
			e.printStackTrace();
			return false;
		}
	}
	public boolean pageSet(Player p, int quantity) {
		try {
			PreparedStatement s = conn.prepareStatement("INSERT INTO `pages` VALUES (?, ?, 0)");
			s.setString(1, p.getUniqueId().toString());
			s.setInt(2, quantity);
			s.execute();
			return true;
		}catch(SQLException e) {
			e.printStackTrace();
			return false;
		}
	}
}
