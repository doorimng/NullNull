package engine;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GameStateTest {

    @Test
    @DisplayName("보스 클리어 시간 저장 및 조회 테스트")
    void testBossClearTime() {
        // given
        // GameState 생성 (level 1, life 3, coop false, coins 0)
        GameState gameState = new GameState(1, 3, false, 0);
        long clearTime = 123456L; // 예: 2분 3초 456밀리초

        // when
        gameState.setBossClearTime(clearTime);

        // then
        assertEquals((int) clearTime, gameState.getBossClearTime());
    }

    @Test
    @DisplayName("보스 클리어 시간 초기값은 0이어야 함")
    void testInitialBossClearTime() {
        // given
        GameState gameState = new GameState(1, 3, false, 0);

        // then
        assertEquals(0, gameState.getBossClearTime());
    }
}