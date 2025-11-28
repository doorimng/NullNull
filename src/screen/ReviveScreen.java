package screen;

import java.awt.event.KeyEvent;

import engine.GameState;
import engine.InputManager;
import engine.ReviveManager;
import engine.DrawManager;

/**
 * 공통 부활(Revive) 로직을 담는 Screen 베이스 클래스.
 * GameScreen / BossScreen 이 상속해서 사용한다.
 */
public abstract class ReviveScreen extends Screen {

    /** 부활 단계 */
    protected enum RevivePhase {
        PLAYING,
        REVIVE_PROMPT,
        REVIVE_RESULT,
        EXITING
    }

    /** 게임 상태 (두 화면 공통) */
    protected final GameState state;
    /** 부활 처리 매니저 */
    protected final ReviveManager reviveManager;

    /** 현재 부활 단계 */
    protected RevivePhase revivePhase = RevivePhase.PLAYING;
    /** Revive UI 선택값 (0 = YES, 1 = NO) */
    protected int reviveSelection = 0;
    /** Revive 실패 메시지 */
    protected String reviveFailMessage = "";

    /**
     * Revive 기능이 필요한 Screen 의 공통 생성자.
     */
    protected ReviveScreen(final GameState gameState,
                           final int width, final int height, final int fps) {
        super(width, height, fps);
        this.state = gameState;
        this.reviveManager = new ReviveManager(this.state);
    }

    /**
     * Revive 관련 상태를 초기화한다.
     * (레벨 시작 시 등 한 번 호출해 주면 됨)
     */
    protected void initReviveState() {
        this.revivePhase = RevivePhase.PLAYING;
        this.reviveSelection = 0;
        this.reviveFailMessage = "";
    }

    // ----------------------------------------------------------------------
    // 공통 Revive 입력 처리
    // ----------------------------------------------------------------------

    /**
     * Revive 선택 창에서의 입력 처리 (위/아래/엔터/스페이스).
     * 실제 성공/실패 이후의 동작은 콜백으로 하위 클래스에서 구현한다.
     */
    protected void handleRevivePromptInput(InputManager inputManager) {
        if (inputManager.isKeyDown(KeyEvent.VK_UP)) {
            reviveSelection = 0; // YES
        }
        if (inputManager.isKeyDown(KeyEvent.VK_DOWN)) {
            reviveSelection = 1; // NO
        }

        if (inputManager.isKeyDown(KeyEvent.VK_ENTER)
                || inputManager.isKeyDown(KeyEvent.VK_SPACE)) {
            if (reviveSelection == 0) { // YES
                boolean ok = reviveManager.tryRevive();
                if (ok) {
                    // ✅ 부활 성공 시, 공통으로 라이프 1개 회복
                    applyReviveLifeIfNeeded();
                    // 그리고 각 화면별 추가 처리는 콜백에서
                    onReviveSuccess();
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
                onReviveRejected();
            }
        }
    }

    /**
     * 🔥 부활 성공 시, 실제로 목숨을 1개 살려주는 공통 로직
     */
    protected void applyReviveLifeIfNeeded() {
        if (state.isSharedLives()) {
            // 2P 공유 라이프 모드
            if (!state.teamAlive()) {
                state.addTeamLife(1);
            }
        } else {
            // 1P / 개별 라이프 모드 → 기본은 P1에게 한 목숨 지급
            if (!state.teamAlive()) {
                state.addLife(0, 1);
            }
        }
    }

    /**
     * Revive 실패 결과 창에서의 입력 처리.
     */
    protected void handleReviveResultInput(InputManager inputManager) {
        if (inputManager.isKeyDown(KeyEvent.VK_ENTER)
                || inputManager.isKeyDown(KeyEvent.VK_SPACE)) {
            onReviveResultAcknowledged();
        }
    }

    /**
     * draw() 내부에서 revive UI를 그릴 때 사용하는 공통 헬퍼.
     */
    protected void drawReviveUiIfNeeded(DrawManager drawManager) {
        if (this.revivePhase == RevivePhase.REVIVE_PROMPT) {
            drawManager.drawRevivePrompt(this, this.reviveSelection);
        } else if (this.revivePhase == RevivePhase.REVIVE_RESULT) {
            drawManager.drawReviveFail(this, this.reviveFailMessage);
        }
    }

    // ----------------------------------------------------------------------
    // 하위 클래스가 구현해야 하는 콜백들
    // ----------------------------------------------------------------------

    /**
     * Revive 성공 시 호출된다.
     * - 목숨/쿨다운/phase 복구 같은 실제 화면별 처리를 여기서 한다.
     */
    protected abstract void onReviveSuccess();

    /**
     * Revive 창에서 "NO" 를 선택했을 때 호출된다.
     * - 점수 화면으로 이동 등 각 화면에 맞는 종료 처리를 하면 된다.
     */
    protected abstract void onReviveRejected();

    /**
     * Revive 실패 메시지를 확인(ENTER/SPACE) 했을 때 호출된다.
     */
    protected abstract void onReviveResultAcknowledged();
}
