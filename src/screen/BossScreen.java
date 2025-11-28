package screen;

import java.awt.event.KeyEvent;
import java.util.HashSet;
import java.util.Set;
import java.util.Collections;
import java.util.logging.Logger;
import java.util.function.IntSupplier;

import engine.Cooldown;
import engine.Core;
import engine.GameSettings;
import engine.GameState;
import engine.AchievementManager;
import engine.SoundManager;
import engine.BossTimer;
import engine.*;
import entity.*;
import entity.BulletEmitter;

/**
 * Implements the boss screen, where the boss fight happens.
 * Based on GameScreen.java.
 */
public class BossScreen extends ReviveScreen {

    /** Milliseconds until the screen accepts user input. */
    private static final int INPUT_DELAY = 6000;
    /** Bonus score for each life remaining at the end of the level. */
    private static final int LIFE_SCORE = 100;
    /** Time from finishing the level to screen change. */
    private static final int SCREEN_CHANGE_INTERVAL = 1500;
    /** Height of the interface separation line. */
    private static final int SEPARATION_LINE_HEIGHT = 68;

    /** Pause / 메뉴 관련 쿨다운 상수 */
    private static final int PAUSE_COOLDOWN_MS = 300;
    private static final int RETURN_MENU_COOLDOWN_MS = 300;

    /** 카운트다운 사운드 발생 지점 (ms) */
    private static final int COUNTDOWN_BEEP_TIME_MS = 1750;

    /** 사운드 경로 상수 (중복 리터럴 제거) */
    private static final String SOUND_BGM       = "sound/SpaceInvader-GameTheme.wav";
    private static final String SOUND_EXPLOSION = "sound/explosion.wav";
    private static final String SOUND_SHOOT     = "sound/shoot.wav";
    private static final String SOUND_SELECT    = "sound/select.wav";
    private static final String SOUND_COUNTDOWN = "sound/CountDownSound.wav";

    /** For Check Achievement */
    private final AchievementManager achievementManager;

    /** Boss Entity. */
    private Boss boss;
    /** Formation of boss's minions. */
    private EnemyShipFormation minionFormation;

    /** Formation of player ships. */
    private final Ship[] ships = new Ship[GameState.NUM_PLAYERS];
    /** Time from finishing the level to screen change. */
    private Cooldown screenFinishedCooldown;
    /** Set of all bullets fired by on screen ships. */
    private Set<Bullet> bullets;
    /** Set of all items spawned. */
    private Set<Item> items;
    /** Time of game start. */
    private long gameStartTime;
    /** Checks if the level is finished. */
    private boolean levelFinished;
    /** Logger instance. */
    private static final Logger bossScreenLogger = Core.getLogger();

    /** Checks if the game is paused. */
    private boolean isPaused;
    /** Cooldown for pausing. */
    private Cooldown pauseCooldown;
    /** Cooldown for returning to menu. */
    private Cooldown returnMenuCooldown;

    /** checks if player took damage */
    private boolean tookDamageThisLevel;
    /** checks if countdown sound has played */
    private boolean countdownSoundPlayed = false;

    /** Player ship types. */
    private final Ship.ShipType shipTypeP1;
    private final Ship.ShipType shipTypeP2;

    /** (Trigger 1) Message for attacking invulnerable boss. */
    private static final String MSG_MINIONS_FIRST = "Let's defeat the minions first!";
    /** Cooldown for the invulnerable message. */
    private Cooldown invulnerableMsgCooldown;
    /** Duration for the invulnerable message. */
    private static final int INVULNERABLE_MSG_DURATION = 1000; // 1 second

    /** (Trigger 2) Message for phase 2 start. */
    private static final String MSG_PHASE_2 = "Phase 2 Started!";
    /** Cooldown for the phase 2 message. */
    private Cooldown phase2MsgCooldown;
    /** Duration for the phase 2 message. */
    private static final int PHASE_2_MSG_DURATION = 2000; // 2 seconds
    /** Counter for the invulnerable message. */
    private int invulnerableMsgCount;
    /** Maximum times the invulnerable message can be shown. */
    private static final int MAX_INVULNERABLE_MSG_SHOWS = 3;

    /** Boss Timer */
    private BossTimer bossTimer;
    private boolean isTimerStarted;

