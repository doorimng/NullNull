package screen;

import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.*;

import engine.*;

/**
 * Implements the score screen.
 *
 * @author <a href="mailto:RobertoIA1987@gmail.com">Roberto Izquierdo Amo</a>
 *
 */
public class ScoreScreen extends Screen {

    /** Milliseconds between changes in user selection. */
    private static final int SELECTION_TIME = 200;
    /** Maximum name length. */
    private static final int MAX_NAME_LENGTH = 5;
    /** Code of max high score. */
    private static final int MAX_HIGH_SCORE_NUM = 7;

    private final GameState gameState;

    /** Player lives left. */
    private int livesRemaining;
    /** Current coins. */
    private int coins;
    /** Total bullets shot by the player. */
    private int bulletsShot;
    /** Total ships destroyed by the player. */
    private int shipsDestroyed;
    /** List of past high scores. */
    private List<Score> highScores;

    /** Checks if current score is a new high score. */
    private boolean isNewRecord;
    /** Check if the player cleared the level. */
    private boolean isClear;

    /** Player name for record input. */
    private StringBuilder name;

    /** [복구됨] Make sure the name is less than 3 characters. */
    private boolean showNameError;

    /** Time between changes in user selection. */
    private Cooldown selectionCooldown;
    /** manages achievements.*/
    private AchievementManager achievementManager;
    /** check 1P/2P mode; */
    private String mode;

    /** Flag to check if the user has released keys after screen transition. */
    private boolean isInputReleased;

    /**
     * Constructor.
     */
    public ScoreScreen(final int width, final int height, final int fps,
                       final GameState gameState, final AchievementManager achievementManager) throws IOException {
        super(width, height, fps);
        this.gameState = gameState;

        this.livesRemaining = gameState.getLivesRemaining();
        this.coins = gameState.getCoins();
        this.bulletsShot = gameState.getBulletsShot();
        this.shipsDestroyed = gameState.getShipsDestroyed();

        // 1. 클리어 여부 판단 (목숨이 있어야 클리어)
        this.isClear = this.livesRemaining > 0;

        this.isNewRecord = false;
        this.name = new StringBuilder();
        this.showNameError = false; // 초기화
        this.selectionCooldown = Core.getCooldown(SELECTION_TIME);
        this.selectionCooldown.reset();

        // 키 뗌 확인을 위한 초기화 (false로 시작)
        this.isInputReleased = false;

        this.achievementManager = achievementManager;
        this.mode = gameState.getCoop() ? "2P" : "1P";

        // 2. 클리어 시에만 신기록 판정 진행 (Time Attack)
        if (this.isClear) {
            int currentClearTime = gameState.getBossClearTime();
            try {
                this.highScores = Core.getFileManager().loadHighScores(this.mode);
                // 기록이 없거나 꼴찌보다 내 시간이 빠르면 신기록
                if (highScores.size() < MAX_HIGH_SCORE_NUM
                        || highScores.get(highScores.size() - 1).getScore() > currentClearTime) {
                    this.isNewRecord = true;
                }
            } catch (IOException e) {
                logger.warning("Couldn't load high scores!");
            }
        } else {
            // 실패 시 무조건 신기록 아님
            this.isNewRecord = false;
        }

        inputManager.clearLastKey();
    }

    /**
     * Starts the action.
     */
    public final int run() {
        SoundManager.playOnce("sound/select.wav");
        super.run();
        return this.returnCode;
    }

    /**
     * Updates the elements on screen and checks for events.
     */
    protected final void update() {
        super.update();
        draw();

        // -------------------------------------------------------
        // [핵심 로직] 키 뗌 확인 (Key Release Check)
        // 화면이 전환된 직후, 사용자가 이전에 누르고 있던 키(Space, Enter 등)를
        // 뗄 때까지 입력을 처리하지 않고 기다립니다.
        // -------------------------------------------------------
        if (!this.isInputReleased) {
            if (inputManager.isKeyDown(KeyEvent.VK_SPACE) ||
                    inputManager.isKeyDown(KeyEvent.VK_ENTER) ||
                    inputManager.isKeyDown(KeyEvent.VK_ESCAPE)) {
                return; // 키가 눌려있으면 아무 동작 안 함
            }
            // 모든 키가 떼어졌으면 입력 허용 상태로 전환
            this.isInputReleased = true;
        }

        // 여기서부터는 실제 입력 처리 로직 (isInputReleased가 true일 때만 도달)

        // -------------------------------------------------------
        // 1. 실패(GAME OVER) 시 로직
        // -------------------------------------------------------
        if (!this.isClear) {
            // ESC: 맵으로 나가기 (Core 루프의 시작인 MapScreen으로 이동하기 위해 returnCode 2 반환)
            if (inputManager.isKeyDown(KeyEvent.VK_ESCAPE) ||
                    (inputManager.isKeyDown(KeyEvent.VK_SPACE))) {
                this.returnCode = 2; // Restart
                this.isRunning = false;
            }
            return;
        }

        // -------------------------------------------------------
        // 2. 성공(CLEAR) 시 로직
        // -------------------------------------------------------

        // ESC 키: 메인 타이틀로 이동 (1번)
        if (inputManager.isKeyDown(KeyEvent.VK_ESCAPE)) {
            this.returnCode = 1; // Title
            this.isRunning = false;
            if (this.isNewRecord) {
                saveScore();
                saveAchievement();
            }
        }
        // SPACE 키: 저장 후 메인 타이틀로 이동 (1번)
        else if (inputManager.isKeyDown(KeyEvent.VK_SPACE)) {
            // [복구됨] 이름이 3글자 미만이면 에러 표시하고 진행 안 함
            if (this.name.length() < 3) {
                this.showNameError = true;
                return;
            }

            this.returnCode = 1; // Title

            this.isRunning = false;
            if (this.isNewRecord) {
                saveScore();
                saveAchievement();
            }
        }

        // 이름 입력 (Backspace)
        if (inputManager.isKeyDown(KeyEvent.VK_BACK_SPACE)
                && this.selectionCooldown.checkFinished()) {
            if (this.name.length() > 0) {
                this.name.deleteCharAt(this.name.length() - 1);
                this.selectionCooldown.reset();
                this.showNameError = false; // 글자 지우면 에러 해제
            }
        }

        // 이름 입력 (문자)
        char typedChar = inputManager.getLastCharTyped();
        if (typedChar != '\0' && Character.isLetterOrDigit(typedChar) && this.name.length() < MAX_NAME_LENGTH) {
            this.name.append(Character.toUpperCase(typedChar));
            this.showNameError = false; // 글자 입력하면 에러 해제
        }
    }

