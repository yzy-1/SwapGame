package leafor.swap.controllers;

import leafor.swap.Main;
import leafor.swap.utils.WorldHelper;
import org.bukkit.Bukkit;
import org.bukkit.World;

import javax.annotation.Nonnull;

public final class CleanController extends Controller {
  public CleanController(@Nonnull World gameWorld) {
    Bukkit.resetRecipes();
    if (!WorldHelper.UnloadWorld(gameWorld)) {
      throw new RuntimeException("Unload world failure");
    }
    if (!WorldHelper.DeleteWorld(gameWorld.getWorldFolder())) {
      throw new RuntimeException("Delete world failure");
    }
    Bukkit.getScheduler().runTask(Main.GetInstance(), Controller::Init);
  }
}
