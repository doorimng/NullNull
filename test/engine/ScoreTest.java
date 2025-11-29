package engine;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ScoreTest {

    @Test
    @DisplayName("Score 생성자 및 Getter 테스트")
    void testScoreConstructorAndGetters() {
        // given
        String name = "TST";
        int time = 120000; // 2분
        String mode = "1P";

        // when
        Score score = new Score(name, time, mode);

        // then
        assertEquals(name, score.getName());
        assertEquals(time, score.getScore()); // 이름은 getScore지만 내용은 time
        assertEquals(mode, score.getMode());
    }

    @Test
    @DisplayName("GameState를 이용한 생성자 테스트")
    void testScoreFromGameState() {
        // given
        GameState mockState = mock(GameState.class);
        when(mockState.getBossClearTime()).thenReturn(50000); // 50초
        when(mockState.getLevel()).thenReturn(6);
        when(mockState.getLivesRemaining()).thenReturn(2);
        // GameState.NUM_PLAYERS = 2 라고 가정하고 모킹
        when(mockState.getScore(0)).thenReturn(100);
        when(mockState.getBulletsShot(0)).thenReturn(50);
        when(mockState.getShipsDestroyed(0)).thenReturn(40);

        // when
        Score score = new Score("P1", mockState, "1P");

        // then
        assertEquals("P1", score.getName());
        assertEquals(50000, score.getScore()); // BossClearTime이 잘 들어갔는지 확인
        assertEquals(6, score.getLevelReached());
        assertEquals(2, score.getLivesRemaining());
    }

    @Test
    @DisplayName("랭킹 정렬: 시간이 짧은 순서(오름차순)로 정렬되어야 함")
    void testCompareTo() {
        // given
        Score fastScore = new Score("FAST", 30000, "1P"); // 30초 (1등)
        Score mediumScore = new Score("MID", 60000, "1P"); // 60초 (2등)
        Score slowScore = new Score("SLOW", 90000, "1P"); // 90초 (3등)

        List<Score> scores = new ArrayList<>();
        scores.add(slowScore);
        scores.add(fastScore);
        scores.add(mediumScore);

        // when
        Collections.sort(scores);

        // then
        assertEquals(fastScore, scores.get(0), "가장 시간이 짧은 기록이 첫 번째여야 합니다.");
        assertEquals(mediumScore, scores.get(1));
        assertEquals(slowScore, scores.get(2), "가장 시간이 긴 기록이 마지막이어야 합니다.");
    }

    @Test
    @DisplayName("동일한 시간일 경우 정렬 순서 유지 (또는 동일 취급)")
    void testCompareToSameTime() {
        Score s1 = new Score("A", 1000, "1P");
        Score s2 = new Score("B", 1000, "1P");

        // compareTo가 0을 반환해야 함
        assertEquals(0, s1.compareTo(s2));
    }
}
