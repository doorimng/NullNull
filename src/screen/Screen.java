package screen;

import java.awt.Insets;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import engine.*;
import entity.Entity;
import entity.Item;
import entity.ItemPool;
import entity.Ship;



/**
 * Implements a generic screen.
 *
 * @author <a href="mailto:RobertoIA1987@gmail.com">Roberto Izquierdo Amo</a>
 *
 */
public class Screen {

	/** Milliseconds until the screen accepts user input. */
	private static final int INPUT_DELAY = 1000;

	/** Draw Manager instance. */
	protected DrawManager drawManager;
	/** Input Manager instance. */
	protected InputManager inputManager;
	/** Application logger. */
	protected Logger logger;

	/** Screen width. */
	protected int width;
	/** Screen height. */
	protected int height;
	/** Frames per second shown on the screen. */
	protected int fps;
	/** Screen insets. */
	protected Insets insets;
	/** Time until the screen accepts user input. */
	protected Cooldown inputDelay;

	/** If the screen is running. */
	protected boolean isRunning;
	/** What kind of screen goes next. */
	protected int returnCode;

    /** Item inventory for player (null for non-game screens) */
    protected ItemInventory inventory;

	/**
	 * Constructor, establishes the properties of the screen.
	 *
	 * @param width
	 *            Screen width.
	 * @param height
	 *            Screen height.
	 * @param fps
	 *            Frames per second, frame rate at which the game is run.
	 */
	public Screen(final int width, final int height, final int fps) {
		this.width = width;
		this.height = height;
		this.fps = fps;

		this.drawManager = Core.getDrawManager();
		this.inputManager = Core.getInputManager();
		this.logger = Core.getLogger();
		this.inputDelay = Core.getCooldown(INPUT_DELAY);
		this.inputDelay.reset();
		this.returnCode = 0;
	}

	/**
	 * Initializes basic screen properties.
	 */
	public void initialize() {

	}

	/**
	 * Activates the screen.
	 *
	 * @return Next screen code.
	 */
	public int run() {
		this.isRunning = true;

		while (this.isRunning) {
			long time = System.currentTimeMillis();

			update();

			time = (1000 / this.fps) - (System.currentTimeMillis() - time);
			if (time > 0) {
				try {
					TimeUnit.MILLISECONDS.sleep(time);
				} catch (InterruptedException e) {
					return 0;
				}
			}
		}

		return 0;
	}

	/**
	 * Updates the elements on screen and checks for events.
	 */
	protected void update() {
	}

	/**
	 * Getter for screen width.
	 *
	 * @return Screen width.
	 */
	public final int getWidth() {
		return this.width;
	}

	/**
	 * Getter for screen height.
	 *
	 * @return Screen height.
	 */
	public final int getHeight() {
		return this.height;
	}

    /**
     * Initializes player inventory
     * Should be called in initialize() of GameScreen and BossScreen
     *
     * @param gameState GameState instance
     * @param playerIndex Player index (0 for single player)
     */
    protected void initializeInventory(GameState gameState, int playerIndex) {
        this.inventory = new ItemInventory(gameState, playerIndex);
    }

    /**
     * Updates inventory (removes expired items)
     * Should be called in update() of GameScreen and BossScreen
     */
    protected void updateInventory() {
        if (inventory != null) {
            inventory.update();
        }
    }

    /**
     * Draws player's item inventory
     * Common method to avoid code duplication in GameScreen and BossScreen
     * Just calls DrawManager's method
     *
     * @param positionX X position for inventory display
     * @param positionY Y position for inventory display
     */
    protected void drawInventory(int positionX, int positionY) {
        if (inventory != null) {
            drawManager.drawItemInventory(inventory, positionX, positionY);
        }
    }

    /**
     * Manages item pickups between player ships and items
     */
    /**
     * Manages item pickups between player ships and items
     */
    protected void manageItemPickups(Set<Item> items, Ship[] ships, GameState gameState) {
        Set<Item> collected = new HashSet<>();

        for (Item item : items) {
            for (Ship ship : ships) {
                if (ship == null) continue;

                if (checkCollision(item, ship) && !collected.contains(item)) {
                    collected.add(item);
                    logger.info("Player " + ship.getPlayerId() + " picked up item: " + item.getType());
                    SoundManager.playOnce("sound/hover.wav");

                    boolean applied = item.applyEffect(gameState, ship.getPlayerId());

                    if (applied && isDurationItem(item.getType())) {
                        ItemEffect.ItemEffectType effectType = getEffectTypeFromItem(item.getType());
                        if (effectType != null && inventory != null) {
                            inventory.addItem(effectType);
                        }
                    }
                }
            }
        }

        items.removeAll(collected);
        ItemPool.recycle(collected);
    }


    /**
     * Checks if two entities are colliding
     */
    public boolean checkCollision(final Entity a, final Entity b) {
        int centerAX = a.getPositionX() + a.getWidth() / 2;
        int centerAY = a.getPositionY() + a.getHeight() / 2;
        int centerBX = b.getPositionX() + b.getWidth() / 2;
        int centerBY = b.getPositionY() + b.getHeight() / 2;
        int maxDistanceX = a.getWidth() / 2 + b.getWidth() / 2;
        int maxDistanceY = a.getHeight() / 2 + b.getHeight() / 2;
        int distanceX = Math.abs(centerAX - centerBX);
        int distanceY = Math.abs(centerAY - centerBY);
        return distanceX < maxDistanceX && distanceY < maxDistanceY;
    }

    /**
     * Checks if item is a duration-based item
     */
    private boolean isDurationItem(String itemType) {
        return itemType.equals("TRIPLESHOT")
                || itemType.equals("SCOREBOOST")
                || itemType.equals("BULLETSPEEDUP");
    }

    /**
     * Converts item type string to ItemEffectType enum
     */
    private ItemEffect.ItemEffectType getEffectTypeFromItem(String itemType) {
        switch (itemType) {
            case "TRIPLESHOT":
                return ItemEffect.ItemEffectType.TRIPLESHOT;
            case "SCOREBOOST":
                return ItemEffect.ItemEffectType.SCOREBOOST;
            case "BULLETSPEEDUP":
                return ItemEffect.ItemEffectType.BULLETSPEEDUP;
            default:
                return null;
        }
    }


}