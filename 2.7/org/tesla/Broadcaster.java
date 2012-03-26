package org.tesla;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;

import ru.tehkode.permissions.bukkit.PermissionsEx;
public class Broadcaster extends JavaPlugin {
	private static Broadcaster plugin;
	private final static Logger logger = Logger.getLogger("Minecraft");
	private static PluginDescriptionFile pluginDescription = null;
	private int currentLineRunning = 0, taskID = 0;
	private boolean isRunning = true;
	private long interval = 20;
	private static ArrayList<String> commands = new ArrayList<String>();
	private static File messages;
	private FileConfiguration config = null;
	private File configFile = null;
	private void loadConfig() {
		if(configFile == null)
			configFile = new File(getDataFolder(), "config.yml");
		if(!configFile.exists())
			loadDefaults();
		config = YamlConfiguration.loadConfiguration(configFile);
	}
	
	private void loadDefaults() {
		config = new YamlConfiguration();
		config.set("interval", 30);
		config.set("file", "messages.txt");
		config.set("running", true);
		config.set("line", 0);
		config.set("restartOnCommand", true);
		config.set("prefix", "&r[Broadcast]");
		config.set("warning", "&4You don't have permission to do that.");
		try {
			config.save(configFile);
		} catch (Exception e) {}
	}
	
	private void saveConfiguration() {
		if(config == null || configFile == null)
			return;
		try {
			config.save(configFile);
		} catch (Exception e) {
			logSevere("Could not save config.yml file.");
		}
	}
	
	private void logSevere(String message) {
		Logger.getLogger(org.bukkit.plugin.java.JavaPlugin.class.getName()).log(Level.SEVERE, message);
	}
	
	private void logWarning(String message) {
		Logger.getLogger(org.bukkit.plugin.java.JavaPlugin.class.getName()).log(Level.WARNING, message);
	}
	
	static {
		commands.add("stop");
		commands.add("start");
		commands.add("interval");
		commands.add("message");
		commands.add("restart");
		commands.add("send");
		commands.add("add");
		commands.add("reload");
		commands.add("remove");
	}
	
	private Runnable runner = new Runnable() {
		public void run() {
			try {
				broadcastNextMessage(messages.getPath());
				Thread.sleep(interval * 20);
			} catch (IOException e) {
				plugin.getDataFolder().mkdir();
				File file = messages;
				if(!file.exists()) {
					try {
						file.createNewFile();
						FileWriter writer = new FileWriter(file);
						writer.write("&cThis is sample text, you should change this in " + file.getPath() + ".");
						writer.close();
					} catch (IOException e1) {
						logSevere("[Broadcaster] Couldn't create '" + file.getName() + "' file");
					}
				}
				if(!file.exists()) {
					Bukkit.getPluginManager().disablePlugin(plugin);
				}
			} catch (InterruptedException e) {
				logSevere("[Broadcaster] Broadcaster was interrupted.");
			}
		}
	};
	
	public void onEnable() {
		init();
		logger.info(pluginDescription.getName() + " version " + pluginDescription.getVersion() + " is now enabled.");
	}
	
