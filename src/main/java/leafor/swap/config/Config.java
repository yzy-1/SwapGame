package leafor.swap.config;

import com.xxmicloxx.NoteBlockAPI.model.Song;
import com.xxmicloxx.NoteBlockAPI.utils.NBSDecoder;
import leafor.swap.Main;
import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.World;

import java.util.Objects;

public final class Config {
  public static boolean bungee_enabled;
  public static String bungee_lobbyServer;
  public static World lobby_world;
  public static String game_world;
  public static int game_startCountdown;
  public static int game_player_min;
  public static int game_player_max;
  public static int game_player_health;
  public static int game_area_radius;
  public static long game_area_time;
  public static int game_protectTime;
  public static int game_swap_time_min;
  public static int game_swap_time_max;
  public static Song game_swap_song;

  private Config() {}

  // 加载配置文件
  public static void Load() {
    var cfg = Main.GetInstance().getConfig();
    bungee_enabled = cfg.getBoolean("bungee.enabled");
    bungee_lobbyServer =
      Objects.requireNonNull(cfg.getString("bungee.lobbyServer"));
    var lobbyWorldName =
      Objects.requireNonNull(cfg.getString("lobby.world"));
    lobby_world = Bukkit.getWorld(lobbyWorldName);
    if (lobby_world == null) {
      throw new RuntimeException(
        "Invalid lobby world %s".formatted(lobbyWorldName));
    }
    lobby_world.setPVP(false);
    lobby_world.setDifficulty(Difficulty.PEACEFUL);
    game_world = Objects.requireNonNull(cfg.getString("game.world"));
    game_startCountdown = cfg.getInt("game.startCountdown");
    game_player_min = cfg.getInt("game.player.min");
    game_player_max = cfg.getInt("game.player.max");
    game_player_health = cfg.getInt("game.player.health");
    game_area_radius = cfg.getInt("game.area.radius");
    game_area_time = cfg.getLong("game.area.time");
    game_protectTime = cfg.getInt("game.protectTime");
    game_swap_time_min = cfg.getInt("game.swap.time.min");
    if (game_swap_time_min <= 10) {
      throw new RuntimeException("game.swap.time.min must be greater than 10");
    }
    game_swap_time_max = cfg.getInt("game.swap.time.max");
    if (game_swap_time_max <= game_swap_time_min) {
      throw new RuntimeException(
        "game.swap.time.max must be greater than game.swap.time.min");
    }
    var swapSongName = cfg.getString("game.swap.song");
    if (swapSongName != null && !swapSongName.isEmpty()) {
      game_swap_song =
        NBSDecoder.parse(Main.GetInstance().getDataFolder().toPath()
                           .resolve(swapSongName).toFile());
    }
  }

  public static void GenerateDefault() {
    var cfg = Main.GetInstance().getConfig();
    cfg.options().copyDefaults(true);
    cfg.addDefault("bungee.enabled", false);
    cfg.addDefault("bungee.lobbyServer", "swap-hub");
    cfg.addDefault("lobby.world", "lobby");
    cfg.addDefault("game.world", "game");
    cfg.addDefault("game.startCountdown", 30);
    cfg.addDefault("game.player.min", 2);
    cfg.addDefault("game.player.max", 16);
    cfg.addDefault("game.player.health", 40);
    cfg.addDefault("game.area.radius", 200);
    cfg.addDefault("game.area.time", 11000L);
    cfg.addDefault("game.protectTime", 60);
    cfg.addDefault("game.swap.time.min", 30);
    cfg.addDefault("game.swap.time.max", 180);
    cfg.addDefault("game.swap.song", "");
    Main.GetInstance().saveConfig();
  }

  public static void Reload() {
    Main.GetInstance().reloadConfig();
    Load();
  }
}