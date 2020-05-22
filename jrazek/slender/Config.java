package jrazek.slender;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.file.YamlConfiguration;

public class Config{
	private Main main;
	private Material join_it;
	private Material leaveItem;
	private Material slenderHideIt;
	private Material lighter_item;
	private Path arenas_dir;
	private List<File> arenas = new ArrayList<File>();
	private float damageRate;
	private int gameTime;
	private int lobbyCountDown;
	private double fov; //angle where the slender is seen
	private List <Sound> slenderSounds = new ArrayList<Sound>();
	private List <Sound> randomSounds = new ArrayList<Sound>();
	private int randomSoundChance;
	private int slenderCaptureDistance;
	private List <String> allowedCommands = new ArrayList<String>();
	
	//SQL
	ConnInfo sql = new ConnInfo();
	public class ConnInfo{
		public String host, dbname, user, password;
		public int port;
	}
	
	Config(Main m){
		//List<String> emptyArr = new ArrayList<String>();
		main = m;
		main.getConfig().addDefault("database-host", "localhost");
		main.getConfig().addDefault("database-name", "slender");
		main.getConfig().addDefault("database-user", "user");
		main.getConfig().addDefault("database-password", "password");
		main.getConfig().addDefault("database-port", 3306);
		
		main.getConfig().addDefault("join-item", "COMPASS");
		main.getConfig().addDefault("leave-item", "REPEATER");
		main.getConfig().addDefault("arenas_dir", "arenas");
		main.getConfig().addDefault("fov-angle", 1);
		main.getConfig().addDefault("slender-capture-distance", 1);
		main.getConfig().addDefault("damage-rate", 1);
		main.getConfig().addDefault("game-time", 480);
		main.getConfig().addDefault("lobby-countdown", 20);
		main.getConfig().addDefault("slender-hide-item", "NETHER_STAR");
		main.getConfig().addDefault("lighter-item", "GOLD_INGOT");
		main.getConfig().addDefault("slender-sounds", new ArrayList<String>());
		main.getConfig().addDefault("random-sounds", new ArrayList<String>());
		main.getConfig().addDefault("random-sound-chance", 2);
		main.getConfig().addDefault("allowed-commands", new ArrayList<String>());
		main.getConfig().options().copyDefaults(true);
		main.saveConfig();
		main.reloadConfig();

		sql.host = main.getConfig().getString("database-host");
		sql.dbname = main.getConfig().getString("database-name");
		sql.user = main.getConfig().getString("database-user");
		sql.password = main.getConfig().getString("database-password");
		sql.port = main.getConfig().getInt("database-port");
		
		
		join_it = Material.getMaterial(main.getConfig().getString("join-item"));
		leaveItem = Material.getMaterial(main.getConfig().getString("leave-item"));
		slenderHideIt = Material.getMaterial(main.getConfig().getString("slender-hide-item"));
		lighter_item = Material.getMaterial(main.getConfig().getString("lighter-item"));

		
		arenas_dir = Paths.get("./plugins/Slender/"+main.getConfig().getString("arenas_dir"));
		fov = Double.parseDouble(main.getConfig().getString("fov-angle"));
		gameTime = main.getConfig().getInt("game-time");
		lobbyCountDown = main.getConfig().getInt("lobby-countdown");
		damageRate = Float.parseFloat(main.getConfig().getString("damage-rate"));
		randomSoundChance = main.getConfig().getInt("random-sound-chance");
		slenderCaptureDistance = main.getConfig().getInt("slender-capture-distance");
		
		if(main.getConfig().getList("slender-sounds") != null) {
			List<String> s = main.getConfig().getStringList("slender-sounds");
			try {
				for(String str : s) {
					slenderSounds.add(Sound.valueOf(str));
				}
			}catch(IllegalArgumentException e) {
				e.printStackTrace();
				//System.out.println("\nINSERT VALID SOUNDS!");
			}
		}

		if(main.getConfig().getList("random-sounds") != null) {
			List<String> s = main.getConfig().getStringList("random-sounds");
			try {
				for(String str : s) {
					randomSounds.add(Sound.valueOf(str));
				}
			}catch(IllegalArgumentException e) {
				e.printStackTrace();
				//System.out.println("\nINSERT VALID SOUNDS!");
			}
		}
		
		if(main.getConfig().getList("allowed-commands") != null) {
			List<String> s = main.getConfig().getStringList("allowed-commands");
			try {
				for(String str : s) {
					allowedCommands.add(str);
				}
			}catch(IllegalArgumentException e) {
				e.printStackTrace();
				//System.out.println("\nINSERT VALID SOUNDS!");
			}catch(NullPointerException e) {
				e.printStackTrace();
				//System.out.println("\nINSERT VALID SOUNDS!");
			}
		}
		
		if(!Files.exists(arenas_dir)) {
			System.out.print("creating new directory " + arenas_dir.toString());         
	        File newFolder = new File(arenas_dir.toString());
	        if(newFolder.mkdir()) {
	        	System.out.print("Directory created successfully");
				loadArenas();
	        }
	        else {
	        	System.out.print("error has occured while creating directory!");
	        }
		}
		else {
			loadArenas();
		}

	}
	public void loadArenas() {
		arenas = new ArrayList<File>();

		File folder = new File(arenas_dir.toString());
		File[] listOfFiles = folder.listFiles();
		
		for (File file : listOfFiles) {
		    if (file.isFile()) {
		        System.out.println(file.getName());
		        arenas.add(file);
	        	System.out.print("Loading arena!");
		    }
		}


	}
	public void createArena(String arenaName) {
		File f = new File(arenas_dir.toString() + "/" + arenaName+".yml");
		if(!f.exists()) {
			YamlConfiguration yamlF = YamlConfiguration.loadConfiguration(f);
			yamlF.set("spawns", "");
			yamlF.set("pages", "");
			yamlF.set("lobby", "");
			yamlF.set("endLocation", "");
			yamlF.set("minPlayers", 2);
			try {
				yamlF.save(f);
			}catch(IOException e){
				e.printStackTrace();
			}finally {
				System.out.print("Creating default file for " + arenaName.toString());
				loadArenas();
				main.loadArenas();
			}
		}else {
			System.out.print(arenas_dir.toString() + " already exists!!");
		}
	}
	public Material getJoinItem() {
		return join_it;
	}
	public List<File> getArenaFiles(){
		return arenas;
	}
	public Path getArenasPath(){
		return arenas_dir;
	}
	public double getFovAngle() {
		return fov;
	}
	public float getDamageRate() {
		return damageRate;
	}
	public int getGameTime() {
		return gameTime;
	}
	public Material getSlenderHideItem() {
		return slenderHideIt;
	}
	public Material getLeaveItem() {
		return leaveItem;
	}
	public Material getLighterItem() {
		return lighter_item;
	}
	public int getLobbyCountDown() {
		return lobbyCountDown;
	}
	public List<String> getAllowedCommands() {
		return allowedCommands;
	}
	public List<Sound> getSlenerSounds() {
		return slenderSounds;
	}
	public List<Sound> getRandomSounds() {
		return randomSounds;
	}
	public int getRandSoundChance() {
		return randomSoundChance;
	}
	public int getSlenderCaptureDistance() {
		return slenderCaptureDistance;
	}
	public ConnInfo getConnInfo() {
		return sql;
	}
}
;