	private void init() {
		plugin = this;
		pluginDescription = this.getDescription();
		loadConfig();
		messages = new File(plugin.getDataFolder(), config.getString("file"));
		interval = config.getInt("interval", 20);
		currentLineRunning = config.getInt("line");
		isRunning = config.getBoolean("running");
		if(isRunning)
			taskID = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, runner, 0, interval * 20);
	}
	
	private String replaceColors(String input) {
		String output = "";
		output = input.replaceAll("&f", ChatColor.WHITE + "");
		output = output.replaceAll("&e", ChatColor.YELLOW + "");
		output = output.replaceAll("&d", ChatColor.LIGHT_PURPLE + "");
		output = output.replaceAll("&a", ChatColor.GREEN + "");
		output = output.replaceAll("&c", ChatColor.RED + "");
		output = output.replaceAll("&4", ChatColor.DARK_RED + "");
		output = output.replaceAll("&6", ChatColor.GOLD + "");
		output = output.replaceAll("&2", ChatColor.DARK_GREEN + "");
		output = output.replaceAll("&b", ChatColor.AQUA + "");
		output = output.replaceAll("&3", ChatColor.DARK_AQUA + "");
		output = output.replaceAll("&1", ChatColor.DARK_BLUE + "");
		output = output.replaceAll("&9", ChatColor.BLUE + "");
		output = output.replaceAll("&5", ChatColor.DARK_PURPLE + "");
		output = output.replaceAll("&7", ChatColor.GRAY + "");
		output = output.replaceAll("&8", ChatColor.DARK_GRAY + "");
		output = output.replaceAll("&0", ChatColor.BLACK + "");
		return output;
	}
	
	private void broadcastNextMessage(String fileName) throws IOException {
		FileInputStream fs;
		fs = new FileInputStream(fileName);
		BufferedReader br = new BufferedReader(new InputStreamReader(fs));
		for(int i = 0; i < currentLineRunning; i++)
			br.readLine();
		String currentLineString = replaceColors(br.readLine());
		Player[] players = Bukkit.getServer().getOnlinePlayers();
		String broadcastPrefix = config.getString("prefix", replaceColors("&4[Broadcast]"));
		for(Player p : players) {
			p.sendMessage(broadcastPrefix + " " + ChatColor.WHITE + currentLineString);
		}
		logger.info("[Broadcast] " + currentLineString);
		LineNumberReader lineNumberReader = new LineNumberReader(new FileReader(new File(fileName)));
		lineNumberReader.skip(Long.MAX_VALUE);
		int lastLine = lineNumberReader.getLineNumber();
		if(currentLineRunning == lastLine)
			currentLineRunning = 0;
		else
			currentLineRunning++;
	}
	
	private static int getMessageLine(String fileName, String lineText) throws IOException {
		FileInputStream fs = new FileInputStream(fileName);
		BufferedReader br = new BufferedReader(new InputStreamReader(fs));
		int cLine = 0;
		boolean found = false;
		String line = "";
		LineNumberReader lnr = new LineNumberReader(new FileReader(new File(fileName)));
		lnr.skip(Long.MAX_VALUE);
		int lastLine = lnr.getLineNumber();
		while(br.ready() && (cLine <= lastLine) && !found) {
			line = br.readLine();
			if(line.contains(lineText)) {
				found = true;
				break;
			}
			cLine++;
		}
		return found ? cLine : -1;
	}
	
	public void restartBroadcasts(int timeout) {
		if(isRunning) {
			Bukkit.getScheduler().cancelTask(taskID);
			taskID = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, runner, timeout, interval * 20);
		}
		else {
			taskID = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, runner, timeout, interval * 20);
			isRunning = true;
		}
	}
	
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
		if(sender instanceof Player || sender instanceof ConsoleCommandSender)
			readCommand(sender, commandLabel, args);
		else
			sender.sendMessage("Your not a player, your not the console, so what are you?");
		return false;
	}
	
	public void readCommand(CommandSender sender, String command, String[] args) {
		if(sender instanceof Player) {
			Player player = (Player) sender;
			if(Bukkit.getServer().getPluginManager().isPluginEnabled("PermissionsEx")) {
				if(!PermissionsEx.getPermissionManager().has(player, "broadcaster.admin")) {
					player.sendMessage(replaceColors(config.getString("warning", "&4You don't have permission to do that.")));
					logWarning("[" + player.getDisplayName() + "] tried to access the /broadcaster command, with no permissions");
					return;
				}
			}
			else if(!player.hasPermission("broadcaster.admin")) {
				player.sendMessage(replaceColors(config.getString("warning", "&4You don't have permission to do that.")));
				logWarning("[" + player.getDisplayName() + "] tried to access the /broadcaster command, with no permissions");
				return;
			}
		}
		if(command.equalsIgnoreCase("broadcaster")) {
			if(args.length > 0) {
				String arg1 = args[0];
				if(arg1.equals("stop"))
					execStopCommand(sender);
				else if(arg1.equals("start"))
					execStartCommand(sender);
				else if(arg1.equals("interval"))
					if(execIntervalCommand(sender, args));
					else
						sendCommandError(sender, command + " " + arg1, "Expected number, got something else.");
				else if(arg1.equals("message")) {
					if(execMessageCommand(sender, args));
					else
						sendCommandError(sender, command + " " + arg1, "Couldn't find message.");
				}
				else if (arg1.equals("restart")) {
					execRestartCommand(sender);
				}
				else if (arg1.equals("send")) {
					if(execSendCommand(args));
					else
						sendCommandError(sender, command + " " + arg1, null);
				}
				else if (arg1.equals("add")) {
					if(execAddCommand(sender, args))
						sender.sendMessage("Message added to messages");
					else
						sendCommandError(sender, command + " " + arg1, "Not able to add text to messages.");
				}
				else if (arg1.equals("reload")) {
					sender.sendMessage("Broadcaster reloading...");
					execReloadCommand();
					sender.sendMessage("Broadcaster reloaded");
				}
				else if (arg1.equals("remove")) {
					if(execRemoveCommand(sender, args))
						sender.sendMessage("Removed line from messages.");
					else
						sendCommandError(sender, command + " " + arg1, "Not able to remove text from messages.");
				}
				else
					sendCommandError(sender, command + " " + arg1, "not a vaild command.");
			} else
				sendCommandError(sender, command, null);
		}
		else
			sendCommandError(sender, command, "Not implemented by " + pluginDescription.getName() + " version " + pluginDescription.getVersion());
	}
	
	private static String assembleString(String[] args, int start) {
		StringBuilder builder = new StringBuilder();
		for(int i = start; i < args.length; i++) {
			builder.append(args[i]);
			if(i != (args.length - 1))
				builder.append(' ');
		}
		return builder.toString();
	}
	
	private static boolean execSendCommand(String[] args) {
		if(args.length > 1) {
			StringBuilder builder = new StringBuilder();
			for(int i = 1; i < args.length; i++) {
				builder.append(args[i]);
				if(i != (args.length - 1))
					builder.append(' ');
			}
			String arg2 = builder.toString();
			Player[] players = Bukkit.getServer().getOnlinePlayers();
			for(Player p : players) {
				p.sendMessage(arg2);
			}
			return true;
		}
		else
			return false;
	}
	
	private void execRestartCommand(CommandSender sender) {
		restartBroadcasts((int)(interval * 20));
		sender.sendMessage("Broadcasts have been started.");
		logger.info("[Broadcaster] Broadcasts have been started by [" + ((sender instanceof Player) ? ((Player)sender).getDisplayName() : "CONSOLE")  + "].");
		isRunning = true;
		config.set("running", true);
	}
	
	private boolean execMessageCommand(CommandSender sender, String[] args) {
		if(args.length > 1) {
			String arg2 = assembleString(args, 1);
			int index = -1;
			try {
				index = getMessageLine(messages.getPath(), arg2);
			} catch (IOException e) {
				logger.info("[Broadcaster] Error finding line index of message: [" + arg2 + "] from user [" + ((sender instanceof Player) ? ((Player)sender).getDisplayName() : "CONSOLE") + "]");
				return false;
			}
			if(index == -1) {
				sender.sendMessage("Couldn't find message.");
				return false;
			}
			else {
				currentLineRunning = index;
				sender.sendMessage("Message found.");
				logger.info("[Broadcaster] Current line changed by [" + ((sender instanceof Player) ? ((Player)sender).getDisplayName() : "CONSOLE") + "]");
				config.set("line", currentLineRunning);
				return true;
			}
		}
		else
			return false;
	}
	
	private boolean execIntervalCommand(CommandSender sender, String[] args) {
		if(args.length > 1) {
			String arg2 = args[1];
			long i;
			try {
				i = Long.parseLong(arg2);
				
			} catch (NumberFormatException pe) {
				return false;
			}
			interval = i;
			if(config.getBoolean("restartOnCommand", false)) {
				restartBroadcasts((int)(interval * 20));
			}
			sender.sendMessage("Interval set to " + interval + " seconds");
			logger.info("[Broadcaster] Broadcast interval changed by [" + ((sender instanceof Player) ? ((Player)sender).getDisplayName() : "CONSOLE") + "].");
			config.set("interval", interval);
			return true;
		}
		else
			return false;
	}
	
	private void execStartCommand(CommandSender sender) {
		if(isRunning)
			sender.sendMessage("Broadcasts already on.");
		else {
			restartBroadcasts((int)(interval * 20));
			sender.sendMessage("Broadcasts have been started.");
			logger.info("[Broadcaster] Broadcasts have been started by [" + ((sender instanceof Player) ? ((Player)sender).getDisplayName() : "CONSOLE") + "].");
			isRunning = true;
			config.set("running", true);
		}
	}
	
	private void execStopCommand(CommandSender sender) {
		if(isRunning) {
			Bukkit.getServer().getScheduler().cancelTask(taskID);
			sender.sendMessage("Broadcasts have been stopped.");
			logger.info("[Broadcaster] Broadcasts have been stopped by [" + ((sender instanceof Player) ? ((Player)sender).getDisplayName() : "CONSOLE") + "].");
			isRunning = false;
			config.set("running", false);
		} else
			sender.sendMessage("Broadcasts already off.");
	}
	
	private static boolean execAddCommand(CommandSender sender, String[] args) {
		if(args.length > 2) {
			String arg2 = args[1];
			int i = -1;
			try {
				i = Integer.parseInt(arg2);
				
			} catch (NumberFormatException pe) {
				return false;
			}
			if(i >= 0) {
				if(args.length >= 3) {
					String text = assembleString(args, 2);
					return addText(messages.getPath(), i, text);
				}
				else
					return false;
			}
			else
				return false;
		}
		else
			return false;
	}
	private static boolean addText(String fileName, int lineNumber, String text) {
		String contents = "";
		try {
			BufferedReader reader = new BufferedReader(new FileReader(new File(fileName)));
			while(reader.ready()) {
				String line = reader.readLine();
				contents += line + "\n";
			}
			contents = contents.substring(0, contents.length() - 1);
			String[] lines = contents.split(Pattern.quote("\n"));
			if(lineNumber > lines.length)
				lineNumber = lines.length;
			else if(lineNumber < 0)
				lineNumber = 0;
			FileWriter writer = new FileWriter(new File(fileName));
			if(lineNumber == 0) {
				writer.write(text);
				for(int i = 0; i < lines.length; i++) {
					writer.write("\n" + lines[i]);
				}
				writer.close();
			}
			else {
				for(int i = 0; i < lineNumber; i++)
					writer.write(lines[i] + "\n");
				writer.write(text);
				if(lineNumber == lines.length)
					writer.close();
				else {
					for(int i = lineNumber; i < lines.length; i++)
						writer.write("\n" + lines[i]);
					writer.close();
				}
			}
		} catch(Exception e) {
			return false;
		}
		return true;
	}
	
	private void execReloadCommand() {
		saveConfiguration();
		loadConfig();
		if(config.getBoolean("restartOnCommand", false)) {
			restartBroadcasts((int)(interval * 20));
		}
	}
	
	private boolean execRemoveCommand(CommandSender sender, String[] args) {
		if(args.length >= 2) {
			String arg2 = args[1];
			int i = -1;
			try {
				i = Integer.parseInt(arg2);
				
			} catch (NumberFormatException pe) {
				String message = assembleString(args, 1);
				try {
					i = getMessageLine(messages.getPath(), message);
				} catch (Exception e) {
					return false;
				}
			}
			if(i >= 0) {
				return removeText(messages.getPath(), i);
			}
			else {
				return false;
			}
		}
		else {
			return false;
		}
	}
	private boolean removeText(String fileName, int lineNumber) {
		String contents = "";
		try {
			BufferedReader reader = new BufferedReader(new FileReader(new File(fileName)));
			while(reader.ready()) {
				String line = reader.readLine();
				contents += line + "\n";
			}
			contents = contents.substring(0, contents.length() - 1);
			String[] lines = contents.split(Pattern.quote("\n"));
			FileWriter writer = new FileWriter(new File(fileName));
			StringBuilder builder = new StringBuilder();
			for(int i = 0; i < lineNumber; i++) {
				builder.append(lines[i]);
				if(i < (lineNumber - 1))
					builder.append('\n');
			}
			for(int i = lineNumber + 1; i < lines.length; i++) {
				builder.append("\n" + lines[i]);
			}
			String builderString = builder.toString().trim();
			writer.write(builderString);
			writer.close();
		} catch(Exception e) {
			return false;
		}
		return true;
	}

	private static void sendCommandError(CommandSender sender, String command, String reason) {
		if(reason == null) {
			sender.sendMessage(ChatColor.RED + "Invalid command: /" + command + ", no arguments given");
		}
		else {
			sender.sendMessage(ChatColor.RED + "Invalid command: /" + command + ", " + reason);
		}
	}
	
	public void onDisable() {
		saveConfiguration();
		logger.info(pluginDescription.getName() + " version " + pluginDescription.getVersion() + " is now disabled.");
	}
}