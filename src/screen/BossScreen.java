
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
import entity.BulletEmitter;
import engine.*;
import entity.BulletEmitter;
import entity.*;

/**
 * Implements the boss screen, where the boss fight happens.
 * Based on GameScreen.java.
 */
public class BossScreen extends Screen {

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
    private static final String SOUND_BGM          = "sound/SpaceInvader-GameTheme.wav";
    private static final String SOUND_EXPLOSION    = "sound/explosion.wav";
    private static final String SOUND_SHOOT        = "sound/shoot.wav";
    private static final String SOUND_SELECT       = "sound/select.wav";
    private static final String SOUND_HOVER        = "sound/hover.wav";
    private static final String SOUND_COUNTDOWN    = "sound/CountDownSound.wav";

    /** For Check Achievement */
    private final AchievementManager achievementManager;
    /** Current game state. */
    private final GameState state;

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

    /** Revive Manager */
    private ReviveManager reviveManager;

    private enum RevivePhase {
        PLAYING,
        REVIVE_PROMPT,
        REVIVE_RESULT,
        EXITING
    }
    private RevivePhase revivePhase = RevivePhase.PLAYING;
    private int reviveSelection = 0; // 0 = YES, 1 = NO
    private String reviveFailMessage = "";


    /**
     * Constructor, establishes the properties of the screen.
     *
     * @param gameState
     * Current game state.
     * @param width
     * Screen width.
     * @param height
     * Screen height.
     * @param fps
     * Frames per second, frame rate at which the game is run.
     * @param shipTypeP1
     * Player 1's ship type.
     * @param shipTypeP2
     * Player 2's ship type.
     * @param achievementManager
     * Achievement manager instance.
     */
    public BossScreen(final GameState gameState,
                      final int width, final int height, final int fps,
                      final Ship.ShipType shipTypeP1, final Ship.ShipType shipTypeP2,
                      final AchievementManager achievementManager) {
        super(width, height, fps);

        this.state = gameState;
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

        // Start Boss Timer
        // this.bossTimer.start(this.state.getLevel());

        // 1. Create player ships
        this.ships[0] = new Ship(this.width / 2 - 60, this.height - 30, Entity.Team.PLAYER1, shipTypeP1, this.state);
        this.ships[0].setPlayerId(1);
        if (state.isCoop()) {
            this.ships[1] = new Ship(this.width / 2 + 60, this.height - 30, Entity.Team.PLAYER2, shipTypeP2, this.state);
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
                // Let update() handle the visual removal
            }
        };

        Runnable onPhase2StartCallback = () -> {
            bossScreenLogger.info("Boss entering Phase 2! Triggering message.");
            this.phase2MsgCooldown.reset();
        };

        IntSupplier minionAlive = () ->
                (this.minionFormation != null) ? this.minionFormation.getShipCount() : 0;

        BulletEmitter emitter = (x, y, vx, vy) -> {
            // Boss.java's vy is just a concept, use given vy or a fixed speed for enemy bullets.
            Bullet bullet = BulletPool.getBullet(x, y, vy, 3 * 2, 5 * 2, Entity.Team.ENEMY);
            bullet.setSpeedX(vx);
            this.bullets.add(bullet);
        };

        // 3. Create Boss
        int bossX = (this.width / 2) - (50 * 2 / 2); // Boss.java BOSS_WIDTH=50
        this.boss = new Boss(bossX, bossY, this.width,
                emitter, minionAlive, spawnHP1Group,
                spawnHP2Group, clearShield, onPhase2StartCallback);

        //add inventory
        this.inventory = new ItemInventory(this.state, 0);

        // 4. Cooldowns and Sets
        this.screenFinishedCooldown = Core.getCooldown(SCREEN_CHANGE_INTERVAL);
        this.bullets = new HashSet<>();
        this.items = new HashSet<>();
        this.invulnerableMsgCooldown = Core.getCooldown(INVULNERABLE_MSG_DURATION);
        this.phase2MsgCooldown = Core.getCooldown(PHASE_2_MSG_DURATION);

        this.invulnerableMsgCount = 0;

        // Special input delay / countdown.
        this.gameStartTime = System.currentTimeMillis();
        this.inputDelay = Core.getCooldown(INPUT_DELAY);
        this.inputDelay.reset();
        drawManager.setDeath(false);

