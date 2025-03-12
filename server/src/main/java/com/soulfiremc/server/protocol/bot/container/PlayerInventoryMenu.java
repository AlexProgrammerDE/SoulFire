package com.soulfiremc.server.protocol.bot.container;

import com.soulfiremc.server.data.MenuType;
import com.soulfiremc.server.protocol.bot.state.entity.Player;
import lombok.Getter;

@Getter
public class PlayerInventoryMenu extends ViewableContainer {
  private static final int HOTBAR_START = 36;
  private final ContainerSlot craftingResult = getSlot(0);
  private final ContainerSlot[] craftingGrid = getSlots(1, 4);
  private final ContainerSlot[] mainInventory = getSlots(9, 35);
  private final ContainerSlot[] hotbar = getSlots(36, 44);
  /**
   * Retrieves the storage slots of the container. This includes the main inventory and the hotbar.
   */
  private final ContainerSlot[] storage = getSlots(9, 44);
  private final ContainerSlot[] armor = getSlots(5, 8);
  private final ContainerSlot offhand = getSlot(45);
  private final PlayerInventory playerInventory;

  public PlayerInventoryMenu(Player player, PlayerInventory playerInventory) {
    super(player, MenuType.SOULFIRE_INVENTORY_MENU.slots(), 0);
    this.playerInventory = playerInventory;
    for (var slotEntry : MenuType.SOULFIRE_INVENTORY_MENU.playerInventory().entrySet()) {
      this.getSlot(slotEntry.getKey()).setStorageFrom(player.inventory().getSlot(slotEntry.getValue()));
    }
  }

  public static boolean isHotbarSlot(ContainerSlot slot) {
    return slot.slot() >= 36 && slot.slot() < 45;
  }

  public static int toHotbarIndex(ContainerSlot slot) {
    return slot.slot() - HOTBAR_START;
  }

  public static boolean isMainInventory(ContainerSlot slot) {
    return slot.slot() >= 9 && slot.slot() < 36;
  }

  public ContainerSlot getSelectedSlot() {
    return getSlot(HOTBAR_START + playerInventory.selected);
  }

  public boolean isHeldItem(ContainerSlot slot) {
    return slot == getSelectedSlot();
  }
}
