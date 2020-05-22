package jrazek.slender;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Rotation;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;


public class Arena {
	private String name;
	private List<Location>  spawns;
	private Location endLoc;
	private Location lobbyLoc;
	private File path;
	private Main main;
	boolean exists = true;
	private int minPlayers;
	
	List <Location> pages = new ArrayList<Location>();
	Arena(File arena_file, Config config, Main m){//this is the file of the arena but it is just ./arena.yml
		name = arena_file.getName().toString().replace(".yml", "");
		path = arena_file;
		main = m;
		loadConfig();
	}
	public void loadConfig() {
		pages = getPagesConfig();
		spawns = getSpawnsConfig();
		endLoc = getEndLocConfig();
		lobbyLoc = getLobbyConfig();
		minPlayers = getMinPlayersConfig();
	}
	public void setSpawn(Location loc) {
		YamlConfiguration yamlF = YamlConfiguration.loadConfiguration(path);
        int i = 1;
        System.out.print(path.toString());
        if(yamlF.getConfigurationSection("spawns") != null)
            i = yamlF.getConfigurationSection("spawns").getKeys(false).size()+1;
        System.out.print("found " + (i-1) + " spawns");
        String spawnsec = "spawns." + i + ".";
		yamlF.set(spawnsec + ".world", loc.getWorld().getName());
		yamlF.set(spawnsec + ".xLoc", loc.getBlockX());
		yamlF.set(spawnsec + ".yLoc", loc.getBlockY());
		yamlF.set(spawnsec + ".zLoc", loc.getBlockZ());
		saveConfig(yamlF);
	}
	public void setEndLoc(Location loc) {
		YamlConfiguration yamlF = YamlConfiguration.loadConfiguration(path);
        String spawnsec = "endLocation";
		yamlF.set(spawnsec + ".world", loc.getWorld().getName());
		yamlF.set(spawnsec + ".xLoc", loc.getBlockX());
		yamlF.set(spawnsec + ".yLoc", loc.getBlockY());
		yamlF.set(spawnsec + ".zLoc", loc.getBlockZ());
		saveConfig(yamlF);
	}
	public void setLobby(Location loc) {
		YamlConfiguration yamlF = YamlConfiguration.loadConfiguration(path);
        String spawnsec = "lobby";
		yamlF.set(spawnsec + ".world", loc.getWorld().getName());
		yamlF.set(spawnsec + ".xLoc", loc.getBlockX());
		yamlF.set(spawnsec + ".yLoc", loc.getBlockY());
		yamlF.set(spawnsec + ".zLoc", loc.getBlockZ());
		saveConfig(yamlF);
	}
	public void setPage(Player p) {
		Vector dirToDestination = p.getEyeLocation().getDirection().normalize();
		RayTraceResult rts = p.getWorld().rayTraceEntities(p.getEyeLocation(), dirToDestination, 3, Entity -> (Entity instanceof ItemFrame));
		if(rts != null) {
			if(rts.getHitEntity() != null) {
				if(rts.getHitEntity().getName().toString().equals("Item Frame")) {
					ItemFrame frame = (ItemFrame)rts.getHitEntity();
					if(frame.getItem().getType().equals(Material.PAPER)) {
						p.sendMessage("success");
						frame.setRotation(Rotation.NONE);
						Location loc = frame.getLocation();
						
						YamlConfiguration yamlF = YamlConfiguration.loadConfiguration(path);
				        int i = 1;
				        System.out.print(path.toString());
				        boolean occupiedPos = false;
				        if(yamlF.getConfigurationSection("pages") != null) {
				            for(int j = 1; j <= yamlF.getConfigurationSection("pages").getKeys(false).size(); j++) {
						        String spawnsec = "pages." + j + ".";
				            	int invalid  = 0;
				            	if(yamlF.getString(spawnsec + ".world").equals(loc.getWorld().getName())) {
				            		invalid ++;
				            	}
				            	if(yamlF.getDouble(spawnsec + ".xLoc") == (loc.getBlockX())) {
				            		invalid ++;
				            	}
				            	if(yamlF.getDouble(spawnsec + ".yLoc") == (loc.getBlockY())) {
				            		invalid ++;
				            	}
				            	if(yamlF.getDouble(spawnsec + ".zLoc") == (loc.getBlockZ())) {
				            		invalid ++;
				            	}
				            	if(invalid == 4) {
				            		occupiedPos = true;
				            		break;
				            	}
				            	i++;
				            }
				        }
				        if(!occupiedPos) {
				        	String spawnsec = "pages." + i + ".";
					        System.out.print("Current numer to write is " + i);
							yamlF.set(spawnsec + ".world", loc.getWorld().getName());
							yamlF.set(spawnsec + ".xLoc", loc.getBlockX());
							yamlF.set(spawnsec + ".yLoc", loc.getBlockY());
							yamlF.set(spawnsec + ".zLoc", loc.getBlockZ());
							saveConfig(yamlF); 
				        }else {
				        	p.sendMessage("There is already this set in config!");
				        }
					}
					else {
						p.sendMessage("There must be page inside the frame!");
					}
				}
				else
					p.sendMessage("no success with " + rts.getHitEntity().getName().toString());
				/*
				ItemFrame frame = (ItemFrame)rts.getHitEntity();
				p.sendMessage(rts.getHitEntity().getName());*/
			}
		}
	}
	private Location getEndLocConfig() {
		YamlConfiguration yamlF = YamlConfiguration.loadConfiguration(path);
		Location loc;
		if(yamlF.getConfigurationSection("endLocation") != null) {
			String spawnsec = "endLocation";
			World w = Bukkit.getWorld(yamlF.getString(spawnsec + ".world"));
			Double xl = yamlF.getDouble(spawnsec + ".xLoc");
			Double yl = yamlF.getDouble(spawnsec + ".yLoc");
			Double zl = yamlF.getDouble(spawnsec + ".zLoc");
			loc = new Location(w, xl, yl, zl);
			return loc;
		}
		return null;
	}	
	private Location getLobbyConfig() {
		YamlConfiguration yamlF = YamlConfiguration.loadConfiguration(path);
		Location loc;
		if(yamlF.getConfigurationSection("lobby") != null) {
			String spawnsec = "lobby";
			World w = Bukkit.getWorld(yamlF.getString(spawnsec + ".world"));
			Double xl = yamlF.getDouble(spawnsec + ".xLoc");
			Double yl = yamlF.getDouble(spawnsec + ".yLoc");
			Double zl = yamlF.getDouble(spawnsec + ".zLoc");
			loc = new Location(w, xl, yl, zl);
			return loc;
		}
		return null;
	}
	private List<Location> getPagesConfig() {
		List<Location> allpgs = new ArrayList<Location>();
		YamlConfiguration yamlF = YamlConfiguration.loadConfiguration(path);
		if(yamlF.getConfigurationSection("pages") != null) {
            for(int j = 1; j <= yamlF.getConfigurationSection("pages").getKeys(false).size(); j++) {
		        String spawnsec = "pages." + j + ".";
		        World w = Bukkit.getWorld(yamlF.getString(spawnsec + ".world")) ;
		        double xLocR = yamlF.getDouble(spawnsec + ".xLoc");
		        double yLocR = yamlF.getDouble(spawnsec + ".yLoc");
		        double zLocR = yamlF.getDouble(spawnsec + ".zLoc");
		        Location loc = new Location(w, xLocR, yLocR, zLocR);
		        allpgs.add(loc);
            }
    		return allpgs;
		}
		return null;
	}
	private List <Location> getSpawnsConfig() {
		YamlConfiguration yamlF = YamlConfiguration.loadConfiguration(path);
		int i = 1;
		List<Location> spaw = new ArrayList<Location>();
		if(yamlF.getConfigurationSection("spawns") != null) {
			for(String l : yamlF.getConfigurationSection("spawns").getKeys(false)) {
				World w = Bukkit.getWorld(yamlF.getString("spawns." + i + ".world"));
				double x = yamlF.getDouble(("spawns." + i + ".xLoc"));
				double y = yamlF.getDouble(("spawns." + i + ".yLoc"));
				double z = yamlF.getDouble(("spawns." + i + ".zLoc"));
				Location loc = new Location(w, x, y, z);
				spaw.add(loc);
				i++;
			}
		}else {
			return null;
		}
		if(spaw.size() < 1)
			return null;
		return spaw;
	}
	private int getMinPlayersConfig() {
		YamlConfiguration yamlF = YamlConfiguration.loadConfiguration(path);
		return yamlF.getInt("minPlayers");
	}
	public void regenerate() {
        if(getPages() != null) {
        	List<Location> tmpPgs = getPages();
            for(int j = 0; j < tmpPgs.size(); j++) {
		        Location loc = tmpPgs.get(j);
		        ItemFrame f;
		        boolean success = true;
		        try {
		        	boolean anew = true;
		        	List<Entity> nearbyFrames = new ArrayList<>(loc.getWorld().getNearbyEntities(loc, 1, 1, 1));
		        	for(Entity tmp : nearbyFrames) {
		        		anew = false;
		        		if(tmp instanceof ItemFrame) {
		        			ItemFrame tmpf = (ItemFrame)tmp;
		        			tmpf.setItem(new ItemStack(Material.PAPER));
		        		}
		        	}
		        	if(anew) {
			        	f = (ItemFrame)loc.getWorld().spawnEntity(loc, EntityType.ITEM_FRAME);
			        	f.setRotation(Rotation.NONE);
			        	f.setItem(new ItemStack(Material.PAPER));
		        	}
		        	
		        	
		        }catch(IllegalArgumentException e) {
		        	e.printStackTrace();
		        	success = false;
		        }
		        if(success) {
		        	System.out.print("Successfully regenerated!");
		        }else {
		        	System.out.print("Oh no! Somethig went wrong. Check terminal for errors.");
		        }
            }
        }
	}
	public String getName() {
		return name;
	}
	public List<Location> getSpawns() {
		return spawns;
	}
	public List<Location> getPages() {
		return pages;
	}
	public Location getEndLoc() {
		return endLoc;
	}
	public Location getLobby() {
		return lobbyLoc;
	}
	public int getMinPlayers() {
		return minPlayers;
	}
	
	private void saveConfig(YamlConfiguration yamlF) {
		try {
			yamlF.save(path);
			System.out.print("Saving done!");
		}catch(IOException e) {
			e.printStackTrace();
		}
	}
}