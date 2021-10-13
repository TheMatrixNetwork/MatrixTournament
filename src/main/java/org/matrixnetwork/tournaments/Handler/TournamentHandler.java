package org.matrixnetwork.tournaments.Handler;

import com.google.gson.Gson;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.math.BlockVector3;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.matrixnetwork.tournaments.Data.Arena;
import org.matrixnetwork.tournaments.Data.PlayerData;
import org.matrixnetwork.tournaments.Data.SaveArena;
import org.matrixnetwork.tournaments.Data.Tournament;
import org.matrixnetwork.tournaments.Listener.TournamentListener;
import org.matrixnetwork.tournaments.MatrixTournaments;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

public class TournamentHandler {
    private static File player_data;
    private static File arena_data;
    private static File extra_data;

    private static List<Arena> arenas;
    private static TreeSet<PlayerData> playerDatas;
    private static List<Tournament> tournaments;

    private static Location tournamentLobby;

    private static HashMap<Player, Arena> arenasInCreation;

    public static HashMap<Player, Tournament> playersInGame;

    /**
     * Loads player data to memory if player has no data, empty playerdata is loaded
     */
    public static PlayerData loadPlayerData(Player player) {
        for (PlayerData actdata : playerDatas)
            if (actdata.getPlayer().equals(player))
                return actdata;

        File actFile = new File(player_data, player.getUniqueId().toString() + ".json");

        if (!actFile.exists())
            return new PlayerData(player, 0, 0, 0, 0);

        try {
            Scanner sc = new Scanner(actFile);
            sc.useDelimiter("\\Z");
            PlayerData data = new Gson().fromJson(sc.next(), PlayerData.class);
            sc.close();
            return data;
        } catch (Exception ex) {
        }

        return new PlayerData(player, 0, 0, 0, 0);
    }

    public static List<Arena> getArenas() {
        return arenas;
    }

    public static boolean existsTournament(String t) {
        return getTournament(t) != null;
    }

    public static List<PlayerData> getPlayerDatas() {
        return new ArrayList<>(playerDatas);
    }

    public static PlayerData getPlayerData(Player p) {
        for (PlayerData data : playerDatas) {
            if (data.getPlayer().equals(p)) {
                return data;
            }
        }

        return loadPlayerData(p);
    }

    public static void createTournament(String name, List<Arena> arenas) {
        for (Arena a : arenas) {
            a.reserve();
        }
        tournaments.add(new Tournament(name, arenas));
    }