    /**
     * Constructor, establishes the properties of the screen.
     *
     * @param gameState          Current game state.
     * @param width              Screen width.
     * @param height             Screen height.
     * @param fps                Frames per second.
     * @param shipTypeP1         Player 1's ship type.
     * @param shipTypeP2         Player 2's ship type.
     * @param achievementManager Achievement manager instance.
     */
    public BossScreen(final GameState gameState,
                      final int width, final int height, final int fps,
                      final Ship.ShipType shipTypeP1,
                      final Ship.ShipType shipTypeP2,
                      final AchievementManager achievementManager) {

        super(gameState, width, height, fps);

        this.achievementManager = achievementManager;
        this.shipTypeP1 = shipTypeP1;
        this.shipTypeP2 = shipTypeP2;
        this.tookDamageThisLevel = false;

        // Initialize BossTimer
        this.bossTimer = new BossTimer(System::currentTimeMillis);
        this.isTimerStarted = false;
    }

    /**
     * Initializes basic screen properties, and adds necessary elements.
     */
    @Override
    public final void initialize() {
        super.initialize();
        if (this.inventory != null) {
            this.inventory.clear();
        } else {
            this.inventory = new ItemInventory(this.state, 0);
        }

        state.clearAllEffects();

        // Start background music
        SoundManager.startBackgroundMusic(SOUND_BGM);

        // 1. Create player ships
        this.ships[0] = new Ship(this.width / 2 - 60, this.height - 30,
                Entity.Team.PLAYER1, shipTypeP1, this.state);
        this.ships[0].setPlayerId(1);
        if (state.isCoop()) {
            this.ships[1] = new Ship(this.width / 2 + 60, this.height - 30,
                    Entity.Team.PLAYER2, shipTypeP2, this.state);
            this.ships[1].setPlayerId(2);
        } else {
            this.ships[1] = null;
        }

        // Define Boss Y position and minion start Y
        int bossY = SEPARATION_LINE_HEIGHT + 40; // Below UI and HP Bar
        int bossHeight = 30 * 2; // From Boss.java
        int minionStartY = bossY + bossHeight + 20; // 20px padding below boss
        int initMinionY = 100; // Default INIT_POS_Y in EnemyShipFormation
        final int yOffset = minionStartY - initMinionY;

        // 2. Define Boss Callbacks
        Runnable spawnHP1Group = () -> {
            bossScreenLogger.info("Boss spawning Phase 1 minions (5x2).");
            GameSettings minionSettings = new GameSettings(5, 2, 100, 2000); // 5x2
            this.minionFormation = new EnemyShipFormation(minionSettings);
            this.minionFormation.attach(this);
            // Move minions below boss
            for (EnemyShip minion : this.minionFormation) {
                minion.move(0, yOffset);
            }
        };

        Runnable spawnHP2Group = () -> {
            bossScreenLogger.info("Boss spawning Phase 2 minions (5x3).");
            GameSettings minionSettings = new GameSettings(5, 3, 90, 1500); // 5x3
            this.minionFormation = new EnemyShipFormation(minionSettings);
            this.minionFormation.attach(this);
            // Move minions below boss
            for (EnemyShip minion : this.minionFormation) {
                minion.move(0, yOffset);
            }
        };

        Runnable clearShield = () -> {
            bossScreenLogger.info("Boss clearing phase 1 minions.");
            if (this.minionFormation != null) {
                for (EnemyShip minion : this.minionFormation) {
                    minion.destroy();
                }
            }
        };

        Runnable onPhase2StartCallback = () -> {
            bossScreenLogger.info("Boss entering Phase 2! Triggering message.");
            this.phase2MsgCooldown.reset();
        };

        IntSupplier minionAlive = () ->
                (this.minionFormation != null)
                        ? this.minionFormation.getShipCount()
                        : 0;

        BulletEmitter emitter = (x, y, vx, vy) -> {
            Bullet bullet =
                    BulletPool.getBullet(x, y, vy, 3 * 2, 5 * 2,
                            Entity.Team.ENEMY);
            bullet.setSpeedX(vx);
            this.bullets.add(bullet);
        };

        // 3. Create Boss
        int bossX = (this.width / 2) - (50 * 2 / 2); // Boss.java BOSS_WIDTH=50
        this.boss = new Boss(bossX, bossY, this.width,
                emitter, minionAlive, spawnHP1Group,
                spawnHP2Group, clearShield, onPhase2StartCallback);

        // inventory
        this.inventory = new ItemInventory(this.state, 0);

        // 4. Cooldowns and Sets
        this.screenFinishedCooldown =
                Core.getCooldown(SCREEN_CHANGE_INTERVAL);
        this.bullets = new HashSet<>();
        this.items = new HashSet<>();
        this.invulnerableMsgCooldown =
                Core.getCooldown(INVULNERABLE_MSG_DURATION);
        this.phase2MsgCooldown = Core.getCooldown(PHASE_2_MSG_DURATION);

        this.invulnerableMsgCount = 0;

        // Special input delay / countdown.
        this.gameStartTime = System.currentTimeMillis();
        this.inputDelay = Core.getCooldown(INPUT_DELAY);
        this.inputDelay.reset();
        drawManager.setDeath(false);

        this.isPaused = false;
        this.pauseCooldown = Core.getCooldown(PAUSE_COOLDOWN_MS);
        this.returnMenuCooldown =
                Core.getCooldown(RETURN_MENU_COOLDOWN_MS);

        // Revive 상태 초기화
        initReviveState();
    }

