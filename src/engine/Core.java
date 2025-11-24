package engine;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import screen.*;
import entity.Ship;
import screen.*;
import screen.BossScreen;


/**
 * Implements core game logic.
 *
 * @author <a href="mailto:RobertoIA1987@gmail.com">Roberto Izquierdo Amo</a>
 *
 */
public final class Core {

    private static final int WIDTH = 448;
    private static final int HEIGHT = 520;
    private static final int FPS = 60;

    /** Lives per player (used to compute team pool in shared mode). */
    private static final int MAX_LIVES = 100;
    private static final int EXTRA_LIFE_FRECUENCY = 3;

    /** Frame to draw the screen on. */
    private static Frame frame;
    private static Screen currentScreen;
    private static List<GameSettings> gameSettings;
    private static final Logger LOGGER = Logger.getLogger(Core.class.getSimpleName());
    private static Handler fileHandler;
    private static ConsoleHandler consoleHandler;
    private static int NUM_LEVELS; // Total number of levels
    private static int currentLevel = 6 ;

    /**
     * Test implementation.
     *
     * @param args
     *             Program args, ignored.
     */
    public static void main(final String[] args) throws IOException {
        try {
            LOGGER.setUseParentHandlers(false);
            fileHandler = new FileHandler("log");
            fileHandler.setFormatter(new MinimalFormatter());
            consoleHandler = new ConsoleHandler();
            consoleHandler.setFormatter(new MinimalFormatter());
            LOGGER.addHandler(fileHandler);
            LOGGER.addHandler(consoleHandler);
            LOGGER.setLevel(Level.ALL);
        } catch (Exception e) {
            e.printStackTrace();
        }

        frame = new Frame(WIDTH, HEIGHT);
        InputManager input = InputManager.getInstance();
        frame.addKeyListener(input); // Register an instance to allow the window to receive keyboard event information
        DrawManager.getInstance().setFrame(frame);
        int width = frame.getWidth();
        int height = frame.getHeight();

        gameSettings = GameSettings.getGameSettings();
        NUM_LEVELS = gameSettings.size(); // Initialize total number of levels


        // 2P mode: modified to null to allow for switch between 2 modes
        GameState gameState = null;
        boolean coopSelected = false; // false = 1P, true = 2P

        int returnCode = 1;

        Ship.ShipType shipTypeP1 = Ship.ShipType.NORMAL; // P1 Ship Type
        Ship.ShipType shipTypeP2 = Ship.ShipType.NORMAL; // P2 Ship Type
        do {
            // Game & score.
            AchievementManager achievementManager = new AchievementManager(); // add 1P/2P achievement manager

            switch (returnCode) {
                case 1:
                    currentScreen = new TitleScreen(width, height, FPS);
                    LOGGER.info("Starting " + WIDTH + "x" + HEIGHT + " title screen at " + FPS + " fps.");
                    returnCode = frame.setScreen(currentScreen);
                    LOGGER.info("Closing title screen.");

                    // 2P mode: reading the mode which user chose from TitleScreen
                    // (edit) TitleScreen to PlayScreen
                    if (returnCode == 2) {
                        currentScreen = new PlayScreen(width, height, FPS);
                        returnCode = frame.setScreen(currentScreen);

                        coopSelected = ((PlayScreen) currentScreen).isCoopSelected();
                    }

                    break;

                case 2:
                    // 2P mode: building gameState now using user choice
                    if (gameState == null) {
                        gameState = new GameState(currentLevel, MAX_LIVES, coopSelected, 0);
                    }

                    // [ ... case 2: ... ]
                    do {
                        // Extra life this level? Give it if team pool is below cap.
                        int teamCap = gameState.isCoop() ? (MAX_LIVES * GameState.NUM_PLAYERS) : MAX_LIVES;
                        boolean bonusLife = gameState.getLevel() % EXTRA_LIFE_FRECUENCY == 0
                                && gameState.getLivesRemaining() < teamCap;

                        currentLevel = gameState.getLevel();

                        // 레벨 시작 전마다 MapScreen을 띄웁니다.
                        currentScreen = new MapScreen(width, height, FPS, currentLevel) ;
                        returnCode = frame.setScreen(currentScreen) ;

                        // esc 버튼을 누르면 TitleScreen으로 이동합니다.
                        if (returnCode == 1) {
                            returnCode = 9 ;
                            break;
                        }

                        // 레벨 시작 전마다 StoryScreen을 띄웁니다.
                        currentScreen = new StoryScreen(width, height, FPS, currentLevel) ;
                        returnCode = frame.setScreen(currentScreen) ;

                        // esc 버튼을 누르면 TitleScreen으로 이동합니다.
                        if (returnCode == 1) {
                            returnCode = 9 ;
                            break;
                        }

                        if (currentLevel == gameSettings.size() + 1) { // 보스 레벨
                            currentScreen = new BossScreen(
                                    gameState, width, height, FPS,
                                    shipTypeP1, shipTypeP2, achievementManager); // BossScreen 생성

                            LOGGER.info("Starting Boss Screen.");

                        } else if (currentLevel <= gameSettings.size()) { // 일반 레벨 : MapScreen -> GameScreen
                            currentScreen = new GameScreen(
                                    gameState,
                                    gameSettings.get(currentLevel - 1),
                                    bonusLife, width, height, FPS, shipTypeP1, shipTypeP2, achievementManager);

                            LOGGER.info("Starting Game Screen Level " + currentLevel);

                        } else {
                            // 모든 레벨과 보스를 클리어함
                            break;
                        }

                        // === 이 부분이 빠져있었습니다! ===
                        LOGGER.info("Starting " + WIDTH + "x" + HEIGHT + " game screen at " + FPS + " fps.");
                        returnCode = frame.setScreen(currentScreen); // 1. 스크린을 실제로 실행
                        LOGGER.info("Closing game screen.");

                        // 2. 게임 도중 메뉴로 나갔을 경우 (Pause -> Backspace)
                        if (returnCode == 1) {
                            returnCode = 10;
                            break;
                        }

                        // 3. 실행된 스크린의 최신 gameState를 가져옴
                        if (currentScreen instanceof GameScreen gameScreen) {
                            gameState = gameScreen.getGameState();
                        } else if (currentScreen instanceof BossScreen bossScreen) {
                            // BossScreen.java에도 getGameState()가 구현되어 있어야 합니다.
                            gameState = bossScreen.getGameState();
                        }
                        // ===================================


                        if (gameState.teamAlive()) {
                            gameState.nextLevel();
                        }

                    } while (gameState.teamAlive() && gameState.getLevel() <= (gameSettings.size() + 1)); // +1은 보스 레벨

                    LOGGER.info("Starting " + WIDTH + "x" + HEIGHT + " score screen at " + FPS + " fps, with a score of "
                            + gameState.getScore() + ", "
                            + gameState.getLivesRemaining() + " lives remaining, "
                            + gameState.getBulletsShot() + " bullets shot and "
                            + gameState.getShipsDestroyed() + " ships destroyed.");
                    if ( returnCode < 9 ) { // 도중 나갔을 경우 ScoreScreen은 표시되지 않습니다.
                        currentScreen = new ScoreScreen(width, height, FPS, gameState, achievementManager);
                        returnCode = frame.setScreen(currentScreen);
                    } else if ( returnCode == 9 ) { // 게임 플레이 도중 나갔을 경우 MapScreen으로 돌아갑니다.
                        returnCode = 1;
                    } else returnCode = 2; // MapScreen에서 도중 나갔을 경우 TitleScreen으로 돌아갑니다.
                    LOGGER.info("Closing score screen.");
                    break;

                case 3:
                    // Achievements.
                    currentScreen = new AchievementScreen(width, height, FPS);
                    LOGGER.info("Starting " + WIDTH + "x" + HEIGHT
                            + " achievements screen at " + FPS + " fps.");
                    returnCode = frame.setScreen(currentScreen);
                    LOGGER.info("Closing achievement screen.");
                    break;

                case 4:
                    // settings screen
                    currentScreen = new SettingScreen(width, height, FPS);
                    LOGGER.info("Starting " + WIDTH + "x" + HEIGHT
                            + " setting screen at " + FPS + " fps.");
                    returnCode = frame.setScreen(currentScreen);
                    LOGGER.info("Closing setting screen.");
                    frame.removeKeyListener(InputManager.getInstance());
                    frame.addKeyListener(InputManager.getInstance()); // Remove and re-register the input manager, forcing the key setting of the frame to be updated
                    break;

                case 5:
                    // Play : Use the play to decide 1p and 2p
                    currentScreen = new PlayScreen(width, height, FPS);
                    LOGGER.info("Starting " + WIDTH + "x" + HEIGHT + " play screen at " + FPS + " fps.");
                    returnCode = frame.setScreen(currentScreen);
                    coopSelected = ((PlayScreen) currentScreen).isCoopSelected();

                    // playscreen -> shipselectionscreen
                    if (returnCode == 2) {
                        returnCode = 6;
                    }
                    LOGGER.info("Closing play screen.");
                    break;

                case 6:
                    // Ship selection for Player 1.
                    currentScreen = new ShipSelectionScreen(width, height, FPS, 1);
                    returnCode = frame.setScreen(currentScreen);
                    shipTypeP1 = ((ShipSelectionScreen) currentScreen).getSelectedShipType();

                    // If clicked back button, go back to the screen 1P screen -> Player select screen
                    if (returnCode == 5) { break; }

                    if (coopSelected) {
                        returnCode = 7; // Go to Player 2 selection.
                    } else {
                        returnCode = 2; // Start game.
                    }
                    break;

                case 7:
                    // Ship selection for Player 2.
                    currentScreen = new ShipSelectionScreen(width, height, FPS, 2);
                    returnCode = frame.setScreen(currentScreen);

                    // If clicked back button, go back to the screen 2P screen -> 1P screen
                    if (returnCode == 6) { break; }

                    shipTypeP2 = ((ShipSelectionScreen) currentScreen).getSelectedShipType();
                    returnCode = 2; // Start game.
                    break;

                case 8:
                    // High scores.
                    currentScreen = new HighScoreScreen(width, height, FPS);
                    LOGGER.info("Starting " + WIDTH + "x" + HEIGHT
                            + " high score screen at " + FPS + " fps.");
                    returnCode = frame.setScreen(currentScreen);
                    LOGGER.info("Closing high score screen.");
                    break;

                default:
                    break;
            }

        } while (returnCode != 0);

        fileHandler.flush();
        fileHandler.close();
        System.exit(0);
    }

