// src/test/java/engine/AchievementUtilTest.java
package engine;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class AchievementUtilTest {

    /**
     * 테스트용 AchievementManager
     * unlock 이 어떤 id 로 호출됐는지 기록만 함
     */
    private static class TestAchievementManager extends AchievementManager {
        private final Set<String> unlocked = new HashSet<>();

        @Override
        public void unlock(String id) {
            // 필요하면 super.unlock(id); 도 호출 가능
            unlocked.add(id);
        }

        boolean isUnlocked(String id) {
            return unlocked.contains(id);
        }

        int unlockedCount() {
            return unlocked.size();
        }
    }

    @Test
    void noAchievementsWhenConditionsNotMet() {
        GameState state = new GameState(
                1,   // level
                0,   // score
                3,   // livesRemaining
                0,   // bulletsShot
                0,   // shipsDestroyed
                0    // coins
        );

        TestAchievementManager manager = new TestAchievementManager();

        AchievementUtil.checkBasicAchievements(state, manager);

        assertFalse(manager.isUnlocked("First Blood"));
        assertFalse(manager.isUnlocked("50 Bullets"));
        assertFalse(manager.isUnlocked("Get 3000 Score"));
        assertEquals(0, manager.unlockedCount());
    }

    @Test
    void unlocksFirstBloodWhenOneShipDestroyed() {
        GameState state = new GameState(
                1,
                0,
                3,
                0,
                1,  // shipsDestroyed == 1
                0
        );

        TestAchievementManager manager = new TestAchievementManager();

        AchievementUtil.checkBasicAchievements(state, manager);

        assertTrue(manager.isUnlocked("First Blood"));
        assertFalse(manager.isUnlocked("50 Bullets"));
        assertFalse(manager.isUnlocked("Get 3000 Score"));
    }

    @Test
    void unlocks50BulletsWhenBulletCountAtLeast50() {
        GameState state = new GameState(
                1,
                0,
                3,
                50,  // bulletsShot >= 50
                0,
                0
        );

        TestAchievementManager manager = new TestAchievementManager();

        AchievementUtil.checkBasicAchievements(state, manager);

        assertTrue(manager.isUnlocked("50 Bullets"));
        assertFalse(manager.isUnlocked("First Blood"));
        assertFalse(manager.isUnlocked("Get 3000 Score"));
    }

    @Test
    void unlocksGet3000ScoreWhenScoreAtLeast3000() {
        GameState state = new GameState(
                1,
                3000,  // score >= 3000
                3,
                0,
                0,
                0
        );

        TestAchievementManager manager = new TestAchievementManager();

        AchievementUtil.checkBasicAchievements(state, manager);

        assertTrue(manager.isUnlocked("Get 3000 Score"));
        assertFalse(manager.isUnlocked("First Blood"));
        assertFalse(manager.isUnlocked("50 Bullets"));
    }

    @Test
    void canUnlockAllBasicAchievementsTogether() {
        GameState state = new GameState(
                1,
                3000, // score >= 3000
                3,
                50,   // bulletsShot >= 50
                1,    // shipsDestroyed == 1
                0
        );

        TestAchievementManager manager = new TestAchievementManager();

        AchievementUtil.checkBasicAchievements(state, manager);

        assertTrue(manager.isUnlocked("First Blood"));
        assertTrue(manager.isUnlocked("50 Bullets"));
        assertTrue(manager.isUnlocked("Get 3000 Score"));
        assertEquals(3, manager.unlockedCount());
    }
}