    /**
     * Starts the action.
     *
     * @return Next screen code.
     */
    @Override
    public final int run() {
        super.run();

        // 2P mode: award bonus score for remaining TEAM lives
        state.addScore(0, LIFE_SCORE * state.getLivesRemaining());

        // Stop all music on exiting this screen
        SoundManager.stopAllMusic();

        bossScreenLogger.info("Boss Screen cleared with a score of "
                + state.getScore());
        return this.returnCode;
    }

    /**
     * Updates the elements on screen and checks for events.
     * (Refactored to reduce Cognitive Complexity)
     */
    @Override
    protected final void update() {
        super.update();

        // ----------------------------------------
        // Revive Phase Handler
        // ----------------------------------------
        switch (this.revivePhase) {
            case REVIVE_PROMPT:
                handleRevivePromptInput(this.inputManager);
                draw();
                return;

            case REVIVE_RESULT:
                handleReviveResultInput(this.inputManager);
                draw();
                return;

            case EXITING:
                this.isRunning = false;
                return;

            case PLAYING:
            default:
                break;
        }

        handleCountdownSound();
        handlePauseAndMenuInput();

        if (!this.isPaused) {
            processGameLogic();
        }

        // Draw final frame
        draw();
    }

    /**
     * 일시정지가 아닐 때 게임의 주요 로직을 처리합니다.
     */
    private void processGameLogic() {
        // 1. 게임 상태(타이머, 플레이어, 적) 업데이트
        updateGameState();

        handleCollisionsAndCleanup();

        // 충돌 중 revive가 걸렸으면 더 진행하지 않음
        if (this.revivePhase != RevivePhase.PLAYING) {
            return;
        }

        checkInGameAchievements();

        // 2. 게임 오버(패배) 조건 체크 및 처리
        checkAndHandleGameOver();

        handleEndOfLevel();
        updateAchievements();
    }

    /**
     * 타이머, 플레이어, 보스/쫄몹의 움직임을 업데이트합니다.
     */
    private void updateGameState() {
        if (this.inputDelay.checkFinished() && !this.levelFinished) {
            if (!isTimerStarted) {
                this.bossTimer.start(this.state.getLevel());
                isTimerStarted = true;
            }
            handlePlayerInputAndShooting();
            updateShips();
            updateBossAndMinions();
        }
    }

    /**
     * 플레이어 팀 전멸 시 게임 오버 시퀀스를 처리합니다.
     */
    private void checkAndHandleGameOver() {
        if (!state.teamAlive() && !this.levelFinished) {
            bossScreenLogger.info(
                    "Player team is defeated on Boss Screen.");

            // 게임 오버 시퀀스 트리거
            this.levelFinished = true;
            this.screenFinishedCooldown.reset();

            // 타이머 정지
            this.bossTimer.stop();

            // 음악 멈추고 패배 사운드 재생
            SoundManager.stopAllMusic();
            SoundManager.playOnce("sound/lose.wav");

            // 리소스 정리
            cleanupEntitiesOnLose();
        }
    }

    /**
     * 패배 시 엔티티(총알, 아이템, 적)를 정리합니다.
     */
    private void cleanupEntitiesOnLose() {
        BulletPool.recycle(this.bullets);
        this.bullets.clear();

        ItemPool.recycle(items);
        this.items.clear();

        if (this.boss != null) {
            this.boss = null;
        }

        if (this.minionFormation != null) {
            for (EnemyShip minion : this.minionFormation) {
                minion.destroy();
            }
        }
    }