    /**
     * Constructor, not called.
     */
    private Core() {

    }

    /**
     * Controls access to the logger.
     *
     *
     * @return Application logger.
     */
    public static Logger getLogger() {
        return LOGGER;
    }

    /**
     * Controls access to the drawing manager.
     *
     * @return Application draw manager.
     */
    public static DrawManager getDrawManager() {
        return DrawManager.getInstance();
    }

    /**
     * Controls access to the input manager.
     *
     * @return Application input manager.
     */
    public static InputManager getInputManager() {
        return InputManager.getInstance();
    }

    /**
     * Controls access to the file manager.
     *
     * @return Application file manager.
     */
    public static FileManager getFileManager() {
        return FileManager.getInstance();
    }

    /**
     * Controls creation of new cooldowns.
     *
     * @param milliseconds
     *                     Duration of the cooldown.
     * @return A new cooldown.
     */
    public static Cooldown getCooldown(final int milliseconds) {
        return new Cooldown(milliseconds);
    }

    /**
     * Controls access to the achievement manager.
     *
     * @return Application achievement manager.
     * [2025-10-09] Added in commit: feat: complete drawAchievementMenu method in DrawManager
     */
    public static AchievementManager getAchievementManager() {
        return AchievementManager.getInstance();
    }

    /**
     * Controls creation of new cooldowns with variance.
     *
     * @param milliseconds
     *                     Duration of the cooldown.
     * @param variance
     *                     Variation in the cooldown duration.
     * @return A new cooldown with variance.
     */
    public static Cooldown getVariableCooldown(final int milliseconds, final int variance) {
        return new Cooldown(milliseconds, variance);
    }

    private static int volumeLevel = 50;

    public static int getVolumeLevel() {
        return volumeLevel;
    }

    public static void setVolumeLevel(int v) {
        volumeLevel = Math.max(0, Math.min(100, v));
    }
}