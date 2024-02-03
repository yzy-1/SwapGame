package leafor.swap.listeners;

import leafor.swap.Main;
import leafor.swap.config.Config;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;
import com.onarandombox.MultiverseCore.api.MultiverseWorld;
import javax.annotation.Nonnull;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public final class WaitListener extends GameListener {
  private final @Nonnull Set<Player> players = new HashSet<>(); // 游戏玩家
  private final @Nonnull Set<Player> spectators = new HashSet<>(); // 观战玩家
  private final @Nonnull MultiverseWorld gameWorld; // 游戏世界
  private final BossBar countdownBar;
  private GameStartCountdown countdown;

  public WaitListener(@Nonnull MultiverseWorld gameWorld) {
    Bukkit.getPluginManager().registerEvents(this, Main.GetInstance());
    this.gameWorld = gameWorld;
    countdownBar = Bukkit.createBossBar("", BarColor.RED, BarStyle.SOLID);
    ResetCountdownBar();
  }

  // 重置倒计时进度条
  private void ResetCountdownBar() {
    countdownBar.setTitle("Waiting for players");
    countdownBar.setProgress(1.0);
  }

  public void AddSpectator(@Nonnull Player p) {
    spectators.add(p);
    InitSpectator(p);
  }

  // 初始化游戏玩家
  public void InitPlayer(@Nonnull Player p) {
    p.setGameMode(GameMode.ADVENTURE); // 冒险模式下玩家无法破坏东西
    p.teleport(Config.lobby_world.getSpawnLocation());
    p.setFireTicks(0);
    p.setHealth(20);
    p.setHealthScaled(false);
    p.setFoodLevel(20);
    p.setSaturation(20);
    Objects.requireNonNull(p.getAttribute(Attribute.GENERIC_MAX_HEALTH)).setBaseValue(20);
    p.setHealth(20);
    p.setExp(0);
    p.setLevel(0);
    p.getActivePotionEffects().forEach(e -> p.removePotionEffect(e.getType()));
    p.getInventory().clear();
    countdownBar.addPlayer(p);
  }

  // 初始化观战玩家
  private void InitSpectator(@Nonnull Player p) {
    p.setGameMode(GameMode.SPECTATOR);
    p.getActivePotionEffects().forEach(e -> p.removePotionEffect(e.getType()));
    p.getInventory().clear();
    p.teleport(Config.lobby_world.getSpawnLocation());
    countdownBar.addPlayer(p);
  }

  // 开始游戏
  public void StartGame() {
    countdown.cancel();
    countdownBar.removeAll();
    System.out.println("[SwapGame] Game will start soon");
    HandlerList.unregisterAll(this);
    Bukkit.getScheduler().runTask(Main.GetInstance(),
        () -> GameListener.SetListener(new RunListener(players, spectators, gameWorld)));
  }

  // 添加玩家
  public void AddPlayer(@Nonnull Player p) {
    final var MAX_PLAYER = Config.game_player_max;
    final var MIN_PLAYER = Config.game_player_min;
    var name = p.displayName();
    players.add(p);
    InitPlayer(p);
    var size = players.size();
    players
        .forEach(x -> x.sendMessage(name.append(Component.text(ChatColor.AQUA + " joined the game")
            .append(Component.text(" %s[%d/%d]".formatted(ChatColor.YELLOW, size, MAX_PLAYER))))));
    spectators
        .forEach(x -> x.sendMessage(name.append(Component.text(ChatColor.AQUA + " joined the game")
            .append(Component.text(" %s[%d/%d]".formatted(ChatColor.YELLOW, size, MAX_PLAYER))))));
    if (size >= MIN_PLAYER && (countdown == null || countdown.isCancelled())) {
      countdown = new GameStartCountdown();
      countdown.runTaskTimer(Main.GetInstance(), 1, 20);
    }
    if (size == MAX_PLAYER) {
      assert countdown != null;
      countdown.seconds = Math.min(countdown.seconds, 5);
    }
  }

  @EventHandler
  public void OnPlayerQuit(PlayerQuitEvent e) {
    e.quitMessage(null);
    var p = e.getPlayer();
    final var MAX_PLAYER = Config.game_player_max;
    var name = p.displayName();
    if (players.contains(p)) {
      players.remove(p);
      var size = players.size();
      players.forEach(
          x -> x.sendMessage(name.append(Component.text(ChatColor.AQUA + " left the game").append(
              Component.text(" %s[%d/%d]".formatted(ChatColor.YELLOW, size, MAX_PLAYER))))));
      spectators.forEach(
          x -> x.sendMessage(name.append(Component.text(ChatColor.AQUA + " left the game").append(
              Component.text(" %s[%d/%d]".formatted(ChatColor.YELLOW, size, MAX_PLAYER))))));
      if (countdown != null && players.size() < Config.game_player_min) {
        countdown.cancel();
        countdown = null;
        ResetCountdownBar();
      }
    } else if (spectators.contains(p)) {
      spectators.remove(p);
    } else {
      throw new RuntimeException("bad case ...");
    }
  }

  @EventHandler
  public void OnPlayerJoin(PlayerJoinEvent e) {
    e.joinMessage(null);
    var p = e.getPlayer();
    if (players.size() < Config.game_player_max) {
      AddPlayer(Objects.requireNonNull(p));
    } else {
      p.sendMessage(ChatColor.RED + "Room is full, you are a spectator");
      AddSpectator(p);
    }
  }

  @EventHandler
  public void OnEntityDamage(EntityDamageEvent e) {
    var p = e.getEntity();
    if (p instanceof Player && players.contains(p)) {
      e.setCancelled(true);
    }
  }

  // 游戏开始倒计时
  public final class GameStartCountdown extends BukkitRunnable {
    private int seconds;

    GameStartCountdown() {
      seconds = Config.game_startCountdown;
    }

    @Override
    public void run() {
      --seconds;
      countdownBar.setTitle("Game will start in %ds".formatted(seconds));
      countdownBar.setProgress((double) seconds / Config.game_startCountdown);
      if (1 <= seconds && seconds <= 5) {
        players.forEach(x -> x.playSound(x.getLocation(), Sound.BLOCK_NOTE_BLOCK_HARP,
            SoundCategory.MASTER, 1, 1));
        spectators.forEach(x -> x.playSound(x.getLocation(), Sound.BLOCK_NOTE_BLOCK_HARP,
            SoundCategory.MASTER, 1, 1));
      }
      if (seconds <= 0) {
        StartGame();
      }
    }
  }
}
