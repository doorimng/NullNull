package screen;

import java.awt.event.KeyEvent;
import java.util.HashSet;
import java.util.Set;

import engine.*;
import entity.*;

// NEW Item code

/**
 * Implements the game screen, where the action happens.(supports co-op with
 * shared team lives)
 *
 * @author <a href="mailto:RobertoIA1987@gmail.com">Roberto Izquierdo Amo</a>
 *
 */
public class GameScreen extends ReviveScreen {

    /** Milliseconds until the screen accepts user input. */
    private static final int INPUT_DELAY = 6000;
    /** Bonus score for each life remaining at the end of the level. */
    private static final int LIFE_SCORE = 100;
    /** Minimum time between bonus ship's appearances. */
    private static final int BONUS_SHIP_INTERVAL = 20000;
    /** Maximum variance in the time between bonus ship's appearances. */
    private static final int BONUS_SHIP_VARIANCE = 10000;
    /** Time until bonus ship explosion disappears. */
    private static final int BONUS_SHIP_EXPLOSION = 500;
    /** Time from finishing the level to screen change. */
    private static final int SCREEN_CHANGE_INTERVAL = 1500;
    /** Height of the interface separation line. */
    private static final int SEPARATION_LINE_HEIGHT = 68;
    private static final int HIGH_SCORE_NOTICE_DURATION = 2000;
    private static boolean sessionHighScoreNotified = false;

    /** For Check Achievement */
    private AchievementManager achievementManager;
    /** Current game difficulty settings. */
    private GameSettings gameSettings;
    /** Current difficulty level number. */
    private int level;
    /** Formation of enemy ships. */
    private EnemyShipFormation enemyShipFormation;
    private EnemyShip enemyShipSpecial;
    /** Formation of player ships. */
    private Ship[] ships = new Ship[GameState.NUM_PLAYERS];
    /** Minimum time between bonus ship appearances. */
    private Cooldown enemyShipSpecialCooldown;
    /** Time until bonus ship explosion disappears. */
    private Cooldown enemyShipSpecialExplosionCooldown;
    /** Time from finishing the level to screen change. */
    private Cooldown screenFinishedCooldown;
    /** Set of all bullets fired by on screen ships. */
    private Set<Bullet> bullets;
    /** Set of all items spawned. */
    private Set<Item> items;
    private long gameStartTime;
    /** Checks if the level is finished. */
    private boolean levelFinished;
    /** Checks if a bonus life is received. */
    private boolean bonusLife;
    private int topScore;
    private boolean highScoreNotified;
    private long highScoreNoticeStartTime;

    private boolean isPaused;
    private Cooldown pauseCooldown;
    private Cooldown returnMenuCooldown;

    private int score;
    private int lives;
    private int bulletsShot;
    private int shipsDestroyed;

    /** checks if player took damage */
    private boolean tookDamageThisLevel;
    private boolean countdownSoundPlayed = false;

    private Ship.ShipType shipTypeP1;
    private Ship.ShipType shipTypeP2;

    /**
     * Constructor, establishes the properties of the screen.
     *
     * @param gameState        Current game state.
     * @param gameSettings     Current game settings.
     * @param bonusLife        Checks if a bonus life is awarded this level.
     * @param width            Screen width.
     * @param height           Screen height.
     * @param fps              Frames per second.
     * @param shipTypeP1       Player 1's ship type.
     * @param shipTypeP2       Player 2's ship type.
     * @param achievementManager Achievement manager instance.
     */
    public GameScreen(final GameState gameState,
                      final GameSettings gameSettings, final boolean bonusLife,
                      final int width, final int height, final int fps,
                      final Ship.ShipType shipTypeP1, final Ship.ShipType shipTypeP2,
                      final AchievementManager achievementManager) {

        super(gameState, width, height, fps);

        this.gameSettings = gameSettings;
        this.bonusLife = bonusLife;
        this.shipTypeP1 = shipTypeP1;
        this.shipTypeP2 = shipTypeP2;
        this.level = gameState.getLevel();
        this.score = gameState.getScore();
        this.lives = gameState.getLivesRemaining();
        if (this.bonusLife)
            this.lives++;
        this.bulletsShot = gameState.getBulletsShot();
        this.shipsDestroyed = gameState.getShipsDestroyed();

        this.achievementManager = achievementManager;
        this.tookDamageThisLevel = false;

        this.highScoreNotified = false;
        this.highScoreNoticeStartTime = 0;

        // 2P: bonus life adds to team pool + singleplayer mode
        if (this.bonusLife) {
            if (state.isSharedLives()) {
                state.addTeamLife(1); // two player
            } else {
                // 1P legacy: grant to P1
                state.addLife(0, 1);  // singleplayer
            }
        }
        if (this.achievementManager == null) {
            this.achievementManager = new AchievementManager();
        }
    }

