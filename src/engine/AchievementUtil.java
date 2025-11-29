// engine/AchievementUtil.java
package engine;

public final class AchievementUtil {

    private AchievementUtil() {
        // static util
    }

    /**
     * GameScreen / BossScreen 공통으로 쓰는 기본 업적들
     * - First Blood
     * - 50 Bullets
     * - Get 3000 Score
     */
    public static void checkBasicAchievements(GameState state,
                                              AchievementManager achievementManager) {
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
}
