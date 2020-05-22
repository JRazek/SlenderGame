package jrazek.slender;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import net.md_5.bungee.api.ChatColor;


public class Main extends JavaPlugin implements Listener{

	Config config;
	List <Game> games = new ArrayList<>();
	List <Arena> arenas = new ArrayList<>();
	Mysql sql;
	public void onEnable() {
		config = new Config(this);
		loadArenas();
	    getServer().getPluginManager().registerEvents(this, this);
	    sql = new Mysql(config.getConnInfo().host, config.getConnInfo().dbname, config.getConnInfo().user,
	    		config.getConnInfo().password, config.getConnInfo().port);
	}
	public void onDisable() {
		for(int i = 0; i < games.size(); i++) {
			games.get(i).end();
		}
	}
	public void loadArenas() {
		arenas = new ArrayList<>();
	    for(File f : config.getArenaFiles()) {
	        if(f != null) {
	        	Arena a = new Arena(f, config, this);
	        	arenas.add(a);
	        }
	    }
	}
    @EventHandler (priority = EventPriority.HIGH)
    public void onClick(PlayerInteractEvent e) {
    	Player p = e.getPlayer();
    	if(e.getAction() == Action.RIGHT_CLICK_BLOCK || e.getAction() == Action.RIGHT_CLICK_AIR) {
    		if(e.getItem() != null) {
	    		if(e.getItem().getType().equals(config.getJoinItem())) {
	    			p.sendMessage(e.getItem().getType().toString());
		    		//Joining GUI or Random arena
	    		}
	    		else if(e.getItem().getType().equals(config.getSlenderHideItem())) {
	    			for(Game g : games) {
	    				if(g.isPlayerInGame(p)) {
	    					if(p == g.getSlender()) {
		    					g.slenderHideToggle();
		    					break;
	    					}
	    				}
	    			}
	    		}
	    		else if(e.getItem().getType().equals(config.getLighterItem())) {
	    			if(e.getItem().getItemMeta().getDisplayName().equals("§eLighter")) {
		    			for(Game g : games) {
		    				if(g.isPlayerInGame(p)) {
		    					if(p != g.getSlender()) {
		    						g.lighters.get(p).toggle(p);
		    						break;
		    					}
		    				}
		    			}
	    			}
	    		}
    		}
    	}
    	if(e.getAction() == Action.RIGHT_CLICK_BLOCK || e.getAction() == Action.RIGHT_CLICK_AIR) {
    		if(e.getItem() != null) {
	    		if(e.getItem().getType().equals(config.getLeaveItem())) {
	    			if(e.getItem().getItemMeta().getDisplayName().equals("§cLobby")) {
	    		    	leaveGame(e.getPlayer());
	    			}
	    		}
    		}
    	}
    }
    @EventHandler (priority = EventPriority.HIGH)
    public void foodChangeLevel(FoodLevelChangeEvent e) {
    	if(e.getEntity() instanceof Player) {
	    	Player p = (Player)e.getEntity();
	    	for(Game g : games) {
	    		if(g.isPlayerInGame(p)) {
	    			p.setFoodLevel(20);
	    			e.setCancelled(true); 
	        		break;
	    		}
	    	}
    	}
    }
    @EventHandler (priority = EventPriority.NORMAL)
    public void healthSpectatorSet(EntityDamageEvent e) {
    	if(e.getEntity() instanceof Player) {
	    	Player p = (Player)e.getEntity();
	    	for(Game g : games) {
	    		if(g.isPlayerInGame(p)) {
	    			if(p.getHealth() <= config.getDamageRate()*(g.getPagesCollectedInt() + 1 )) {
	    				g.setSpecOnDeath(p);
	    				e.setCancelled(true);
	    			}
	        		break;
	    		}
	    	}
    	}
    }
    @EventHandler (priority = EventPriority.HIGH)
    public void denyBreak(BlockBreakEvent e) {
    	Player p = e.getPlayer();
    	for(Game g : games) {
    		if(g.isPlayerInGame(p)) {
    			e.setCancelled(true);
        		break;
    		}
    	}
    }
    @EventHandler (priority = EventPriority.HIGH)
    public void denyPlace(BlockPlaceEvent e) {
    	Player p = e.getPlayer();
    	for(Game g : games) {
    		if(g.isPlayerInGame(p)) {
    			e.setCancelled(true);
        		break;
    		}
    	}
    }
    @EventHandler (priority = EventPriority.HIGH)
    public void denyHealthRestoring(EntityRegainHealthEvent e) {
    	if(e.getEntity() instanceof Player) {
	    	Player p = (Player)e.getEntity();
	    	for(Game g : games) {
	    		if(g.isPlayerInGame(p)) {
	    			e.setCancelled(true);
	        		break;
	    		}
	    	}
    	}
    }
    @EventHandler (priority = EventPriority.HIGH)
    public void commandsBlock(PlayerCommandPreprocessEvent e) {
    	Player p = e.getPlayer();
    	for(Game g : games) {
    		if(g.isPlayerInGame(p)) {
    			String str = e.getMessage();
    			str = str.replace("/", "");
    			if(!config.getAllowedCommands().contains(str)) {
        			e.setCancelled(true);
        			p.sendMessage(ChatColor.RED + "You cannot use that command durig the game!");
    			}
        		break;
    		}
    	}
    }
    @EventHandler (priority = EventPriority.HIGH)
    public void DenyDropItems(PlayerDropItemEvent e){
    	if(e.getItemDrop().getItemStack().getType().equals(Material.CLOCK)) {
    		e.setCancelled(true);
    		e.getPlayer().sendMessage("You cant drop it!");
    	}
    }
    @EventHandler
    public void PlayerRightClick(EntityDamageByEntityEvent e) {
    	if(e.getDamager() instanceof Player) {
			Player p = (Player) e.getDamager();
			for(Game g : games) {
				if(g.isPlayerInGame(p)) {
			    	if(e.getEntityType().equals(EntityType.ITEM_FRAME)) {
			    		ItemFrame f = (ItemFrame)e.getEntity();
						g.onPageTake(p, f, e);
			    	}
			    	else {
			    		e.setCancelled(true);
			    	}
					break;
				}
			}
        	//e.getDamager().sendMessage(e.getEntity().getName());
    	}
    	//System.out.print(e.getDamager().toString());
    }
    @EventHandler (priority = EventPriority.HIGH)
    public void onLeave(PlayerQuitEvent e) {
    	leaveGame(e.getPlayer());
    }
    
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

