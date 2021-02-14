package leafor.swap.controllers;

import leafor.swap.Main;
import leafor.swap.config.Config;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import javax.annotation.Nonnull;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public final class WaitController extends Controller {
  private final Set<Player> players = new HashSet<>(); // 游戏玩家
  private final Set<Player> spectators = new HashSet<>(); // 观战玩家
  private final World gameWorld; // 游戏世界
  private final BossBar countdownBar;
  private GameStartCountdown gameStartCountdown;

  public WaitController(@Nonnull World gameWorld) {
    this.gameWorld = gameWorld;
    countdownBar = Bukkit.createBossBar("", BarColor.RED, BarStyle.SOLID);
    ResetCountdownBar();
  }

  private void ResetCountdownBar() {
    countdownBar.setTitle("Waiting for players");
    countdownBar.setProgress(1.0);
  }

  public void AddSpectator(@Nonnull Player p) {
    spectators.add(p);
    InitSpectator(p);
  }

  // 初始化等待大厅中的玩家
  public void InitPlayer(@Nonnull Player p) {
    p.setGameMode(GameMode.ADVENTURE); // 冒险模式下玩家无法破坏东西
    p.teleport(Config.lobby_world.getSpawnLocation());
    p.setFireTicks(0);
    p.setHealth(20);
    p.setHealthScaled(false);
    p.setFoodLevel(20);
    p.setSaturation(20);
    Objects.requireNonNull(p.getAttribute(Attribute.GENERIC_MAX_HEALTH))
      .setBaseValue(20);
    p.setHealth(20);
    p.setExp(0);
    p.setLevel(0);
    p.getActivePotionEffects().forEach(e -> p.removePotionEffect(e.getType()));
    p.getInventory().clear();
    countdownBar.addPlayer(p);
  }

  private void InitSpectator(@Nonnull Player p) {
    p.setGameMode(GameMode.SPECTATOR);
    p.getActivePotionEffects().forEach(
      e -> p.removePotionEffect(e.getType()));
    p.getInventory().clear();
    p.teleport(Config.lobby_world.getSpawnLocation());
    countdownBar.addPlayer(p);
  }

  public void StartGame() {
    countdownBar.removeAll();
    System.out.println("[SwapGame] Game will start soon");
    Bukkit.getScheduler().runTask(Main.GetInstance(), () ->
      Controller.SetController(new RunController(
        players, spectators, gameWorld)));
  }

  public void AddPlayer(@Nonnull Player p) {
    final var MAX_PLAYER = Config.game_player_max;
    final var MIN_PLAYER = Config.game_player_min;
    var name = p.getDisplayName();
    if (players.size() >= MAX_PLAYER) {
      p.sendMessage(ChatColor.RED + "Room is full, you are a spectator");
      AddSpectator(p);
      return;
    }
    players.add(p);
    InitPlayer(p);
    var size = players.size();
    players.forEach(x -> x.sendMessage(
      "%s%s joined the game %s[%d/%d]".formatted(
        name, ChatColor.AQUA, ChatColor.YELLOW, size, MAX_PLAYER)));
    spectators.forEach(x -> x.sendMessage(
      "%s%s joined the game %s[%d/%d]".formatted(
        name, ChatColor.AQUA, ChatColor.YELLOW, size, MAX_PLAYER)));
    if (size >= MIN_PLAYER &&
      (gameStartCountdown == null || gameStartCountdown.isCancelled())) {
      gameStartCountdown = new GameStartCountdown(Config.game_startCountdown);
      gameStartCountdown.runTaskTimer(Main.GetInstance(), 1, 20);
    }
    if (size == MAX_PLAYER) {
      assert gameStartCountdown != null;
      gameStartCountdown.seconds = Math.min(gameStartCountdown.seconds, 5);
    }
  }

  public void QuitPlayer(@Nonnull Player p) {
    final var MAX_PLAYER = Config.game_player_max;
    var name = p.getDisplayName();
    if (players.contains(p)) {
      players.remove(p);
      var size = players.size();
      players.forEach(x -> x.sendMessage(
        "%s%s exited the game %s[%d/%d]".formatted(
          name, ChatColor.AQUA, ChatColor.YELLOW, size, MAX_PLAYER)));
      spectators.forEach(x -> x.sendMessage(
        "%s%s exited the game %s[%d/%d]".formatted(
          name, ChatColor.AQUA, ChatColor.YELLOW, size, MAX_PLAYER)));
      if (gameStartCountdown != null &&
        players.size() < Config.game_player_min) {
        gameStartCountdown.cancel();
        ResetCountdownBar();
      }
    } else if (spectators.contains(p)) {
      spectators.remove(p);
    } else {
      throw new RuntimeException("bad case ...");
    }
  }

  // 向所有人发送信息
  public void SendMessage(@Nonnull String message) {
    Config.lobby_world.getPlayers().forEach(p -> p.sendMessage(message));
  }

  // 游戏开始倒计时
  final class GameStartCountdown extends BukkitRunnable {
    private int seconds;

    GameStartCountdown(int seconds) {
      this.seconds = seconds;
    }

    @Override
    public void run() {
      --seconds;
      countdownBar.setTitle("Game will start in %ds".formatted(seconds));
      countdownBar.setProgress((double) seconds / Config.game_startCountdown);
      if (1 <= seconds && seconds <= 5) {
        players.forEach(
          x -> x.playSound(
            x.getLocation(),
            Sound.BLOCK_NOTE_BLOCK_HARP,
            SoundCategory.MASTER,
            1, 1));
        spectators.forEach(
          x -> x.playSound(
            x.getLocation(),
            Sound.BLOCK_NOTE_BLOCK_HARP,
            SoundCategory.MASTER,
            1, 1));
      }
      if (seconds <= 0) {
        cancel();
        StartGame();
      }
    }
  }
}