    /** 카운트다운 사운드 처리 */
    private void handleCountdownSound() {
        if (!this.inputDelay.checkFinished() && !countdownSoundPlayed) {
            long elapsed = System.currentTimeMillis() - this.gameStartTime;
            if (elapsed > COUNTDOWN_BEEP_TIME_MS) {
                SoundManager.playOnce(SOUND_COUNTDOWN);
                countdownSoundPlayed = true;
            }
        }
    }

    /** 일시정지 / 메뉴 복귀 입력 처리 */
    private void handlePauseAndMenuInput() {
        // Pause toggle
        if (this.inputDelay.checkFinished()
                && inputManager.isKeyDown(KeyEvent.VK_ESCAPE)
                && this.pauseCooldown.checkFinished()) {

            this.isPaused = !this.isPaused;
            this.pauseCooldown.reset();

            if (this.isPaused) {
                SoundManager.stopBackgroundMusic();
            } else {
                SoundManager.startBackgroundMusic(SOUND_BGM);
            }
        }

        // Return to menu while paused
        if (this.isPaused
                && inputManager.isKeyDown(KeyEvent.VK_BACK_SPACE)
                && this.returnMenuCooldown.checkFinished()) {

            SoundManager.playOnce(SOUND_SELECT);
            SoundManager.stopAllMusic();
            returnCode = 1;
            this.isRunning = false;
        }
    }

    /**
     * 플레이어 입력 및 발사 처리
     */
    private void handlePlayerInputAndShooting() {
        for (int p = 0; p < GameState.NUM_PLAYERS; p++) {
            handleSingleShipInput(p, this.ships, this.bullets, this.state);
        }
    }


    /**
     * 개별 선박의 입력, 이동, 사격을 처리합니다.
     */


    /** 플레이어 ship 상태 업데이트 */
    private void updateShips() {
        for (Ship s : this.ships) {
            if (s != null) {
                s.update();
            }
        }
    }

    /** 보스 및 쫄몹 로직 업데이트 */
    private void updateBossAndMinions() {
        if (this.boss != null && this.boss.getHp() > 0) {
            this.boss.update();
        }

        if (this.minionFormation != null) {
            this.minionFormation.update();
            this.minionFormation.shoot(this.bullets);
        }
    }

    /** 충돌, 총알/아이템 정리, 이펙트 업데이트 */
    private void handleCollisionsAndCleanup() {
        manageCollisions();
        cleanBullets();
        cleanItems();
        if (this.inventory != null) {
            this.inventory.update();
        }
        manageItemPickups();

        state.updateEffects();
        drawManager.setLastLife(state.getLivesRemaining() == 1);
    }

    /** 보스 사망 및 레벨 종료 처리 */
    private void handleEndOfLevel() {
        if (this.boss != null && this.boss.getHp() <= 0
                && !this.levelFinished) {
            bossScreenLogger.info("Boss defeated!");

            this.bossTimer.stop();
            state.setBossClearTime(this.bossTimer.getDuration());

            BulletPool.recycle(this.bullets);
            this.bullets.clear();
            ItemPool.recycle(items);
            this.items.clear();

            if (this.minionFormation != null) {
                for (EnemyShip minion : this.minionFormation) {
                    minion.destroy();
                }
            }

            this.levelFinished = true;
            this.screenFinishedCooldown.reset();

            grantBossVictoryAchievements();
        }

        if (this.levelFinished
                && this.screenFinishedCooldown.checkFinished()
                && !achievementManager.hasPendingToasts()) {
            this.isRunning = false;
        }
    }

    /**
     * 보스 처치 시 승리 업적을 계산하고 부여합니다.
     */
    private void grantBossVictoryAchievements() {
        if (!this.tookDamageThisLevel) {
            achievementManager.unlock("Survivor");
        }

        int totalHitsLanded =
                state.getShipsDestroyed() + this.boss.getMaxHp();
        int totalBulletsShot = state.getBulletsShot();
        double accuracy = 0.0;

        if (totalBulletsShot > 0) {
            accuracy = (double) totalHitsLanded
                    / (double) totalBulletsShot;
        }

        if (accuracy >= 0.8) {
            achievementManager.unlock("Sharpshooter");
        }

        if (totalBulletsShot > 0
                && totalBulletsShot == totalHitsLanded) {
            achievementManager.unlock("Perfect Shooter");
        }
    }

    /** 업적 팝업 업데이트 */
    private void updateAchievements() {
        if (this.achievementManager != null) {
            this.achievementManager.update();
        }
    }

