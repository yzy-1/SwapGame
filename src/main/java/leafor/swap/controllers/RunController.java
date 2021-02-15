package leafor.swap.controllers;

import com.xxmicloxx.NoteBlockAPI.songplayer.RadioSongPlayer;
import leafor.swap.Main;
import leafor.swap.config.Config;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.stream.Collectors;

public final class RunController extends Controller {
  private final Set<Player> players; // 游戏玩家
  private final Set<Player> spectators; // 观战玩家
  private final World gameWorld; // 游戏世界
  private final SwapCountdown countdown = new SwapCountdown();
  private boolean isGameEnd = false;
  private boolean isProtectionPeriod = true;
  private BossBar countdownBar;

  public RunController(
    @Nonnull Set<Player> players,
    @Nonnull Set<Player> spectators,
    @Nonnull World gameWorld) {
    this.players = players;
    this.spectators = spectators;
    this.gameWorld = gameWorld;
    gameWorld.setFullTime(10000); // 设置时间为傍晚
    countdownBar = Bukkit.createBossBar(
      "Protection period", BarColor.RED, BarStyle.SOLID);
    countdownBar.setProgress(1.0);
    final var MAX_HEALTH = Config.game_player_health;
    // 初始化每个玩家
    players.forEach(p -> {
      p.teleport(gameWorld.getSpawnLocation());
      p.setGameMode(GameMode.SURVIVAL);
      p.setFireTicks(0);
      p.setFoodLevel(20);
      p.setSaturation(20);
      Objects.requireNonNull(p.getAttribute(Attribute.GENERIC_MAX_HEALTH))
        .setBaseValue(MAX_HEALTH);
      p.setHealthScaled(false);
      p.setHealth(MAX_HEALTH);
      p.setExp(0);
      p.setLevel(0);
      p.getActivePotionEffects().forEach(
        e -> p.removePotionEffect(e.getType()));
      p.addPotionEffect(new PotionEffect(
        PotionEffectType.FAST_DIGGING, Integer.MAX_VALUE, 1));
      var inv = p.getInventory();
      inv.clear();
      inv.setChestplate(new ItemStack(Material.IRON_CHESTPLATE));
      p.sendTitle(ChatColor.RED + "Game start", "",
                  10, 70, 20);
      p.playSound(
        p.getLocation(),
        Sound.ENTITY_ENDER_DRAGON_GROWL,
        SoundCategory.MASTER,
        1F,
        1F);
      countdownBar.addPlayer(p);
    });
    // 将观战玩家 tp 到游戏场地
    spectators.forEach(p -> {
      p.teleport(gameWorld.getSpawnLocation());
      p.getActivePotionEffects().forEach(
        e -> p.removePotionEffect(e.getType()));
      p.getInventory().clear();
      p.sendTitle(ChatColor.RED + "Game start", "",
                  10, 70, 20);
      p.playSound(
        p.getLocation(),
        Sound.ENTITY_ENDER_DRAGON_GROWL,
        SoundCategory.MASTER,
        1F,
        1F);
      countdownBar.addPlayer(p);
    });
    countdown.runTaskTimer(Main.GetInstance(), 0, 20);
  }

  // 向所有人发送信息
  private void SendMessage(@Nonnull String message) {
    gameWorld.getPlayers().forEach(p -> p.sendMessage(message));
  }

  // 结束游戏
  private void EndGame() {
    players.forEach(x -> x.playSound(
      x.getLocation(),
      Sound.ENTITY_WITHER_DEATH,
      SoundCategory.MASTER,
      1F,
      1F));
    spectators.forEach(x -> x.playSound(
      x.getLocation(),
      Sound.ENTITY_WITHER_DEATH,
      SoundCategory.MASTER,
      1F,
      1F));
    if (!isGameEnd) {
      isGameEnd = true;
      countdown.cancel();
      countdownBar.removeAll();
      countdownBar = null;
      System.out.println("[SwapGame] Game over");
      Bukkit.getScheduler().runTaskLater(
        Main.GetInstance(),
        () -> Controller.SetController(
          new CleanController(gameWorld)), 10 * 20);
    }
  }

  private void CheckWinner() {
    assert players.size() <= 1;
    if (players.size() == 0) {
      System.out.println("Oops! We can not decide who the winner is");
      SendMessage(ChatColor.RED + "Oops! We can not decide who the winner is");
    } else {
      var winner = (Player) players.toArray()[0];
      players.forEach(p -> {
        if (players.contains(p)) {
          p.sendTitle(
            ChatColor.RED + "Congratulations!",
            "You won the game",
            10, 70, 20);
        } else {
          p.sendTitle(
            ChatColor.RED + "Game over!",
            "%s is the winner".formatted(winner.getDisplayName()),
            10, 70, 20);
        }
      });
    }
    EndGame();
  }

