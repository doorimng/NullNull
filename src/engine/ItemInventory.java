package engine;

import engine.ItemEffect.ItemEffectType;

/**
 * Manages the inventory display of active items for a player.
 * Maximum 2 slots - when full, player cannot pick up new duration items.
 */
public class ItemInventory {

    private static final int MAX_SLOTS = 2;

    /** Slots storing active item types */
    private ItemEffectType[] slots;

    /** Reference to GameState to check active effects */
    private GameState gameState;

    /** Player index (0 for P1) */
    private int playerIndex;

    /**
     * Constructor
     *
     * @param gameState Reference to game state
     * @param playerIndex Player index (0 or 1)
     */
    public ItemInventory(GameState gameState, int playerIndex) {
        this.gameState = gameState;
        this.playerIndex = playerIndex;
        this.slots = new ItemEffectType[MAX_SLOTS];
    }

    /**
     * Updates inventory by checking active effects.
     * Removes expired items from slots.
     */
    public void update() {
        for (int i = 0; i < MAX_SLOTS; i++) {
            if (slots[i] != null) {
                // Check if effect is still active
                if (!gameState.hasEffect(playerIndex, slots[i])) {
                    slots[i] = null; // Remove expired item
                }
            }
        }
    }

    /**
     * Attempts to add an item to inventory.
     *
     * @param itemType Type of item to add
     * @return true if added successfully, false if inventory full
     */
    public boolean addItem(ItemEffectType itemType) {
        // Check if item already exists in inventory
        for (int i = 0; i < MAX_SLOTS; i++) {
            if (slots[i] == itemType) {
                return true; // Already have this item (duration extended)
            }
        }

        // Find empty slot
        for (int i = 0; i < MAX_SLOTS; i++) {
            if (slots[i] == null) {
                slots[i] = itemType;
                return true;
            }
        }

        return false; // Inventory full
    }

    /**
     * Checks if inventory is full.
     *
     * @return true if all slots occupied, false otherwise
     */
    public boolean isFull() {
        for (int i = 0; i < MAX_SLOTS; i++) {
            if (slots[i] == null) {
                return false;
            }
        }
        return true;
    }

    /**
     * Gets the item type in a specific slot.
     *
     * @param slotIndex Slot index (0 or 1)
     * @return ItemEffectType or null if empty
     */
    public ItemEffectType getSlot(int slotIndex) {
        if (slotIndex >= 0 && slotIndex < MAX_SLOTS) {
            return slots[slotIndex];
        }
        return null;
    }

    /**
     * Gets remaining duration for item in slot.
     *
     * @param slotIndex Slot index (0 or 1)
     * @return Remaining milliseconds, or 0 if empty
     */
    public int getRemainingDuration(int slotIndex) {
        ItemEffectType itemType = getSlot(slotIndex);
        if (itemType != null && gameState.hasEffect(playerIndex, itemType)) {
            return gameState.getEffectDuration(playerIndex, itemType);
        }
        return 0;
    }

    /**
     * Clears all inventory slots.
     * Called at the start of each level.
     */
    public void clear() {
        for (int i = 0; i < MAX_SLOTS; i++) {
            slots[i] = null;
        }
    }

    /**
     * Gets the maximum number of inventory slots.
     *
     * @return Maximum slots (always 2)
     */
    public int getMaxSlots() {
        return MAX_SLOTS;
    }
}