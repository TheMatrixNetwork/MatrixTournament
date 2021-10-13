package org.matrixnetwork.tournaments;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.java.annotation.command.Command;
import org.bukkit.plugin.java.annotation.command.Commands;
import org.bukkit.plugin.java.annotation.plugin.ApiVersion;
import org.bukkit.plugin.java.annotation.plugin.ApiVersion.Target;
import org.bukkit.plugin.java.annotation.plugin.Description;
import org.bukkit.plugin.java.annotation.plugin.Plugin;
import org.bukkit.plugin.java.annotation.plugin.author.Author;
import org.matrixnetwork.tournaments.Handler.TournamentHandler;

@Plugin(name = "MatrixTournaments", version = "0.0.1")
@Description(value = "This Plugin handles the tournament system on Matrix")
@Author(value = "S1mple133")
@Commands( @Command(name = "tournament", desc = "Basic usage"))
@ApiVersion(Target.v1_13)
public class MatrixTournaments extends JavaPlugin{
	
	private static MatrixTournaments plugin;
	
	@Override
	public void onLoad() {
		// initialize the plugin
		plugin = this;
		
		getLogger().info("Loaded");
	}
	
	@Override
	public void onEnable() {
		try {
			TournamentHandler.init(this);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void onDisable() {
		
		
		getLogger().info("Disabled");
	}
	
	public static org.bukkit.plugin.Plugin getPlugin() {
		return plugin;
	}
}