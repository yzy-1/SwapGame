package leafor.swap.controllers;

import javax.annotation.Nonnull;

public abstract class Controller {
  private static Controller controller;

  protected static void SetController(@Nonnull Controller c) {
    controller = c;
  }

  public static Controller GetController() {
    return controller;
  }

  public static void Init() {
    controller = new InitController();
  }
}