    public static boolean saveTournamentLobby(Location lobby) {
        try {
            Files.write(extra_data.toPath(), (new Gson()).toJson(lobby.serialize()).getBytes());
            tournamentLobby = lobby;
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Save player data to disk
     * 
     * @param data
     */
    public static void savePlayerData(PlayerData data) {
        String output = new Gson().toJson(data);
        File actFile = new File(player_data, data.getPlayer().getUniqueId().toString() + ".json");

        try {
            if (!actFile.exists())
                actFile.createNewFile();

            Files.write(actFile.toPath(), output.getBytes());
        } catch (Exception ex) {
            MatrixTournaments.getPlugin().getLogger()
                    .info("Could not save tournament Player data of " + data.getPlayer().getName());
        }
    }

    /**
     * Loads arenas from disk
     */
    public static void loadArenas() {
        for (File f : arena_data.listFiles()) {
            if (!f.isDirectory()) {
                try {
                    Scanner sc = new Scanner(f);
                    sc.useDelimiter("\\Z");
                    arenas.add(Arena.fromSaveArena(new Gson().fromJson(sc.next(), SaveArena.class)));
                    sc.close();
                } catch (Exception ex) {
                    MatrixTournaments.getPlugin().getLogger().info("Could not load " + f.getName());
                }
            }
        }
    }

    /**
     * Saves arena to disk
     * 
     * @param toSave
     */
    public static boolean saveArena(Arena toSave, Player saver) {
        if (toSave.getSpawnPoint1() == null || toSave.getSpawnPoint2() == null || toSave.getSpectatorPoint() == null) {
            return false;
        }

        String output = new Gson().toJson(Arena.toSaveArena(toSave));
        File actFile = new File(arena_data, toSave.getName() + ".json");

        try {
            if (!actFile.exists())
                actFile.createNewFile();

            Files.write(actFile.toPath(), output.getBytes());
            arenas.add(toSave);
            arenasInCreation.remove(saver);
            return true;
        } catch (Exception ex) {
            MatrixTournaments.getPlugin().getLogger().info("Could not save arena " + toSave.getName());
            return false;
        }
    }

    /**
     * Called when the plugin is loaded
     */
    public static void init(MatrixTournaments plugin) throws Exception {
        plugin.getCommand("tournament").setExecutor(new TournamentCommandHandler());

        player_data = new File(MatrixTournaments.getPlugin().getDataFolder(), "tournaments");

        if(!MatrixTournaments.getPlugin().getDataFolder().exists())
            MatrixTournaments.getPlugin().getDataFolder().mkdir();

        if (!player_data.exists())
            player_data.mkdir();

        extra_data = new File(player_data, "extras.yml");

        if(!extra_data.exists()) {
            try {
                extra_data.createNewFile();
                tournamentLobby = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else {
            try {
                tournamentLobby = Location.deserialize(new Gson().fromJson(Files.readAllLines(extra_data.toPath()).get(0), Map.class));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        arena_data = new File(player_data, "arena_data");
        player_data = new File(player_data, "player_data");

        if (!player_data.exists())
            if(!player_data.mkdir())
                throw new Exception("Cannot create player data folder!");

        if (!arena_data.exists())
            if(!arena_data.mkdir())
                throw new Exception("Cannot create arena data folder!");

        arenas = new ArrayList<>();
        tournaments = new ArrayList<>();
        playerDatas = new TreeSet<>();
        playersInGame = new HashMap<>();
        arenasInCreation = new HashMap<>();



        plugin.getServer().getPluginManager().registerEvents(new TournamentListener(), plugin);
        loadArenas();
    }

    public static Arena getArenaOfPlayerInCreation(Player p) {
        return arenasInCreation.containsKey(p) ? arenasInCreation.get(p) : null;
    }

    public static void createArenaInCreation(Player p, Arena a) {
        arenasInCreation.put(p, a);
    }

    public static List<Tournament> getTournaments() {
        return tournaments;
    }

    public static void removeTournament(Tournament toStop) {
        tournaments.remove(toStop);
    }

    public static Tournament getTournament(String arg) {
        for (Tournament t : tournaments) {
            if (t.getName().equalsIgnoreCase(arg))
                return t;
        }

        return null;
    }

    public static void pasteArena(Arena a) {
        File f = new File(MatrixTournaments.getPlugin().getDataFolder().getAbsolutePath() + "/../WorldEdit/schematics/" + a.getSchemName() + ".schematic");

        try {
            ClipboardFormats.findByFile(f).load(f).paste(BukkitAdapter.adapt(a.getSchemPasteLoc().getWorld()),
                    BlockVector3.at(a.getSchemPasteLoc().getX(), a.getSchemPasteLoc().getY(),
                    a.getSchemPasteLoc().getZ()),
                    false,
                    true,
                    false,
                    null);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * When a participator does /tt leave
     * @param participator
     */
    public static void removeParticipatorFromTournament(Player participator) {
        PlayerData pd = TournamentHandler.loadPlayerData(participator);

        if(getTournamentOfPlayer(participator) != null) {
            getTournamentOfPlayer(participator).remvoveParticipator(pd);
            pd.leftTournament();
        }

        if(playersInGame.containsKey(participator)) {
            playersInGame.get(participator).finishRound(participator);
        }
    }

    public static Arena getArena(String arg) {
        for(Arena ar : arenas) {
            if(ar.getName().equals(arg))
                return ar;
        }

        return null;
    }

    public static Tournament getTournamentOfPlayer(Player sender) {
        for (Tournament t : tournaments) {
            if(t.hasParticipator(sender))
                return t;
        }

        return null;
    }

    public static Arena getArenaOfPlayer(Player player) {
        return getTournamentOfPlayer(player).getRoundOfPlayer(player).getArena();
    }

    public static void teleportPlayerWithMsg(Player player, Location teleportTo, String msg) {
        BukkitTask runnable = new BukkitRunnable() {
            private int seconds = 3;

            @Override
            public void run() {
                if(seconds == 3)
                    player.sendMessage(msg);
                if(seconds <= 0) {
                    player.teleport(teleportTo);
                    MatrixTournaments.getPlugin().getServer().getScheduler().cancelTask(this.getTaskId());
                }

                if(seconds <= 0)
                    player.sendMessage(ChatColor.YELLOW + "Teleporting. . .");
                else
                    player.sendMessage(ChatColor.YELLOW + "Teleporting in " + seconds-- + ". . .");
            }

        }.runTaskTimer(MatrixTournaments.getPlugin(), 0, 20);
    }

    public static Location getTournamentLobby() {
        return tournamentLobby;
    }
}
