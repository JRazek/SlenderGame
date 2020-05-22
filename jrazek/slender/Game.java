package jrazek.slender;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.util.Vector;

import me.libraryaddict.disguise.disguisetypes.DisguiseType;
import me.libraryaddict.disguise.disguisetypes.MobDisguise;

public class Game implements Listener{
	
	private Main main;

	private int databaseGameID;
	
	private List<Player> players = new ArrayList<>();
	private Config config;
	private Player slender;
	private Arena arena;
	private List<Player> deadPlayers = new ArrayList<>();
	
	private boolean running = false;
	
	private MobDisguise endermanDisguise = new MobDisguise(DisguiseType.ENDERMAN);
	
	private BukkitTask checks;
	private BukkitTask lighterChecking;
	private BukkitTask gameTimer;
	private BukkitTask lobbyCountdownTimer;
    BossBar bossBar;

	private ScoreboardManager scm = Bukkit.getScoreboardManager();
	
	private Map <Player, List<ItemFrame>> pagesCollected = new HashMap<>();
	private int pagesCollectedInt = 0;
	
	private boolean isSlenderHidden = false;
	private boolean allowChangeToSlender = true;
	
	private int gameTimeLeft;
	private int lobbyCountdownLeft;
	
	Map <Player, Lighter> lighters = new HashMap<>();
	
