package screen;

import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import engine.Core;
import engine.Score;
import engine.SoundManager;

/**
 * Implements the high scores screen, it shows player records.
 * Refactored to reduce duplication and support both 1P/2P.
 *
 * @author <a href="mailto:RobertoIA1987@gmail.com">Roberto Izquierdo Amo</a>
 */
public class HighScoreScreen extends Screen {

    /** List of past high scores. */
    private List<Score> highScores1P;

    /**
     * Constructor, establishes the properties of the screen.
     *
     * @param width
     * Screen width.
     * @param height
     * Screen height.
     * @param fps
     * Frames per second, frame rate at which the game is run.
     */
    public HighScoreScreen(final int width, final int height, final int fps) {
        super(width, height, fps);
        SoundManager.playLoop("sound/menu_sound.wav");

        this.returnCode = 1;

        // [Refactoring] Use helper method to load scores for both modes
        this.highScores1P = loadAndSortScores("1P");
    }

    /**
     * Loads, sorts, and trims high scores for a specific mode.
     * Reduces code duplication for 1P and 2P loading logic.
     *
     * @param mode "1P" or "2P"
     * @return Sorted list of top 7 scores
     */
    private List<Score> loadAndSortScores(String mode) {
        try {
            List<Score> scores = Core.getFileManager().loadHighScores(mode);
            // Sort by time (ascending) for Time Attack
            scores.sort((a, b) -> Integer.compare(a.getScore(), b.getScore()));

            // Keep only top 7
            if (scores.size() > 7) {
                return scores.subList(0, 7);
            }
            return scores;
        } catch (NumberFormatException | IOException e) {
            logger.warning("Couldn't load high scores for " + mode);
            return Collections.emptyList();
        }
    }

    /**
     * Starts the action.
     *
     * @return Next screen code.
     */
    public final int run() {
        super.run();
        SoundManager.playOnce("sound/select.wav");
        return this.returnCode;
    }

    /**
     * Updates the elements on screen and checks for events.
     */
    protected final void update() {
        super.update();

        draw();
        if (inputManager.isKeyDown(KeyEvent.VK_ESCAPE)
                && this.inputDelay.checkFinished())
            this.isRunning = false;

        // back button click event
        if (inputManager.isMouseClicked()) {
            int mx = inputManager.getMouseX();
            int my = inputManager.getMouseY();
            java.awt.Rectangle backBox = drawManager.getBackButtonHitbox(this);

            if (backBox.contains(mx, my)) {
                this.returnCode = 1;
                this.isRunning = false;
            }
        }
    }

    /**
     * Draws the elements associated with the screen.
     */
    private void draw() {
        drawManager.initDrawing(this);

        drawManager.drawHighScoreMenu(this);

        // [Refactoring] Draw both columns using the generalized method
        // 1P Scores
        drawManager.drawScoreColumn(this, highScores1P, width / 2, "");


        // hover highlight
        int mx = inputManager.getMouseX();
        int my = inputManager.getMouseY();
        java.awt.Rectangle backBox = drawManager.getBackButtonHitbox(this);

        if (backBox.contains(mx, my)) {
            drawManager.drawBackButton(this, true);
        }

        drawManager.completeDrawing(this);
    }
}