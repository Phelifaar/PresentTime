package ezo.phelifar.presenttime;

import java.io.File;
import java.sql.*;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Date;

import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class main extends JavaPlugin implements Listener, CommandExecutor
{
    
    BukkitRunnable client = new BukkitRunnable() {
        public void run() {
            Date date = new Date();
            
            long tick = date.getHours() * 1000 + 18000 + date.getMinutes() * 1000 / 60 + tickUTC;
            if (tick >= 24000L) {
                tick -= 24000L;
            }
        	if(player != null) {
        		if(getConfig().getList("enable_world").contains(player.getWorld().getName())) player.setPlayerTime(tick, true);
        	}
        }
    };
    
    long tickUTC = 0;
    
    private Connection connection;
    private String host, database, username, password;
    private int port;

	private Player player;
    
    public void onEnable() {
    	bStats stats = new bStats(this, 7115);
        Bukkit.getPluginManager().registerEvents(this, this);
        getCommand("mytime").setExecutor(this);
        getLogger().info("PresentTime build 7 by Phelifar");
        File config = new File(getDataFolder() + File.separator + "config.yml");
        if (!config.exists()) {
            getConfig().options().copyDefaults(true);
            saveDefaultConfig();
        }

        host = getConfig().getString("database.host");
        database = getConfig().getString("database.database");
        username = getConfig().getString("database.username");
        password = getConfig().getString("database.password");
        port = getConfig().getInt("database.port");
        
        client.runTaskTimer(this, 0L, 20L);        
    }
    
    public void openConnection() throws SQLException, ClassNotFoundException {
        if (connection != null && !connection.isClosed()) {
            return;
        }
     
        synchronized (this) {
            if (connection != null && !connection.isClosed()) {
                return;
            }
            Class.forName("com.mysql.jdbc.Driver");
            connection = DriverManager.getConnection("jdbc:mysql://" + this.host+ ":" + this.port + "/" + this.database, this.username, this.password);
        }
    }
    
    @EventHandler
    public void JoinPlayer(PlayerJoinEvent event) {
    	player = event.getPlayer();
    	try {    
            openConnection();
            Statement statement = connection.createStatement();
        	ResultSet hasPlayer = statement.executeQuery("SELECT COUNT(NICK) as count FROM `playerutc` WHERE NICK = '" + event.getPlayer().getName() + "';");
        	if (hasPlayer.next()) {
            	ResultSet getUTC = statement.executeQuery("SELECT * FROM `playerutc` WHERE NICK = '" + event.getPlayer().getName() + "';");
            	if (getUTC.next()) {
            	    tickUTC = getUTC.getInt("UTC");
            	}else statement.executeUpdate("INSERT INTO `playerutc` (NICK, UTC) VALUES ('" + player.getName() + "', '0');");
        	}

        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    	Date timeServer = new Date();
    	if(args.length < 2) return false;
    	if(sender instanceof Player) player = (Player) sender;
    	tickUTC = ((Long.parseLong(args[0]) - timeServer.getHours()) * 1000) + ((Long.parseLong(args[1]) - timeServer.getMinutes()) * 1000 / 60);

        if (tickUTC >= 24000L) {
        	tickUTC -= 24000L;
        }
        if (tickUTC <= 0L) {
        	tickUTC += 24000L;
        }
    	try {    
            openConnection();
            Statement statement = connection.createStatement();          
            statement.executeUpdate("UPDATE `playerutc` SET UTC='" + tickUTC + "'WHERE NICK= '" + player.getName() + "';");
    	} catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    	return true;
    }
}