    /**
     * Resets the session high score notification flag.
     * Should be called when a new game starts from the main menu.
     */
    public static void resetSessionHighScoreNotified() {
        sessionHighScoreNotified = false;
    }

    /**
     * Initializes basic screen properties, and adds necessary elements.
     */
    public final void initialize() {
        super.initialize();

        state.clearAllEffects();

        // Start background music for gameplay
        SoundManager.startBackgroundMusic("sound/SpaceInvader-GameTheme.wav");

        enemyShipFormation = new EnemyShipFormation(this.gameSettings);
        enemyShipFormation.attach(this);

        // 2P mode: create both ships, tagged to their respective teams
        this.ships[0] = new Ship(this.width / 2 - 60, this.height - 30,
                Entity.Team.PLAYER1, shipTypeP1, this.state); // P1
        this.ships[0].setPlayerId(1);

        // only allowing second ship to spawn when 2P mode is chosen
        if (state.isCoop()) {
            this.ships[1] = new Ship(this.width / 2 + 60, this.height - 30,
                    Entity.Team.PLAYER2, shipTypeP2, this.state); // P2
            this.ships[1].setPlayerId(2);
        } else {
            this.ships[1] = null; // ensuring there's no P2 ship in 1P mode
        }

        this.enemyShipSpecialCooldown =
                Core.getVariableCooldown(BONUS_SHIP_INTERVAL, BONUS_SHIP_VARIANCE);
        this.enemyShipSpecialCooldown.reset();
        this.enemyShipSpecialExplosionCooldown = Core.getCooldown(BONUS_SHIP_EXPLOSION);
        this.screenFinishedCooldown = Core.getCooldown(SCREEN_CHANGE_INTERVAL);
        this.bullets = new HashSet<>();

        // New Item Code
        this.items = new HashSet<>();

        // Special input delay / countdown.
        this.gameStartTime = System.currentTimeMillis();
        this.inputDelay = Core.getCooldown(INPUT_DELAY);
        this.inputDelay.reset();
        drawManager.setDeath(false);

        this.isPaused = false;
        this.pauseCooldown = Core.getCooldown(300);
        this.returnMenuCooldown = Core.getCooldown(300);

        // Revive 상태 초기화 (부모 클래스)
        initReviveState();

        initializeInventory(state, 0);
    }

    /**
     * Starts the action.
     *
     * @return Next screen code.
     */
    public final int run() {
        super.run();

        // 2P mode: award bonus score for remaining TEAM lives
        state.addScore(0, LIFE_SCORE * state.getLivesRemaining());

        // Stop all music on exiting this screen
        SoundManager.stopAllMusic();

        this.logger.info("Screen cleared with a score of " + state.getScore());
        return this.returnCode;
    }

