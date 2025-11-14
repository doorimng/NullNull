package entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.InOrder;
import java.util.function.IntSupplier;

import static org.mockito.Mockito.*;

public class BossTest {

    BulletEmitter emitter;
    IntSupplier minionAlive;
    Runnable spawnHP1, spawnHP2, clearShield;
    Runnable onPhase2Start;
    Boss boss;

    @BeforeEach
    void setUp() {
        emitter = mock(BulletEmitter.class);
        minionAlive = mock(IntSupplier.class);
        spawnHP1 = mock(Runnable.class);
        spawnHP2 = mock(Runnable.class);
        clearShield = mock(Runnable.class);

        when(minionAlive.getAsInt()).thenReturn(5);

        // 요구사항에 맞춰 HP 10, P2 전환 50% (HP 5)로 수정
        boss = new Boss(10, 5, 448, emitter, minionAlive, spawnHP1, spawnHP2, clearShield, onPhase2Start);

        // 테스트 편의를 위해 발사 간격 1로 설정
        boss.setFireEveryFramesP1(1);
        boss.setFireEveryFramesP2(1);
    }

    @Test
    @DisplayName("초기: P1, HP1 그룹 스폰 1회, 첫 update에서 무적(true)")
    void initState() {
        assertEquals(BossPhase.P1, boss.getPhase());
        verify(spawnHP1, times(1)).run();

        boss.update();
        assertTrue(boss.isInvulnerable());
    }

    @Test
    @DisplayName("무적 규칙: 쫄몹>0이면 무적, 0이면 피격 가능")
    void invulnTracksMinions() {
        boss.update();
        assertTrue(boss.isInvulnerable());

        when(minionAlive.getAsInt()).thenReturn(0);
        boss.update();
        assertFalse(boss.isInvulnerable());

        when(minionAlive.getAsInt()).thenReturn(3);
        boss.update();
        assertTrue(boss.isInvulnerable());
    }

    @Test
    @DisplayName("보스는 무적이어도 공격: P1은 3갈래 발사")
    void firesWhileInvulnerable() {
        clearInvocations(emitter);
        boss.update();
        verify(emitter, times(3)).fire(anyInt(), anyInt(), anyInt(), anyInt());
    }

    @Test
    @DisplayName("HP가 5가 되는 순간 P2 전환(<=50%)")
    void phase2AtHalf() {
        when(minionAlive.getAsInt()).thenReturn(0);
        boss.update();

        boss.onHit(4); // 10 -> 6 (P1 유지)
        assertEquals(BossPhase.P1, boss.getPhase());

        boss.onHit(1); // 6 -> 5 (P2 전환)
        assertEquals(BossPhase.P2, boss.getPhase());
    }

    @Test
    @DisplayName("P2 진입 시 clearShield→spawnHP2 순서 호출 + P2는 5갈래")
    void p2RespawnAndFire5Way() {
        when(minionAlive.getAsInt()).thenReturn(0);
        boss.update();
        boss.onHit(6); // 10 -> 4 (P2 전환)

        InOrder order = inOrder(clearShield, spawnHP2);
        order.verify(clearShield).run();
        order.verify(spawnHP2).run();
        order.verify(onPhase2Start).run();

        clearInvocations(emitter);
        boss.update();
        verify(emitter, times(5)).fire(anyInt(), anyInt(), anyInt(), anyInt());
    }

    @Test
    @DisplayName("무적 중 데미지 무시, 전멸 후 데미지 적용")
    void damageGate() {
        boss.update();
        boss.onHit(3);
        assertEquals(10, boss.getHp()); // HP 10 (초기값)

        when(minionAlive.getAsInt()).thenReturn(0);
        boss.update();
        boss.onHit(3);
        assertEquals(7, boss.getHp()); // 10 -> 7
    }

    @Test
    @DisplayName("P1에서 update 50회 호출 시 1회 오른쪽 이동")
    void p1MovesRight() {
        int initialX = boss.getPositionX();
        int expectedSpeed = 8 * 1;

        for (int i = 0; i < 50; i++) {
            boss.update();
        }

        assertEquals(initialX + expectedSpeed, boss.getPositionX());
    }

    @Test
    @DisplayName("P2에서 update 80회 호출 시 1회 오른쪽 이동")
    void p2MovesRight() {
        when(minionAlive.getAsInt()).thenReturn(0);
        boss.onHit(6);

        boss.setSpeedP2_pxPerFrame(3);
        int expectedSpeed = 8 * 3;
        int initialX = boss.getPositionX();

        for (int i = 0; i < 80; i++) {
            boss.update();
        }

        assertEquals(initialX + expectedSpeed, boss.getPositionX());
    }

    @Test
    @DisplayName("오른쪽 벽에 닿으면 방향 전환")
    void hitsRightWallAndTurnsLeft() {
        boss.setPosition(337, 5);

        for (int i = 0; i < 50; i++) boss.update();
        assertEquals(338, boss.getPositionX());

        for (int i = 0; i < 50; i++) boss.update();
        assertEquals(338 - 8, boss.getPositionX());
    }

    @Test
    @DisplayName("왼쪽 벽에 닿으면 방향 전환")
    void hitsLeftWallAndTurnsRight() {
        boss.setPosition(337, 5);
        for (int i = 0; i < 50; i++) boss.update();

        boss.setPosition(11, 5);

        for (int i = 0; i < 50; i++) boss.update();
        assertEquals(10, boss.getPositionX());

        for (int i = 0; i < 50; i++) boss.update();
        assertEquals(10 + 8, boss.getPositionX());
    }

    @Test
    @DisplayName("HP가 0보다 낮아지면 0으로 고정")
    void bossHpGoesBelowZero() {
        when(minionAlive.getAsInt()).thenReturn(0);
        boss.update();

        boss.onHit(15);
        assertEquals(0, boss.getHp());
    }

    @Test
    @DisplayName("Emitter가 null일 때 update() 호출 시 NPE 발생 안 함")
    void nullEmitterDoesNotCrash() {
        Boss nullBoss = new Boss(10, 5, 448, null, minionAlive,
                spawnHP1, spawnHP2, clearShield, onPhase2Start);
        nullBoss.setFireEveryFramesP1(1);

        assertDoesNotThrow(() -> nullBoss.update());
    }
}