	final class Lighter{
		private boolean disable = false;
		private boolean status = false;
		private float change = (float) 0.2;
		private float charge = (float)1;
		public void use(Player p) {
			if(charge >= 0.1) {
				charge = charge - change;
			}else {
				toggle(p);
				disable = true;
			}
		}
		public void rest(Player p) {
			if(charge <= 0.9) {
				charge = charge + change;
			}else {
				disable = false;
			}
		}
		public void toggle(Player p) {
			if(!disable) {
				status = !status; 	 
				p.playSound(p.getLocation(), Sound.BLOCK_DISPENSER_DISPENSE, 1, 1);
				if(status) {
					p.removePotionEffect(PotionEffectType.BLINDNESS);
				}else {
					p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, Integer.MAX_VALUE, 1));
				}
			}
		}
		public float getCharge() {
			return charge;
		}
		public boolean getStatus() {
			return status;
		}
	}
	
	public Game(Arena a, Config c, Main m) {
		arena = a;
		config = c;
		main = m;
		gameTimeLeft = config.getGameTime();
		lobbyCountdownLeft = config.getLobbyCountDown();
		bossBar = Bukkit.createBossBar("Game starts in...", BarColor.BLUE, BarStyle.SOLID);
	}
	public void reloadScoreboard(Player p, int timeLeft) {
		Scoreboard scb = scm.getNewScoreboard();
		Objective objective = scb.registerNewObjective("scoreboard", "s", ChatColor.BLACK + "Slender " + ChatColor.DARK_AQUA + "mc.koxicraft.com");
		objective.setDisplaySlot(DisplaySlot.SIDEBAR);
		Score pages;
		Score timer;
		pages = objective.getScore(ChatColor.WHITE + "Pages " + ChatColor.GREEN + pagesCollectedInt + "/" + arena.getPages().size());
		String minutes;
		String seconds;
		if(Integer.toString(timeLeft/60).length() == 1) {
			minutes = "0" + Integer.toString(timeLeft/60);
		}else {
			minutes = Integer.toString(timeLeft/60);
		}
		
		if(Integer.toString(timeLeft%60).length() == 1) {
			seconds = "0" + Integer.toString(timeLeft%60);
		}else {
			seconds = Integer.toString(timeLeft%60);
		}
		
		timer = objective.getScore("Time Left " + ChatColor.AQUA + minutes + ":" + seconds);
		pages.setScore(1);
		timer.setScore(2);
		p.setScoreboard(scb);
		//if slender on boss bar show the left visibility time. Remove delay and make the visibility renewing like mana
	}
	
	public void join(Player p) {
		if(!running) {
			if(!players.contains(p)) {
				players.add(p);
				p.teleport(arena.getLobby());
				bossBar.addPlayer(p);
				//p.sendMessage("You have joined the arena");
				broadcastMessage("§4[SLENDER] §6" + p.getName() + " §ajoined the game");
				giveEq(p, "lobby");
			}
			else {
				p.sendMessage("You are already in this game!");
			}
			if(players.size() >= arena.getMinPlayers()) {
				lobbyCountdown();
			}
		}
		else {
			p.sendMessage("Arena is currently running");
		}
	}
	public void leave(Player p) {
		players.remove(p);
		p.teleport(arena.getEndLoc());
		onLeaveFix(p);
		if(!running) {
			if(players.size() < arena.getMinPlayers()) {
				try {
					lobbyCountdownTimer.cancel();
					bossBar.setProgress(1);
					lobbyCountdownLeft = config.getLobbyCountDown();
				}catch(NullPointerException e) {
					
				}
			}
		}
		broadcastMessage("§4[SLENDER] §6" + p.getName() + " §cleft the game");
		if(running && (players.size() < 2 || p.equals(slender))) {
			end();
		}
	}
	public void show(Player p) {
		for(int i = 0; i < players.size(); i++) {
			p.sendMessage(players.get(i).getName());
		}
	}
	private void onLeaveFix(Player p) {
		removePotions(p);
		p.setHealth(20);
		p.setFoodLevel(20);
		bossBar.removePlayer(p);
		p.teleport(arena.getEndLoc());
		p.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
		p.getInventory().clear();
		if(lighters.get(p) != null) {
			lighters.remove(p);
		}
	}
	public void start() {
		//getting slender player
		if(!running) {
			if(players.size() > 1) {
				databaseGameID = main.sql.newGame(this);
				if(databaseGameID == -1) {
					end();
				}
				running = true;
				arena.regenerate();
				Random rand = new Random();
				int n = rand.nextInt(players.size());
				slender = players.get(n);
				endermanDisguise.setEntity(slender);
				endermanDisguise.startDisguise();
				gameTimer();
				lighterChecking();
				broadcastMessage("§aPlayers §fhave to collect all pages until the §4Slender kills them");
				for(Player p : players) {
					bossBar.addPlayer(p);
					removePotions(p);
					p.setHealth(20);
					p.setFoodLevel(20);
					if(p != slender) {
						lighters.put(p, new Lighter());
						p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, Integer.MAX_VALUE, 1));
						giveEq(p, "player");
						
					}else {
						giveEq(p, "slender");
					}
					randomSpawn(p);
				}
				slenderHideToggle();
				checking();
			}
			else {
				System.out.println("You need more players to start!");
			}
		}
	}
	public void test(Player p) {
		if(running) {
			for(Player pl : deadPlayers) {
				System.out.println(pl.getName());
			}
		}
	}
	private void giveEq(Player p, String type) {
		Inventory inv = p.getInventory();
		ItemStack leaveItem = new ItemStack(config.getLeaveItem());
		ItemMeta leaveItemMeta = leaveItem.getItemMeta();
		leaveItemMeta.setDisplayName("§cLobby");
		leaveItem.setItemMeta(leaveItemMeta);
		inv.clear();
		
		if(type.equals("lobby")) {
			p.getInventory().setItem(8, leaveItem);
		}
		else if(type.equals("player")) {
			ItemStack light = new ItemStack(config.getLighterItem());
			ItemMeta lightMeta = light.getItemMeta();
			lightMeta.setDisplayName("§eLighter");
			light.setItemMeta(lightMeta);
			p.getInventory().setItem(0, light);
		//	p.getInventory().setItem(0, leaveItem);
		}
		else if(type.contentEquals("slender")) {
			ItemStack slToggIt = new ItemStack(config.getSlenderHideItem());
			ItemMeta slToggMeta = slToggIt.getItemMeta();
			slToggMeta.setDisplayName("§1Hide Toggler");
			slToggIt.setItemMeta(slToggMeta);
			p.getInventory().setItem(0, slToggIt);
			//p.getInventory().setItem(8, leaveItem);
		}
	}
	public void end() {
		if(running) {
			for(int i = 0; i < players.size(); i++) {
				Player p = players.get(i);
				onLeaveFix(p);
				displayStats(p);
				System.out.println(p.getName());
			}
			endermanDisguise.removeDisguise();
		//	endermanDisguise = new MobDisguise(DisguiseType.ENDERMAN);
			//running  = false;
			gameTimer.cancel();
			checks.cancel();
			lighterChecking.cancel();
			arena.regenerate();
			players.clear();
			bossBar.removeAll();
			System.out.println("Game ended");
			main.killGame(this);
		}else {
			for(Player p : players) {
				onLeaveFix(p);
			}
		}
	}
	public boolean isPlayerInGame(Player p) {
		for(int i = 0; i < players.size(); i++) {
			if(p == players.get(i))
				return true;
		}
		return false;
	}
	public void randomSpawn(Player p) {
		if(arena.getSpawns() != null) {
			List<Location> spawns = arena.getSpawns();
			Random rand = new Random();
			int n = rand.nextInt(spawns.size());
			p.teleport(spawns.get(n));
			System.out.print("im here with " + p.getName());
		}else {
			System.out.print("You need at least 2 spawns to start!");
		}
	}
	public void gameEndNormal(int winner) {

		main.sql.endGame(this, winner);
		System.out.println("Checkpoint2");
		if(winner == 0) {
			broadcastTitle("§4Slender won", "");
		}else if(winner == 1) {
			broadcastTitle("§aPlayers won", "");
		}else if(winner == 2) {
			broadcastTitle("§9Draw", "");
		}
		end();
		//scoreboard and shit
	}
    public void onPageTake(Player p, ItemFrame frame, EntityDamageByEntityEvent e) {
    	//Map <Player, List<ItemFrame>> pagesCollected;
    	if(p == slender) {
    		e.setCancelled(true);
    	}else {
			frame.setItem(new ItemStack(Material.AIR));
	    	if(pagesCollected.get(p) == null) {
	    		List<ItemFrame> pGs= new ArrayList<ItemFrame>();
	    		pGs.add(frame);
	    		pagesCollected.put(p, pGs);
	    	}else {
	    		List<ItemFrame> pGsG = pagesCollected.get(p);
	    		pGsG.add(frame);
	    		pagesCollected.put(p, pGsG);
	    	}
	    	frame.remove();
	    	pagesCollectedInt++;
	    	if(pagesCollectedInt == arena.getPages().size()) {
	    		gameEndNormal(1);
	    	}else {
	    		broadCastPageCollected();
	    	}
    	}
    }
	public void slenderHideToggle() {
		if(allowChangeToSlender) {
			if(slender.hasPotionEffect(PotionEffectType.INVISIBILITY)) {
				slender.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, Integer.MAX_VALUE, 2));
				slender.removePotionEffect(PotionEffectType.INVISIBILITY);
				isSlenderHidden = false;
			}else {
				slender.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 1));
				slender.removePotionEffect(PotionEffectType.SLOW);
				isSlenderHidden = true;
				allowChangeToSlender = false;
				slenderSound(slender);
				delaySlender();
			}
		}
	}
	private void delaySlender() {
        Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(main, new Runnable(){
            @Override
            public void run(){
            	allowChangeToSlender = true;
            }
        }, 60);
	}
	
	private void removePotions(Player p) {
		for (PotionEffect effect : p.getActivePotionEffects())
			p.removePotionEffect(effect.getType());
		p.setGameMode(GameMode.ADVENTURE);
	}
	private boolean seeSlender(Player p) {
		if(p.getGameMode().equals(GameMode.ADVENTURE)) {
			if(!isSlenderHidden) {
				if(p.getLocation().distance(slender.getLocation())
						<= config.getSlenderCaptureDistance()) {
					if(p.hasLineOfSight(slender)) {
						return true;
					}
				}else {
					//Vector v = new Vector(0, 1.5, 0);
					Vector dirToDestination = slender.getEyeLocation().toVector().subtract(p.getEyeLocation().toVector());
					Vector playerDirection = p.getEyeLocation().getDirection().normalize();
					double angle = dirToDestination.angle(playerDirection);
					if(config.getFovAngle() >= angle*57) {
						if(p.hasLineOfSight(slender)) {
							return true;
						}
					}
				}
			}
		}
		return false;
	}
	private void checking() {
		checks = new BukkitRunnable() {
        	@Override
        	public void run() {
        		//get nearby entities from slender
        		for(Entity ent : slender.getNearbyEntities(15, 15, 15)) {
        			if(ent instanceof Player) {
        				Player p = (Player)ent;
            			if(p != slender) {
            				if(seeSlender(p)) {
                				p.damage(config.getDamageRate()*(pagesCollectedInt + 1));
                				p.sendTitle(ChatColor.RED + "RUN", "", 5, 20, 5);
                				slenderSound(p);
            				}
            				Random rand = new Random();
            				int n = rand.nextInt(99);
            				if(n >= 0 && n < config.getRandSoundChance()) {
            					randomSound(p);
            				}
            			}
        			}
        		}
        	}
        }.runTaskTimer(this.main, 0, 4);
	}

	private void displayStats(Player p) {
		p.sendMessage(ChatColor.BLACK + "Slender Statistics");
		p.sendMessage(ChatColor.DARK_RED + "[" + slender.getName() + "]" + " " + deadPlayers.size() +  " kills");
		for(Player tp : players) {
			if(tp != slender) {
				String nick = ChatColor.GREEN + tp.getName();
				if(deadPlayers.contains(tp)) {
					nick = ChatColor.GRAY + tp.getName();
				}
				int pgsCollected = 0;
				if(pagesCollected.get(tp) != null) {
					pgsCollected = pagesCollected.get(tp).size();
					System.out.println("Pages collected");
				}
				p.sendMessage("[" + nick + ChatColor.GRAY + "]" + " " + pgsCollected + " pages");
			}
		}
	}
	public void setSpecOnDeath(Player p) {
		removePotions(p);
		deadPlayers.add(p);
		p.setGameMode(GameMode.SPECTATOR);
		//checks if all players (except slender) are dead
		if(deadPlayers.size() == players.size()-1) {
			//System.out.println("Checkpoint1");
    		gameEndNormal(0);
		}else {
			//System.out.println(deadPlayers.size() + " =/= " + players.size());
		}
	}
	public void slenderSound(Player p) {
		Random rand = new Random();
		int n = rand.nextInt(config.getSlenerSounds().size());
		p.playSound(p.getLocation(), config.getSlenerSounds().get(n), 1, 2); 
	}
	public void randomSound(Player p) {
		Random rand = new Random();
		int n = rand.nextInt(config.getRandomSounds().size());
		p.playSound(p.getLocation(), config.getRandomSounds().get(n), 1, 2); 
		//System.out.println("im here nibba2");
	}
	public Player getSlender() {
		return slender;
	}
	private void broadCastPageCollected() {
		for(Player p : players) {
			p.sendTitle(ChatColor.GREEN + "PAGE TAKEN", pagesCollectedInt + "/" + arena.getPages().size(), 10, 40, 10);
			p.playSound(p.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1, 1); 
		}
	}
	private void lighterChecking() {
		lighterChecking = new BukkitRunnable() {
        	@Override
        	public void run() {
        		for (Map.Entry<Player, Lighter> entry : lighters.entrySet()) {
        		   // System.out.println(entry.getKey() + "/" + entry.getValue());
        		    if(entry.getValue().getStatus() == true) {
        		    	entry.getValue().use(entry.getKey());
        		    }else {
        		    	entry.getValue().rest(entry.getKey());
        		    }
    		    	entry.getKey().setExp(entry.getValue().getCharge());
        		}
        	}
        }.runTaskTimer(this.main, 0, 20);
	}
	private void gameTimer() {
		bossBar.setVisible(false);
		bossBar = null;
		bossBar = Bukkit.createBossBar("Slender mc.koxicraft.com", BarColor.BLUE, BarStyle.SOLID);
		bossBar.setProgress(1);
		gameTimer = new BukkitRunnable() {
        	@Override
        	public void run() {
        		if(gameTimeLeft <= 0){
        			gameEndNormal(2);
        		}else {
            		for(Player p : players) {
                		reloadScoreboard(p, gameTimeLeft);
            		}
        		}
        		System.out.println((float)gameTimeLeft/(float)config.getGameTime());
        		bossBar.setProgress((float)gameTimeLeft/(float)config.getGameTime());
        		gameTimeLeft = gameTimeLeft - 1;
        	}
        }.runTaskTimer(this.main, 0, 20);
	}
	private void lobbyCountdown() {
		bossBar.setProgress(1);
		lobbyCountdownTimer = new BukkitRunnable() {
        	@Override
        	public void run() {
        		if(lobbyCountdownLeft <= 0){
        			start();
        			this.cancel();
        		}else {
        			if(lobbyCountdownLeft < 5) {
	            		for(Player p : players) {
	            			p.sendTitle(ChatColor.GREEN + Integer.toString(lobbyCountdownLeft), "", 10, 40, 10);
	            		}
        			}
        		}
        		bossBar.setProgress((float)((float)lobbyCountdownLeft/(float)config.getLobbyCountDown()));
        		lobbyCountdownLeft = lobbyCountdownLeft - 1;
        	}
        }.runTaskTimer(this.main, 0, 20);
	}
	
	public void broadcastTitle(String title, String undertext) {
		for(Player p : players) {
			p.sendTitle(title, undertext, 10, 40, 10);
		}
	}
	public void broadcastMessage(String title) {
		for(Player p : players) {
			p.sendMessage(title);
		}
	}
	public List<Player> getPlayers(){
		return players;
	}
	public boolean getStatus() {
		return running;
	}
	public Arena getArena() {
		return arena;
	}
	public int getPagesCollectedInt() {
		return pagesCollectedInt;
	} 
	public Map <Player, List<ItemFrame>> getPagesCollected(){
		return pagesCollected;
	}
	public int getDatabaseID() {
		return databaseGameID;
	}
}