    /**
     * Updates the elements on screen and checks for events.
     */
    protected final void update() {
        super.update();

        /// ----------------------------------------
        // Revive Phase Handler (공통 헬퍼 사용)
        // ----------------------------------------
        if (!handleRevivePhaseState(this.inputManager)) {
            draw();     // 화면만 다시 그리고
            return;     // 이 프레임 로직 종료
        }


        // Countdown beep once during pre-start
        if (!this.inputDelay.checkFinished() && !countdownSoundPlayed) {
            long elapsed = System.currentTimeMillis() - this.gameStartTime;
            if (elapsed > 1750) {
                SoundManager.playOnce("sound/CountDownSound.wav");
                countdownSoundPlayed = true;
            }
        }

        checkAchievement();

        // pause 토글
        if (this.inputDelay.checkFinished()
                && inputManager.isKeyDown(KeyEvent.VK_ESCAPE)
                && this.pauseCooldown.checkFinished()) {
            this.isPaused = !this.isPaused;
            this.pauseCooldown.reset();

            if (this.isPaused) {
                // Pause game music when pausing - no sound during pause
                SoundManager.stopBackgroundMusic();
            } else {
                // Resume game music when unpausing
                SoundManager.startBackgroundMusic("sound/SpaceInvader-GameTheme.wav");
            }
        }

        // pause 중 메뉴로 복귀
        if (this.isPaused
                && inputManager.isKeyDown(KeyEvent.VK_BACK_SPACE)
                && this.returnMenuCooldown.checkFinished()) {
            SoundManager.playOnce("sound/select.wav");
            SoundManager.stopAllMusic(); // Stop all music before returning to menu
            returnCode = 1;
            this.isRunning = false;
        }

        if (!this.isPaused) {
            if (this.inputDelay.checkFinished() && !this.levelFinished) {

                // Per-player input/move/shoot
                // Per-player input/move/shoot
                for (int p = 0; p < GameState.NUM_PLAYERS; p++) {
                    handleSingleShipInput(p, this.ships, this.bullets, this.state);
                }


                // Special ship lifecycle
                if (this.enemyShipSpecial != null) {
                    if (!this.enemyShipSpecial.isDestroyed())
                        this.enemyShipSpecial.move(2, 0);
                    else if (this.enemyShipSpecialExplosionCooldown.checkFinished())
                        this.enemyShipSpecial = null;
                }
                if (this.enemyShipSpecial == null
                        && this.enemyShipSpecialCooldown.checkFinished()) {
                    this.enemyShipSpecial = new EnemyShip();
                    this.enemyShipSpecialCooldown.reset();
                    SoundManager.playLoop("sound/special_ship_sound.wav");
                    this.logger.info("A special ship appears");
                }
                if (this.enemyShipSpecial != null
                        && this.enemyShipSpecial.getPositionX() > this.width) {
                    this.enemyShipSpecial = null;
                    SoundManager.stop();
                    this.logger.info("The special ship has escaped");
                }

                // Update ships & enemies
                for (Ship s : this.ships)
                    if (s != null)
                        s.update();

                this.enemyShipFormation.update();
                int bulletsBefore = this.bullets.size();
                this.enemyShipFormation.shoot(this.bullets);
                if (this.bullets.size() > bulletsBefore) {
                    // At least one enemy bullet added
                    SoundManager.playOnce("sound/shoot_enemies.wav");
                }
            }

            manageCollisions();

            // collision 에서 revivePhase 가 PROMPT 로 바뀌었으면 여기서 멈춤
            if (this.revivePhase != RevivePhase.PLAYING) {
                return;
            }

            cleanBullets();

            // Item Entity Code
            cleanItems();

            manageItemPickups();
            updateInventory();

            // check active item affects
            state.updateEffects();
            drawManager.setLastLife(state.getLivesRemaining() == 1);
            draw();

            if (!sessionHighScoreNotified
                    && this.state.getScore() > this.topScore) {
                sessionHighScoreNotified = true;
                this.highScoreNotified = true;
                this.highScoreNoticeStartTime = System.currentTimeMillis();
            }

            // End condition: formation cleared or TEAM lives exhausted.
            if ((this.enemyShipFormation.isEmpty() || !state.teamAlive())
                    && !this.levelFinished) {
                // The object managed by the object pool pattern
                // must be recycled at the end of the level.
                BulletPool.recycle(this.bullets);
                this.bullets.removeAll(this.bullets);
                ItemPool.recycle(items);
                this.items.removeAll(this.items);

                this.levelFinished = true;
                this.screenFinishedCooldown.reset();

                if (enemyShipFormation.getShipCount() == 0
                        && state.getBulletsShot() > 0
                        && state.getBulletsShot()
                        == state.getShipsDestroyed()) {
                    achievementManager.unlock("Perfect Shooter");
                }
                if (enemyShipFormation.getShipCount() == 0
                        && !this.tookDamageThisLevel) {
                    achievementManager.unlock("Survivor");
                }
                if (enemyShipFormation.getShipCount() == 0
                        & state.getLevel() == 5) {
                    achievementManager.unlock("Clear");
                }
                checkAchievement();
            }

            if (this.levelFinished
                    && this.screenFinishedCooldown.checkFinished()) {
                if (!achievementManager.hasPendingToasts()) {
                    this.isRunning = false;
                }
            }
        }

        if (this.achievementManager != null)
            this.achievementManager.update();

        // pause 상태에서도 다시 그려줘야 함
        if (this.isPaused) {
            draw();
        }
    }

