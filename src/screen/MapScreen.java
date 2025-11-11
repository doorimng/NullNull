package screen;

import engine.GameState ;
import java.awt.event.KeyEvent;
import java.io.IOException;

public class MapScreen extends Screen {

    public static GameState gameState ;

    /**
     * Constructor, establishes the properties of the screen.
     *
     * @param width          Screen width.
     * @param height         Screen height.
     * @param fps           Frames per second, frame rate at which the game is run.
     * @param currentLevel  현재 스테이지 인덱스.
     */
    public MapScreen(int width, int height, int fps, int currentLevel) {
        super(width, height, fps);
        this.currentLevel = currentLevel;
        this.returnCode = 1;
    }

    /** 현재스테이지 인덱스 */
    private int currentLevel = 1;

    /**
     * @return Exit 버튼 클릭 여부를 반환합니다.
     * */
    public boolean isClickedExit() {
        return true ;
    }

    public void changeScreen() {
        //if ( isClickedExit() ) gameState
    }


    public final int run() {
        super.run();

        return this.returnCode;
    }


    protected final void update() {
        super.update();

        draw();

        // 키 입력
        if (this.inputDelay.checkFinished()) {
            if (inputManager.isKeyDown(KeyEvent.VK_SPACE)) {
                this.returnCode = 5;  // go to PlayScreen
                this.isRunning = false;
            } else if (inputManager.isKeyDown(KeyEvent.VK_ESCAPE)) {
                this.returnCode = 1;  // 메인 메뉴로
                this.isRunning = false;
            }
        }


    }

    private void draw() {
        drawManager.initDrawing(this);
        drawManager.drawMap(this, this.currentLevel);
        drawManager.completeDrawing(this);
    }

}