  private void RemovePlayer(@Nonnull Player p) {
    assert players.contains(p);
    players.remove(p);
    if (countdownBar != null) {
      countdownBar.removePlayer(p);
    }
    var size = players.size();
    SendMessage("%s%s%d players left".formatted(
      ChatColor.RED, ChatColor.BOLD, size));
    if (players.size() <= 1) {
      CheckWinner();
    }
  }

  // 淘汰玩家
  public void EliminatePlayer(@Nonnull Player p) {
    assert players.contains(p);
    gameWorld.strikeLightningEffect(p.getLocation());
    p.sendTitle(
      ChatColor.RED + "You lost the game!",
      "Now you are a spectator",
      10, 70, 20);
    var loc = p.getLocation();
    p.getInventory().forEach(x -> {
      if (x != null) {
        gameWorld.dropItemNaturally(loc, x);
      }
    });
    p.getInventory().clear();
    RemovePlayer(p);
    AddSpectator(p); // 加入观战列表
  }

  // 当玩家退出时
  public void QuitPlayer(@Nonnull Player p) {
    if (isGameEnd) {
      return;
    }
    var name = p.getDisplayName();
    if (players.contains(p)) {
      EliminatePlayer(p);
      SendMessage(
        "%s%s exited the game".formatted(name, ChatColor.AQUA));
    } else if (spectators.contains(p)) {
      spectators.remove(p);
    } else {
      throw new RuntimeException("bad case ...");
    }
  }

  // 添加观战
  public void AddSpectator(@Nonnull Player p) {
    spectators.add(p);
    p.setGameMode(GameMode.SPECTATOR);
    if (!p.getWorld().equals(gameWorld)) {
      p.teleport(gameWorld.getSpawnLocation());
    }
    if (countdownBar != null) {
      countdownBar.addPlayer(p);
    }
  }

  // 交换倒计时
  final class SwapCountdown extends BukkitRunnable {
    private final Random random;
    private int seconds, maxSeconds;

    SwapCountdown() {
      random = new Random();
      seconds = maxSeconds = Config.game_protectTime;
    }

    private void RandomSeconds() {
      var minTime = Config.game_swap_time_min;
      var maxTime = Config.game_swap_time_max;
      seconds = maxSeconds = random.nextInt(maxTime - minTime) + minTime;
    }

    @Override
    public void run() {
      --seconds;
      if (countdownBar != null) {
        countdownBar.setTitle(
          (isProtectionPeriod ? "PVP will be available in %ds" :
            "Will swap in %ds").formatted(seconds));
        countdownBar.setProgress((double) seconds / maxSeconds);
      }
      final var SWAP_SONG = Config.game_swap_song;
      final var SONG_TIME =
        Math.round((float) Config.game_swap_song.getLength() /
                     Config.game_swap_song.getSpeed());
      if (seconds == SONG_TIME && !isProtectionPeriod) {
        RadioSongPlayer rsp = new RadioSongPlayer(SWAP_SONG);
        players.forEach(rsp::addPlayer);
        spectators.forEach(rsp::addPlayer);
        rsp.setPlaying(true);
      }
      if (seconds <= 0) {
        RandomSeconds();
        if (isProtectionPeriod) {
          isProtectionPeriod = false;
          gameWorld.setPVP(true);
        } else {
          var ps = new ArrayList<>(players);
          Collections.shuffle(ps, random);
          var locations =
            ps.stream().map(Entity::getLocation).collect(Collectors.toList());
          for (int i = 0; i < ps.size(); i++) {
            var p = ps.get(i);
            var fromLoc = p.getLocation();
            var toLoc = locations.get(i == ps.size() - 1 ? 0 : i + 1);
            p.sendMessage(
              "%sYour position: %s%s%d %d %d%s%s -> %s%s%d %d %d".formatted(
                ChatColor.AQUA,
                ChatColor.YELLOW,
                ChatColor.BOLD,
                fromLoc.getBlockX(),
                fromLoc.getBlockY(),
                fromLoc.getBlockZ(),
                ChatColor.RESET,
                ChatColor.AQUA,
                ChatColor.YELLOW,
                ChatColor.BOLD,
                toLoc.getBlockX(),
                toLoc.getBlockY(),
                toLoc.getBlockZ()
              ));
            p.teleport(toLoc);
            p.playSound(
              toLoc,
              Sound.ENTITY_ENDERMAN_TELEPORT,
              SoundCategory.MASTER,
              1F,
              1F);
          }
        }
      }
    }
  }
}