    /**
     * Draws the elements associated with the screen.
     */
    private void draw() {
        drawManager.initDrawing(this);

        drawManager.drawExplosions();
        drawManager.updateGameSpace();

        for (Ship s : this.ships)
            if (s != null)
                drawManager.drawEntity(s, s.getPositionX(), s.getPositionY());

        if (this.enemyShipSpecial != null)
            drawManager.drawEntity(this.enemyShipSpecial,
                    this.enemyShipSpecial.getPositionX(),
                    this.enemyShipSpecial.getPositionY());

        enemyShipFormation.draw();

        for (Bullet bullet : this.bullets)
            drawManager.drawEntity(bullet, bullet.getPositionX(),
                    bullet.getPositionY());

        // draw items
        for (Item item : this.items)
            drawManager.drawEntity(item, item.getPositionX(),
                    item.getPositionY());

        // Aggregate UI (team score & team lives)
        drawManager.drawScore(this, state.getScore());
        drawManager.drawLives(this, state.getLivesRemaining(), state.isCoop());
        drawManager.drawCoins(this, state.getCoins());

        drawManager.drawLevel(this, this.state.getLevel());
        drawManager.drawHorizontalLine(this, SEPARATION_LINE_HEIGHT - 1);
        drawManager.drawShipCount(this, enemyShipFormation.getShipCount());
        drawInventory(40, SEPARATION_LINE_HEIGHT - 40);

        if (!this.inputDelay.checkFinished()) {
            int countdown = (int) ((INPUT_DELAY
                    - (System.currentTimeMillis() - this.gameStartTime)) / 1000);
            drawManager.drawCountDown(this, this.state.getLevel(),
                    countdown, this.bonusLife);
            drawManager.drawHorizontalLine(this,
                    this.height / 2 - this.height / 12);
            drawManager.drawHorizontalLine(this,
                    this.height / 2 + this.height / 12);
        }

        if (this.highScoreNotified &&
                System.currentTimeMillis() - this.highScoreNoticeStartTime
                        < HIGH_SCORE_NOTICE_DURATION) {
            drawManager.drawNewHighScoreNotice(this);
        }

        // draw achievement toasts
        drawManager.drawAchievementToasts(
                this,
                (this.achievementManager != null)
                        ? this.achievementManager.getActiveToasts()
                        : java.util.Collections.emptyList()
        );

        if (this.isPaused) {
            drawManager.drawPauseOverlay(this);
        }

        // --- Revive UI (공통 헬퍼 사용) ---
        drawReviveUiIfNeeded(drawManager);
        // -------------------

        drawManager.completeDrawing(this);
    }

    /**
     * Cleans bullets that go off screen.
     */
    private void cleanBullets() {
        cleanBulletsCommon(this.bullets, SEPARATION_LINE_HEIGHT);
    }

    private void cleanItems() {
        cleanItemsCommon(this.items);
    }


    /**
     * Manages pickups between player and items.
     */
    private void manageItemPickups() {
        manageItemPickups(this.items, this.ships, this.state);
    }