    /**
     * Draws the elements associated with the screen.
     */
    private void draw() {
        drawManager.initDrawing(this);

        drawManager.drawExplosions();
        drawManager.updateGameSpace(); // Background stars

        for (Ship s : this.ships) {
            if (s != null) {
                drawManager.drawEntity(s,
                        s.getPositionX(), s.getPositionY());
            }
        }

        if (this.boss != null && this.boss.getHp() > 0) {
            drawManager.drawEntity(this.boss,
                    this.boss.getPositionX(),
                    this.boss.getPositionY());
        }

        if (this.minionFormation != null) {
            this.minionFormation.draw();
        }

        for (Bullet bullet : this.bullets) {
            drawManager.drawEntity(bullet,
                    bullet.getPositionX(),
                    bullet.getPositionY());
        }

        for (Item item : this.items) {
            drawManager.drawEntity(item,
                    item.getPositionX(),
                    item.getPositionY());
        }

        drawManager.drawBossTimer(this, this.bossTimer.getDuration());
        drawManager.drawLives(this, state.getLivesRemaining(),
                state.isCoop());
        drawManager.drawCoins(this, state.getCoins());
        drawManager.drawLevel(this, this.state.getLevel());
        drawManager.drawHorizontalLine(this,
                SEPARATION_LINE_HEIGHT - 1);
        if (this.inventory != null && this.inputDelay.checkFinished()) {
            drawManager.drawItemInventory(
                    this, this.inventory,
                    40, SEPARATION_LINE_HEIGHT - 40);
        }

        if (this.boss != null) {
            drawManager.drawBossHPBar(this,
                    this.boss.getHp(), this.boss.getMaxHp());
        }

        drawManager.drawBossTimer(this, this.bossTimer.getDuration());

        if (this.minionFormation != null) {
            drawManager.drawShipCount(this,
                    this.minionFormation.getShipCount());
        }

        if (!this.inputDelay.checkFinished()) {
            int countdown = (int) ((INPUT_DELAY
                    - (System.currentTimeMillis() - this.gameStartTime))
                    / 1000);
            drawManager.drawCountDown(this,
                    this.state.getLevel(),
                    countdown, false);
            drawManager.drawHorizontalLine(this,
                    this.height / 2 - this.height / 12);
            drawManager.drawHorizontalLine(this,
                    this.height / 2 + this.height / 12);
        }

        drawManager.drawAchievementToasts(
                this,
                (this.achievementManager != null)
                        ? this.achievementManager.getActiveToasts()
                        : Collections.emptyList()
        );

        if (this.isPaused) {
            drawManager.drawPauseOverlay(this);
        }

        // Revive UI 공통 헬퍼
        drawReviveUiIfNeeded(drawManager);

        drawMessages();

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
     * 총알과 엔티티 간의 충돌을 관리합니다.
     */
    private void manageCollisions() {
        Set<Bullet> recyclable = new HashSet<>();
        for (Bullet bullet : this.bullets) {
            if (bullet.getSpeed() > 0) {
                if (handleEnemyBulletCollision(bullet)) {
                    recyclable.add(bullet);
                }
            } else {
                if (handlePlayerBulletCollision(bullet)) {
                    recyclable.add(bullet);
                }
            }
        }
        this.bullets.removeAll(recyclable);
        BulletPool.recycle(recyclable);
    }

    /**
     * 적 총알이 플레이어에게 맞았는지 확인하고 처리합니다.
     */
    private boolean handleEnemyBulletCollision(Bullet bullet) {
        for (int p = 0; p < GameState.NUM_PLAYERS; p++) {
            Ship ship = this.ships[p];
            if (ship != null && !ship.isDestroyed()
                    && checkCollision(bullet, ship)
                    && !this.levelFinished) {

                drawManager.triggerExplosion(
                        ship.getPositionX(), ship.getPositionY(),
                        false, state.getLivesRemaining() == 1);
                ship.addHit();
                ship.destroy();
                SoundManager.playOnce(SOUND_EXPLOSION);
                state.decLife(p);
                this.tookDamageThisLevel = true;
                drawManager.setLastLife(state.getLivesRemaining() == 1);
                drawManager.setDeath(state.getLivesRemaining() == 0);
                bossScreenLogger.info("Hit on player " + (p + 1));

                // --- Revive Trigger ---
                if (state.getLivesRemaining() == 0) {
                    this.revivePhase = RevivePhase.REVIVE_PROMPT;
                }
                // ------------------------------

                return true;
            }
        }
        return false;
    }

    /**
     * 플레이어 총알이 쫄몹이나 보스에게 맞았는지 확인하고 처리합니다.
     */
    private boolean handlePlayerBulletCollision(Bullet bullet) {
        final int ownerId = bullet.getOwnerPlayerId();
        final int pIdx = (ownerId == 2) ? 1 : 0;

        if (handleMinionCollision(bullet, pIdx)) {
            return true;
        }

        return handleBossCollision(bullet);
    }

    /**
     * 플레이어 총알이 쫄몹에게 맞았는지 확인합니다.
     */
    private boolean handleMinionCollision(Bullet bullet, int pIdx) {
        if (this.minionFormation != null) {
            for (EnemyShip enemyShip : this.minionFormation) {
                if (!enemyShip.isDestroyed()
                        && checkCollision(bullet, enemyShip)) {
                    enemyShip.hit();

                    if (enemyShip.isDestroyed()) {
                        int points = enemyShip.getPointValue();
                        state.addCoins(pIdx, enemyShip.getCoinValue());
                        drawManager.triggerExplosion(
                                enemyShip.getPositionX(),
                                enemyShip.getPositionY(),
                                true, false);
                        state.addScore(pIdx, points);
                        state.incShipsDestroyed(pIdx);

                        Item drop =
                                engine.ItemManager.getInstance()
                                        .obtainDrop(enemyShip);
                        if (drop != null) {
                            this.items.add(drop);
                        }

                        this.minionFormation.destroy(enemyShip);
                        SoundManager.playOnce("sound/invaderkilled.wav");
                    }
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 플레이어 총알이 보스에게 맞았는지 확인합니다.
     */
    private boolean handleBossCollision(Bullet bullet) {
        if (this.boss != null && this.boss.getHp() > 0
                && checkCollision(bullet, this.boss)) {
            if (this.boss.isInvulnerable()) {
                if (this.invulnerableMsgCount
                        < MAX_INVULNERABLE_MSG_SHOWS) {
                    this.invulnerableMsgCooldown.reset();
                    this.invulnerableMsgCount++;
                }
            } else {
                this.boss.onHit(1);
            }

            if (this.boss.getHp() <= 0) {
                drawManager.triggerExplosion(
                        this.boss.getPositionX()
                                + this.boss.getWidth() / 2,
                        this.boss.getPositionY()
                                + this.boss.getHeight() / 2,
                        true, true
                );
                SoundManager.stop();
                SoundManager.playOnce(SOUND_EXPLOSION);
            }
            return true;
        }
        return false;
    }

    /**
     * Returns a GameState object representing the status of the game.
     *
     * @return Current game state.
     */
    public final GameState getGameState() {
        return this.state;
    }

    private void drawMessages() {
        int x = 10;
        int y = this.height - 20;

        if (this.phase2MsgCooldown != null
                && !this.phase2MsgCooldown.checkFinished()) {
            drawManager.drawString(MSG_PHASE_2,
                    x, y, java.awt.Color.YELLOW);
        } else if (this.invulnerableMsgCooldown != null
                && !this.invulnerableMsgCooldown.checkFinished()) {
            drawManager.drawString(MSG_MINIONS_FIRST,
                    x, y, java.awt.Color.WHITE);
        }
    }

    /**
     * Checks for in-game achievements (ported from GameScreen).
     * These achievements can be unlocked at any time during gameplay.
     */
    public void checkInGameAchievements() {
        if (state.getShipsDestroyed() == 1) {
            achievementManager.unlock("First Blood");
        }

        if (state.getBulletsShot() >= 50) {
            achievementManager.unlock("50 Bullets");
        }

        if (state.getScore() >= 3000) {
            achievementManager.unlock("Get 3000 Score");
        }
    }

    // ------------------------------------------------------------------
    // ReviveScreen 콜백 구현부
    // ------------------------------------------------------------------

    @Override
    protected void onReviveSuccess() {
        this.levelFinished = false;
        this.screenFinishedCooldown.reset();
        this.revivePhase = RevivePhase.PLAYING;
    }

    @Override
    protected void onReviveRejected() {
        this.returnCode = 2;
        this.isRunning = false;
    }

    @Override
    protected void onReviveResultAcknowledged() {
        this.returnCode = 2;
        this.isRunning = false;
    }
}