		if(cmd.getName().equalsIgnoreCase("slender")) {
				Player p = (Player) sender;
				if(args.length > 0) {
					if(args[0].equalsIgnoreCase("join")) {
						if(args.length == 2) {
							//check wether theres any game on the list with that arena already
							boolean found = false;
						    for(Game g : games) {
						        if(g != null && g.getArena().getName().equals(args[1])) {
						        	g.join(p);
						        	found = true;
						        	
						        	break;
						        }
						    }
						    if(!found) {
								leaveGame(p);
						    	//check wether arena exist 
						    	boolean exist = false;
						    	for(int i = 0; i < arenas.size(); i++) {
						        	System.out.print(arenas.get(i).getName());
						    		if(arenas.get(i).getName().equals(args[1])) {
						    			Game g = new Game(arenas.get(i), config, this);
						    			games.add(g);
						    			games.get(games.size()-1).join(p);
						    			exist = true;
							    	//	p.sendMessage("You have joined " + games.get(games.size()-1).getArena().getName());
						    		}
						    	}
						    	if(!exist) {
						    		p.sendMessage("Theres no such arena as " + args[1]);
						    	}
						    }
						}
					}
					else if(args[0].equalsIgnoreCase("leave")){
						leaveGame(p);
					}
					else if(args[0].equalsIgnoreCase("show")) {
						
					}
					else if(args[0].equalsIgnoreCase("start")) {
						if(args.length == 2) {
							for(int i = 0; i < games.size(); i++) {
								if(games.get(i).getArena().getName().equals(args[1])) {
									games.get(i).start();
								}
							}
						}
					}
					else if(args[0].equalsIgnoreCase("end")) {
						if(args.length == 2) {
							for(int i = 0; i < games.size(); i++) {
								if(games.get(i).getArena().getName().equals(args[1])) {
									games.get(i).end();
								}
							}
						}
					}
					else if(args[0].equalsIgnoreCase("test")) {
						/*for(int i = 0; i < arenas.size(); i ++) {
							System.out.print(arenas.get(i).getName());
						}
						for(int i = 0; i < games.size(); i ++) {
							if(games.get(i).isPlayerInGame(p)) {
								games.get(i).test(p);
							}
						}*/
					}
					else if(args[0].equalsIgnoreCase("create") && args.length == 2) {
						if(p.hasPermission("slender.admin")) {
							config.createArena(args[1]);
						}else {
							p.sendMessage("You have no permission to do that!");
						}
					}
					else if(p.hasPermission("slender.admin")) {
						if(args[0].equalsIgnoreCase("reload")) {
							config = new Config(this);
							loadArenas();
							p.sendMessage("Reloading the plugin...");
						}
						if(args.length == 2) {
							if(args[0].equalsIgnoreCase("setspawn")) {
								for (Arena arena : arenas) {
									if(arena.getName().equals(args[1])) {
										arena.setSpawn(p.getLocation());
										p.sendMessage("Setting spawn location");
										break;
									}
								}
							}
							else if(args[0].equalsIgnoreCase("create")) {
								config.createArena(args[1]);
							}
							else if(args[0].equalsIgnoreCase("setpage")) {
								for (Arena arena : arenas) {
									if(arena.getName().equals(args[1])) {
										arena.setPage(p);
										break;
									}
								}
							}
							else if(args[0].equalsIgnoreCase("setlobby")) {
								for (Arena arena : arenas) {
									if(arena.getName().equals(args[1])) {
										arena.setLobby(p.getLocation());
										p.sendMessage("Setting lobby location");
										break;
									}
								}
							}
							else if(args[0].equalsIgnoreCase("setendloc")) {
								for (Arena arena : arenas) {
									if(arena.getName().equals(args[1])) {
										arena.setEndLoc(p.getLocation());
										p.sendMessage("Setting end location");
										break;
									}
								}
							}
							else if(args[0].equalsIgnoreCase("regenerate")) {
								for (Arena arena : arenas) {
									if(arena.getName().equals(args[1])) {
										arena.regenerate();
										break;
									}
								}
							}
							else {
								p.sendMessage("Bad usage! " + args[0]);
							}
						}
					}
					else {
						p.sendMessage("Bad usage! np" + args[0]);
					}
				}
		}
		return true;
	}
	public void leaveGame(Player p) {
		for(int i = 0; i < games.size(); i ++) {
			if(games.get(i).isPlayerInGame(p)) {
				games.get(i).leave(p);
				p.sendMessage("You left the game");
			}
		}
	}
	public void killGame(Game g) {
		games.remove(g);
		g = null;
	}
	public double getVectorAngle(Vector v1, Vector v2) {
		return 0;
	}
}