        this.isPaused = false;
        this.pauseCooldown = Core.getCooldown(PAUSE_COOLDOWN_MS);
        this.returnMenuCooldown = Core.getCooldown(RETURN_MENU_COOLDOWN_MS);

        // Initialize ReviveManager
        this.reviveManager = new ReviveManager(this.state);
        this.revivePhase = RevivePhase.PLAYING;
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

        bossScreenLogger.info("Boss Screen cleared with a score of " + state.getScore());
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
                handleRevivePrompt();  // UI 띄우기 + 입력받기
                return; // 게임 업데이트 중단

            case REVIVE_RESULT:
                handleReviveFailed();  // 실패 메시지 UI
                return;

            case EXITING:
                this.isRunning = false;
                return;
        }

        handleCountdownSound();
        handlePauseAndMenuInput();

        if (!this.isPaused) {
            // 복잡한 로직을 processGameLogic 함수로 위임하여 복잡도 감소
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

        // Check if collision triggered revive
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
        // revivePhase가 PLAYING 상태인데 목숨이 없고, 아직 레벨이 안끝났다면 처리
        if (!state.teamAlive() && !this.levelFinished) {
            bossScreenLogger.info("Player team is defeated on Boss Screen.");

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
        // 패배 시 즉시 엔티티 재활용
        BulletPool.recycle(this.bullets);
        this.bullets.clear();

        ItemPool.recycle(items);
        this.items.clear();

        // 보스와 쫄몹 화면에서 제거
        if (this.boss != null) {
            this.boss = null;
        }

        if (this.minionFormation != null) {
            for (EnemyShip minion : this.minionFormation) {
                minion.destroy(); // 폭발 이펙트 없이 파괴
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
     * 플레이어 입력 및 발사 처리 (리팩토링됨)
     * 복잡도를 줄이기 위해 개별 선박 처리는 별도 함수로 위임합니다.
     */
    private void handlePlayerInputAndShooting() {
        for (int p = 0; p < GameState.NUM_PLAYERS; p++) {
            Ship ship = this.ships[p];
            // 배가 없거나 파괴되었으면 스킵
            if (ship != null && !ship.isDestroyed()) {
                handleSingleShipInput(p, ship);
            }
        }
    }

    /**
     * 개별 선박의 입력, 이동, 사격을 처리합니다.
     * (handlePlayerInputAndShooting에서 분리됨)
     */
    private void handleSingleShipInput(int playerIndex, Ship ship) {
        boolean moveRight;
        boolean moveLeft;
        boolean fire;

        // 플레이어 번호에 따른 키 입력 확인
        if (playerIndex == 0) {
            moveRight = inputManager.isP1RightPressed();
            moveLeft = inputManager.isP1LeftPressed();
            fire = inputManager.isP1ShootPressed();
        } else {
            moveRight = inputManager.isP2RightPressed();
            moveLeft = inputManager.isP2LeftPressed();
            fire = inputManager.isP2ShootPressed();
        }

        // 화면 경계 체크
        boolean isRightBorder = ship.getPositionX() + ship.getWidth() + ship.getSpeed() > this.width - 1;
        boolean isLeftBorder = ship.getPositionX() - ship.getSpeed() < 1;

        // 이동 처리
        if (moveRight && !isRightBorder) {
            ship.moveRight();
        }
        if (moveLeft && !isLeftBorder) {
            ship.moveLeft();
        }

        // 발사 처리
        if (fire && ship.shoot(this.bullets)) {
            SoundManager.playOnce(SOUND_SHOOT);
            state.incBulletsShot(playerIndex);
        }
    }

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
            this.boss.update(); // Boss moves, checks invuln, fires
        }

        if (this.minionFormation != null) {
            this.minionFormation.update(); // Minions move
            this.minionFormation.shoot(this.bullets); // Minions shoot
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

    /** * 보스 사망 및 레벨 종료 처리
     * (복잡도 해결: 업적 계산 로직을 별도 함수로 분리함)
     */
    private void handleEndOfLevel() {
        // End Condition: Boss HP <= 0
        if (this.boss != null && this.boss.getHp() <= 0 && !this.levelFinished) {
            bossScreenLogger.info("Boss defeated!");

            // 타이머 정지
            this.bossTimer.stop();
            state.setBossClearTime(this.bossTimer.getDuration());

            // Recycle entities (리소스 정리)
            BulletPool.recycle(this.bullets);
            this.bullets.clear();
            ItemPool.recycle(items);
            this.items.clear();

            // Clear remaining minions (쫄몹 정리)
            if (this.minionFormation != null) {
                for (EnemyShip minion : this.minionFormation) {
                    minion.destroy();
                }
            }

            this.levelFinished = true;
            this.screenFinishedCooldown.reset();

            // [핵심] 업적 부여 로직을 별도 함수로 위임하여 복잡도를 낮춤
            grantBossVictoryAchievements();
        }

        // Screen transition (화면 전환)
        if (this.levelFinished && this.screenFinishedCooldown.checkFinished() && !achievementManager.hasPendingToasts()) {
            this.isRunning = false;
        }
    }

    /**
     * 보스 처치 시 승리 업적을 계산하고 부여합니다.
     * (handleEndOfLevel에서 분리됨)
     */
    private void grantBossVictoryAchievements() {
        // Survivor (한 대도 안 맞음)
        if (!this.tookDamageThisLevel) {
            achievementManager.unlock("Survivor");
        }

        // 명중률 계산
        int totalHitsLanded = state.getShipsDestroyed() + this.boss.getMaxHp();
        int totalBulletsShot = state.getBulletsShot();
        double accuracy = 0.0;

        if (totalBulletsShot > 0) {
            accuracy = (double) totalHitsLanded / (double) totalBulletsShot;
        }

        // Sharpshooter (명중률 80% 이상)
        if (accuracy >= 0.8) {
            achievementManager.unlock("Sharpshooter");
        }

        // Perfect Shooter (명중률 100%)
        if (totalBulletsShot > 0 && totalBulletsShot == totalHitsLanded) {
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

        // Draw players
        for (Ship s : this.ships) {
            if (s != null) {
                drawManager.drawEntity(s, s.getPositionX(), s.getPositionY());
            }
        }

        // Draw Boss
        if (this.boss != null && this.boss.getHp() > 0) {
            drawManager.drawEntity(this.boss, this.boss.getPositionX(), this.boss.getPositionY());
        }

        // Draw Minions
        if (this.minionFormation != null) {
            this.minionFormation.draw();
        }

        // Draw Bullets
        for (Bullet bullet : this.bullets) {
            drawManager.drawEntity(bullet, bullet.getPositionX(), bullet.getPositionY());
        }

        // Draw items
        for (Item item : this.items) {
            drawManager.drawEntity(item, item.getPositionX(), item.getPositionY());
        }

        // Draw Top UI (Score, Lives, Coins)
//        drawManager.drawScore(this, state.getScore());
        drawManager.drawBossTimer(this, this.bossTimer.getDuration());
        drawManager.drawLives(this, state.getLivesRemaining(), state.isCoop());
        drawManager.drawCoins(this, state.getCoins());
        drawManager.drawLevel(this, this.state.getLevel());
        drawManager.drawHorizontalLine(this, SEPARATION_LINE_HEIGHT - 1);
        if (this.inventory != null && this.inputDelay.checkFinished()) {
            drawManager.drawItemInventory(this, this.inventory, 40, SEPARATION_LINE_HEIGHT -40);
        }

        // Draw Boss HP Bar
        if (this.boss != null) {
            drawManager.drawBossHPBar(this, this.boss.getHp(), this.boss.getMaxHp());
        }

        // Draw Boss Timer
        drawManager.drawBossTimer(this, this.bossTimer.getDuration());

        // Draw Minion count
        if (this.minionFormation != null) {
            drawManager.drawShipCount(this, this.minionFormation.getShipCount());
        }

        // Draw Countdown
        if (!this.inputDelay.checkFinished()) {
            int countdown = (int) ((INPUT_DELAY - (System.currentTimeMillis() - this.gameStartTime)) / 1000);
            drawManager.drawCountDown(this, this.state.getLevel(), countdown, false); // false for bonus life
            drawManager.drawHorizontalLine(this, this.height / 2 - this.height / 12);
            drawManager.drawHorizontalLine(this, this.height / 2 + this.height / 12);
        }

        // Draw Achievement Toasts
        drawManager.drawAchievementToasts(
                this,
                (this.achievementManager != null)
                        ? this.achievementManager.getActiveToasts()
                        : Collections.emptyList()
        );

        // Draw Pause Overlay
        if (this.isPaused) {
            drawManager.drawPauseOverlay(this);
        }

        // --- Revive UI ---
        if (this.revivePhase == RevivePhase.REVIVE_PROMPT) {
            drawManager.drawRevivePrompt(this, this.reviveSelection);
        }

        if (this.revivePhase == RevivePhase.REVIVE_RESULT) {
            drawManager.drawReviveFail(this, this.reviveFailMessage);
        }
        // -------------------

        drawMessages();

        drawManager.completeDrawing(this);
    }

    /**
     * Cleans bullets that go off screen.
     */
    private void cleanBullets() {
        Set<Bullet> recyclable = new HashSet<>();
        for (Bullet bullet : this.bullets) {
            bullet.update();
            if (bullet.getPositionY() < SEPARATION_LINE_HEIGHT
                    || bullet.getPositionY() > this.height) {
                recyclable.add(bullet);
            }
        }
        this.bullets.removeAll(recyclable);
        BulletPool.recycle(recyclable);
    }

    /**
     * Cleans items that go off screen.
     */
    private void cleanItems() {
        Set<Item> recyclableItems = new HashSet<>();
        for (Item item : this.items) {
            item.update();
            if (item.getPositionY() > this.height) {
                recyclableItems.add(item);
            }
        }
        this.items.removeAll(recyclableItems);
        ItemPool.recycle(recyclableItems);
    }

    /**
     * Manages pickups between player and items.
     */
    private void manageItemPickups() {
        manageItemPickups(this.items, this.ships, this.state);
    }

    /**
     * 총알과 엔티티 간의 충돌을 관리합니다.
     * (복잡도 해결: 충돌 로직을 대상별로 분리함)
     */
    private void manageCollisions() {
        Set<Bullet> recyclable = new HashSet<>();
        for (Bullet bullet : this.bullets) {
            if (bullet.getSpeed() > 0) {
                // 적 총알 -> 플레이어 충돌 확인
                if (handleEnemyBulletCollision(bullet)) {
                    recyclable.add(bullet);
                }
            } else {
                // 플레이어 총알 -> 적 충돌 확인
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
            if (ship != null && !ship.isDestroyed() && checkCollision(bullet, ship) && !this.levelFinished) {
                // 플레이어 피격 처리
                drawManager.triggerExplosion(ship.getPositionX(), ship.getPositionY(), false, state.getLivesRemaining() == 1);
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

                return true; // 충돌 발생함
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

        // 1. 쫄몹 충돌 확인
        if (handleMinionCollision(bullet, pIdx)) {
            return true; // 쫄몹에 맞았으면 보스 체크 안 함
        }

        // 2. 보스 충돌 확인
        return handleBossCollision(bullet);
    }

    /**
     * 플레이어 총알이 쫄몹에게 맞았는지 확인합니다.
     */
    private boolean handleMinionCollision(Bullet bullet, int pIdx) {
        if (this.minionFormation != null) {
            for (EnemyShip enemyShip : this.minionFormation) {
                if (!enemyShip.isDestroyed() && checkCollision(bullet, enemyShip)) {
                    enemyShip.hit();

                    if (enemyShip.isDestroyed()) {
                        int points = enemyShip.getPointValue();
                        state.addCoins(pIdx, enemyShip.getCoinValue());
                        drawManager.triggerExplosion(enemyShip.getPositionX(), enemyShip.getPositionY(), true, false);
                        state.addScore(pIdx, points);
                        state.incShipsDestroyed(pIdx);

                        Item drop = engine.ItemManager.getInstance().obtainDrop(enemyShip);
                        if (drop != null) {
                            this.items.add(drop);
                        }

                        this.minionFormation.destroy(enemyShip);
                        SoundManager.playOnce("sound/invaderkilled.wav");
                    }
                    return true; // 충돌 발생
                }
            }
        }
        return false;
    }

    /**
     * 플레이어 총알이 보스에게 맞았는지 확인합니다.
     */
    private boolean handleBossCollision(Bullet bullet) {
        if (this.boss != null && this.boss.getHp() > 0 && checkCollision(bullet, this.boss)) {
            if (this.boss.isInvulnerable()) {
                // 보스 무적 상태일 때 메시지 처리
                if (this.invulnerableMsgCount < MAX_INVULNERABLE_MSG_SHOWS) {
                    this.invulnerableMsgCooldown.reset();
                    this.invulnerableMsgCount++;
                }
            } else {
                // 데미지 적용
                this.boss.onHit(1);
            }

            // 보스 사망 체크
            if (this.boss.getHp() <= 0) {
                drawManager.triggerExplosion(
                        this.boss.getPositionX() + this.boss.getWidth() / 2,
                        this.boss.getPositionY() + this.boss.getHeight() / 2,
                        true, true
                );
                SoundManager.stop();
                SoundManager.playOnce(SOUND_EXPLOSION);
            }
            return true; // 충돌 발생
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
        int x = 10; // 왼쪽 하단 X 좌표
        int y = this.height - 20; // 왼쪽 하단 Y 좌표 (텍스트 베이스라인)

        // 2페이즈 메시지가 활성화 상태인지 확인 (노란색)
        if (this.phase2MsgCooldown != null && !this.phase2MsgCooldown.checkFinished()) {
            drawManager.drawString(MSG_PHASE_2, x, y, java.awt.Color.YELLOW);
        }
        // (else if 사용) 2페이즈 메시지가 아닐 때만 무적 메시지 확인 (하얀색)
        else if (this.invulnerableMsgCooldown != null && !this.invulnerableMsgCooldown.checkFinished()) {
            drawManager.drawString(MSG_MINIONS_FIRST, x, y, java.awt.Color.WHITE);
        }
    }

    /**
     * Checks for in-game achievements (ported from GameScreen).
     * These achievements can be unlocked at any time during gameplay.
     */
    public void checkInGameAchievements() {
        // First Blood (Checks if this is the first kill of the game)
        // state.getShipsDestroyed()는 GameState에 의해 관리되므로 보스전 쫄몹을 잡아도 1이 됩니다.
        if (state.getShipsDestroyed() == 1) {
            achievementManager.unlock("First Blood");
        }

        // 50 Bullets
        if (state.getBulletsShot() >= 50) {
            achievementManager.unlock("50 Bullets");
        }

        // Get 3000 Score
        if (state.getScore() >= 3000) {
            achievementManager.unlock("Get 3000 Score");
        }
    }

    // ----------------------------
    // Revive Prompt Input Handler
    // ----------------------------
    private void handleRevivePromptInput() {
        if (inputManager.isKeyDown(KeyEvent.VK_UP)) {
            reviveSelection = 0; // YES
        }
        if (inputManager.isKeyDown(KeyEvent.VK_DOWN)) {
            reviveSelection = 1; // NO
        }

        if (inputManager.isKeyDown(KeyEvent.VK_ENTER) || inputManager.isKeyDown(KeyEvent.VK_SPACE)) {
            if (reviveSelection == 0) { // YES
                boolean ok = reviveManager.tryRevive();
                if (ok) {
                    respawnPlayer();
                    this.revivePhase = RevivePhase.PLAYING;
                } else {
                    if (!reviveManager.canRevive(state.getLevel())) {
                        reviveFailMessage = "It's already revived at this level";
                    } else if (state.getCoins() < 50) {
                        reviveFailMessage = "You don't have enough coins";
                    } else {
                        reviveFailMessage = "You can't revive";
                    }
                    this.revivePhase = RevivePhase.REVIVE_RESULT;

                    InputManager.resetKeys();
                }
            } else { // NO
                // 거절 시 점수 화면(2)으로 이동
                this.returnCode = 2;
                this.isRunning = false;
            }
        }
    }


    // ----------------------------
    // Revive Result Input Handler
    // ----------------------------
    private void handleReviveResultInput() {
        if (inputManager.isKeyDown(KeyEvent.VK_ENTER) ||
                inputManager.isKeyDown(KeyEvent.VK_SPACE)) {
            // 실패 메시지 확인 후에도 점수 화면(2)으로 이동
            this.returnCode = 2;
            this.isRunning = false;
        }
    }

    // ----------------------------
    // Respawn Player (부활 위치)
    // ----------------------------
    private void respawnPlayer() {
        // 부활 시 추가 로직이 필요하면 여기에 작성
        // 현재는 ReviveManager.tryRevive()에서 목숨값은 증가시켰으므로,
        // 화면 상태만 돌려주면 됨
        this.levelFinished = false;
        this.screenFinishedCooldown.reset();
        this.revivePhase = RevivePhase.PLAYING;
    }

    // --------------------------------------------
    // RevivePrompt UI.phase handler
    // --------------------------------------------
    private void handleRevivePrompt() {
        handleRevivePromptInput();
        draw();
    }

    // --------------------------------------------
    // Revive Failure
    // --------------------------------------------
    private void handleReviveFailed() {
        handleReviveResultInput();
        draw();
    }
}