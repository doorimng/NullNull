package engine;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages a 2-slot inventory for duration-based items.
 * Each slot holds an active item effect type.
 * Similar to item system in racing games like KartRider.
 */
public class Inventory {

    /** Maximum number of inventory slots */
    private static final int MAX_SLOTS = 2;

    /** Array of item effect types in inventory slots (null = empty) */
    private ItemEffect.ItemEffectType[] slots;

    /**
     * Constructor - initializes empty inventory
     */
    public Inventory() {
        this.slots = new ItemEffect.ItemEffectType[MAX_SLOTS];
    }

    /**
     * Checks if inventory is full (all slots occupied)
     *
     * @return true if all slots are filled
     */
    public boolean isFull() {
        for (ItemEffect.ItemEffectType slot : slots) {
            if (slot == null) {
                return false;
            }
        }
        return true;
    }

    /**
     * Adds an item to the first available slot
     *
     * @param itemType the item effect type to add
     * @return true if successfully added, false if inventory is full
     */
    public boolean addItem(ItemEffect.ItemEffectType itemType) {
        if (itemType == null) {
            return false;
        }

        for (int i = 0; i < MAX_SLOTS; i++) {
            if (slots[i] == null) {
                slots[i] = itemType;
                return true;
            }
        }
        return false; // Inventory full
    }

    /**
     * Checks if a specific item type exists in inventory
     *
     * @param itemType the item effect type to check
     * @return true if item exists in any slot
     */
    public boolean hasItem(ItemEffect.ItemEffectType itemType) {
        if (itemType == null) {
            return false;
        }

        for (ItemEffect.ItemEffectType slot : slots) {
            if (itemType.equals(slot)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Removes the first occurrence of an item from inventory
     *
     * @param itemType the item effect type to remove
     * @return true if item was found and removed
     */
    public boolean removeItem(ItemEffect.ItemEffectType itemType) {
        if (itemType == null) {
            return false;
        }

        for (int i = 0; i < MAX_SLOTS; i++) {
            if (itemType.equals(slots[i])) {
                slots[i] = null;
                return true;
            }
        }
        return false;
    }

    /**
     * Gets all inventory slots
     *
     * @return array of item effect types (null = empty slot)
     */
    public ItemEffect.ItemEffectType[] getSlots() {
        return slots;
    }

    /**
     * Gets the number of occupied slots
     *
     * @return count of non-null slots
     */
    public int getOccupiedCount() {
        int count = 0;
        for (ItemEffect.ItemEffectType slot : slots) {
            if (slot != null) {
                count++;
            }
        }
        return count;
    }

    /**
     * Gets list of all active items in inventory
     *
     * @return list of non-null item effect types
     */
    public List<ItemEffect.ItemEffectType> getActiveItems() {
        List<ItemEffect.ItemEffectType> activeItems = new ArrayList<>();
        for (ItemEffect.ItemEffectType slot : slots) {
            if (slot != null) {
                activeItems.add(slot);
            }
        }
        return activeItems;
    }

    /**
     * Clears all inventory slots (resets to empty)
     */
    public void clear() {
        for (int i = 0; i < MAX_SLOTS; i++) {
            slots[i] = null;
        }
    }

    /**
     * Gets the maximum number of slots
     *
     * @return maximum inventory capacity
     */
    public int getMaxSlots() {
        return MAX_SLOTS;
    }
}