    /**
     * Enemy bullets hit players → decrement TEAM lives; player bullets hit enemies
     * → add score.
     */
    private void manageCollisions() {
        Set<Bullet> recyclable = new HashSet<>();
        for (Bullet bullet : this.bullets) {
            if (bullet.getSpeed() > 0) {
                // Enemy bullet vs both players
                for (int p = 0; p < GameState.NUM_PLAYERS; p++) {
                    Ship ship = this.ships[p];
                    if (ship != null && !ship.isDestroyed()
                            && checkCollision(bullet, ship)
                            && !this.levelFinished) {
                        recyclable.add(bullet);

                        drawManager.triggerExplosion(
                                ship.getPositionX(), ship.getPositionY(),
                                false,
                                state.getLivesRemaining() == 1);
                        ship.addHit();

                        ship.destroy();
                        SoundManager.playOnce("sound/explosion.wav");
                        state.decLife(p);

                        // Record damage for Survivor achievement check
                        this.tookDamageThisLevel = true;

                        drawManager.setLastLife(
                                state.getLivesRemaining() == 1);
                        drawManager.setDeath(
                                state.getLivesRemaining() == 0);

                        this.logger.info("Hit on player " + (p + 1)
                                + ", team lives now: "
                                + state.getLivesRemaining());
                        break;
                    }
                }

                // --- Revive Trigger ---
                if (state.getLivesRemaining() == 0) {
                    this.revivePhase = RevivePhase.REVIVE_PROMPT;
                    return;
                }
                // ------------------------------

            } else {
                // Player bullet vs enemies
                final int ownerId = bullet.getOwnerPlayerId(); // 1 or 2 (0 if unset)
                final int pIdx = (ownerId == 2) ? 1 : 0; // default to P1 when unset

                boolean finalShip = this.enemyShipFormation.lastShip();

                // Check collision with formation enemies
                for (EnemyShip enemyShip : this.enemyShipFormation) {
                    if (!enemyShip.isDestroyed()
                            && checkCollision(bullet, enemyShip)) {
                        recyclable.add(bullet);
                        enemyShip.hit();

                        if (enemyShip.isDestroyed()) {
                            int points = enemyShip.getPointValue();
                            state.addCoins(pIdx,
                                    enemyShip.getCoinValue());

                            drawManager.triggerExplosion(
                                    enemyShip.getPositionX(),
                                    enemyShip.getPositionY(),
                                    true, finalShip);
                            state.addScore(pIdx, points);
                            state.incShipsDestroyed(pIdx);

                            Item drop =
                                    engine.ItemManager.getInstance()
                                            .obtainDrop(enemyShip);
                            if (drop != null) {
                                this.items.add(drop);
                                this.logger.info(
                                        "Spawned " + drop.getType()
                                                + " at "
                                                + drop.getPositionX()
                                                + ","
                                                + drop.getPositionY());
                            }

                            this.enemyShipFormation.destroy(enemyShip);
                            SoundManager.playOnce(
                                    "sound/invaderkilled.wav");
                            this.logger.info("Hit on enemy ship.");

                            checkAchievement();
                        }
                        break;
                    }
                }

                if (this.enemyShipSpecial != null
                        && !this.enemyShipSpecial.isDestroyed()
                        && checkCollision(bullet,
                        this.enemyShipSpecial)) {
                    int points = this.enemyShipSpecial.getPointValue();

                    state.addCoins(pIdx,
                            this.enemyShipSpecial.getCoinValue());
                    state.addScore(pIdx, points);
                    state.incShipsDestroyed(pIdx);

                    this.enemyShipSpecial.destroy();
                    SoundManager.stop();
                    SoundManager.playOnce("sound/explosion.wav");
                    drawManager.triggerExplosion(
                            this.enemyShipSpecial.getPositionX(),
                            this.enemyShipSpecial.getPositionY(),
                            true, true);
                    this.enemyShipSpecialExplosionCooldown.reset();
                    recyclable.add(bullet);
                }
            }
        }
        this.bullets.removeAll(recyclable);
        BulletPool.recycle(recyclable);
    }

    /**
     * Returns a GameState object representing the status of the game.
     *
     * @return Current game state.
     */
    public final GameState getGameState() {
        return this.state;
    }

    /**
     * check Achievement released;
     */
    public void checkAchievement() {
        AchievementUtil.checkBasicAchievements(state, achievementManager);
        // Clear
        if (levelFinished && this.enemyShipFormation.isEmpty()
                && state.getLevel() == 5) {
            achievementManager.unlock("Clear");
            float p1Acc =
                    state.getBulletsShot(0) > 0
                            ? (float) state.getShipsDestroyed(0)
                            / state.getBulletsShot(0) * 100
                            : 0f;
            float p2Acc =
                    state.getBulletsShot(1) > 0
                            ? (float) state.getShipsDestroyed(1)
                            / state.getBulletsShot(1) * 100
                            : 0f;
            // Survivor
            if (!this.tookDamageThisLevel) {
                achievementManager.unlock("Survivor");
            }
            // Sharpshooter
            if (p1Acc >= 80) {
                //1p
                achievementManager.unlock("Sharpshooter");
                //coop
                if (p2Acc >= 80) {
                    achievementManager.unlock("Sharpshooter");
                }
            }
        }
    }

    // ------------------------------------------------------------------
    // ReviveScreen 콜백 구현부
    // ------------------------------------------------------------------

    @Override
    protected void onReviveSuccess() {
        // 원래 respawnPlayer() 내용
        this.levelFinished = false;
        this.screenFinishedCooldown.reset();
        this.revivePhase = RevivePhase.PLAYING;
    }

    @Override
    protected void onReviveRejected() {
        // 거절 시 점수 화면(2)으로 이동
        this.returnCode = 2;
        this.isRunning = false;
    }

    @Override
    protected void onReviveResultAcknowledged() {
        // 실패 메시지 확인 후에도 점수 화면(2)으로 이동
        this.returnCode = 2;
        this.isRunning = false;
    }
}
