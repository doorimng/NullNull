package engine;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BossTimerTest {

    @Mock
    private TimeProvider timeProvider;

    private BossTimer bossTimer;

    @BeforeEach
    void setUp() {
        bossTimer = new BossTimer(timeProvider);
    }

    @Test
    @DisplayName("일반 스테이지에서는 타이머가 켜지면 안 됨")
    void testStartAtNormalLevel() {
        bossTimer.start(5);

        assertFalse(bossTimer.isRunning());
        assertEquals(0, bossTimer.getDuration());

        verify(timeProvider, never()).getCurrentTime();
    }

    @Test
    @DisplayName("보스 스테이지(6)에서는 타이머가 정상 작동해야 함")
    void testStartAtBossLevel() {
        when(timeProvider.getCurrentTime()).thenReturn(1000L);

        bossTimer.start(6);

        assertTrue(bossTimer.isRunning());
        verify(timeProvider).getCurrentTime();
    }

    @Test
    @DisplayName("정상적인 시간 측정 흐름")
    void testFullTimerFlow() {
        when(timeProvider.getCurrentTime())
                .thenReturn(1000L)
                .thenReturn(6000L);

        bossTimer.start(6);
        bossTimer.stop();

        assertEquals(5000L, bossTimer.getDuration());
        assertFalse(bossTimer.isRunning());
    }

    @Test
    @DisplayName("시작하지 않고 종료(stop)를 불렀을 때 예외 없이 0 반환")
    void testStopWithoutStart() {
        bossTimer.stop();

        assertEquals(0, bossTimer.getDuration());
    }
}