    /**
     * Saves the score (TIME) as a high score.
     */
    private void saveScore() {
        String mode = (gameState != null && gameState.isCoop()) ? "2P" : "1P";
        String newName = new String(this.name);
        int clearTime = this.gameState.getBossClearTime();

        Score newScore = new Score(newName, clearTime, mode);

        boolean foundAndReplaced = false;
        for (int i = 0; i < highScores.size(); i++) {
            Score existingScore = highScores.get(i);
            if (existingScore.getName().equals(newName)) {
                // [확인] Time Attack이므로 시간이 더 짧을 때 갱신
                if (newScore.getScore() < existingScore.getScore()) {
                    highScores.set(i, newScore);
                    foundAndReplaced = true;
                } else {
                    foundAndReplaced = true;
                }
                break;
            }
        }
        if (!foundAndReplaced) highScores.add(newScore);

        Collections.sort(highScores);
        if (highScores.size() > MAX_HIGH_SCORE_NUM)
            highScores.remove(highScores.size() - 1);

        try {
            Core.getFileManager().saveHighScores(highScores, mode);
        } catch (IOException e) {
            logger.warning("Couldn't save high scores!");
        }
    }

    private void saveAchievement() {
        try {
            this.achievementManager.saveToFile(new String(this.name), this.mode);
        } catch (IOException e) {
            logger.warning("Couldn't save achievements!");
        }
    }

    /**
     * Draws the elements associated with the screen.
     */
    private void draw() {
        drawManager.initDrawing(this);

        // 1. [타이틀 및 안내 문구]
        drawManager.drawGameTitle(this, this.isClear);

        // 2. [클리어 시간 표시]
        if (this.isClear) {
            int timeMs = this.gameState.getBossClearTime();
            long minutes = (timeMs / 1000) / 60;
            long seconds = (timeMs / 1000) % 60;
            String timeString = String.format("Clear Time: %02d:%02d", minutes, seconds);
            drawManager.drawCenteredRegularString(this, timeString, this.getHeight() / 4 + 20);
        }

        // 3. [결과 통계 표시]
        boolean isFailure = !this.isClear;

        if (this.gameState != null && this.gameState.isCoop()) {
            // [2P 모드]
            drawManager.drawResults(this, this.gameState.getCoins(),
                    this.gameState.getLivesRemaining(), this.gameState.getShipsDestroyed(),
                    0f, false, isFailure);

            float p1Acc = this.gameState.getBulletsShot(0) > 0 ? (float) this.gameState.getShipsDestroyed(0) / this.gameState.getBulletsShot(0) : 0f;
            float p2Acc = this.gameState.getBulletsShot(1) > 0 ? (float) this.gameState.getShipsDestroyed(1) / this.gameState.getBulletsShot(1) : 0f;
            String p1 = String.format("P1  %04d  |  acc %.2f%%", this.gameState.getScore(0), p1Acc * 100f);
            String p2 = String.format("P2  %04d  |  acc %.2f%%", this.gameState.getScore(1), p2Acc * 100f);

            int y = this.isNewRecord ? this.getHeight() / 2 + 40 : this.getHeight() / 2 + 80;
            drawManager.drawCenteredRegularString(this, p1, y);
            drawManager.drawCenteredRegularString(this, p2, y + 20);

        } else {
            // [1P 모드]
            float acc = (this.bulletsShot > 0) ? (float) this.shipsDestroyed / this.bulletsShot : 0f;
            drawManager.drawResults(this, this.coins, this.livesRemaining,
                    this.shipsDestroyed, acc, true, isFailure);
        }

        // 4. [이름 입력창] - 성공했을 때만 표시
        if (this.isClear) {
            drawManager.drawNameInput(this, this.name, this.isNewRecord);
            // [복구됨] 에러 메시지 표시
            if (this.showNameError) {
                drawManager.drawNameInputError(this);
            }
        }

        drawManager.completeDrawing(this);
    }
}