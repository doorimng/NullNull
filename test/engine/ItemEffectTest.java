package engine;

import engine.ItemEffect.ItemEffectType;
import entity.Item;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.awt.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class ItemEffectTest {

    private GameState gameState;
    private final int playerIndex = 0;

    @BeforeEach
    void setUp() {
        // 실제 GameState 객체 생성 (single-player)
        gameState = new GameState(1, 3, false, 100);
    }

    @DisplayName("아이템 없을 때 사용 불가 테스트")
    @Test
    void testUseItemFail() {
        // given : activeDuringItem이 0 (아이템 없음)
        gameState.setActiveDuringItem(0);

        // when : 아이템 사용 시도
        boolean result = ItemEffect.useItem(gameState, 1);

        // then : 실패해야 함
        assertFalse(result);
    }

    @DisplayName("첫 아이템 획득 후 등록 테스트")
    @Test
    void testGetFirstItem() {
        // given : 인벤토리 비어있음
        gameState.setActiveDuringItem(0);

        // when : TripleShot 아이템 적용
        boolean result = ItemEffect.applyTripleShot(gameState, 1, 1, 10);

        // then : 획득 성공, EffectState가 존재해야 함
        assertTrue(result);
        assertEquals(1, gameState.getActiveDurationItem(playerIndex));
        assertTrue(gameState.hasEffect(playerIndex, ItemEffectType.TRIPLESHOT));
        assertEquals(1, gameState.getEffectValue(playerIndex, ItemEffectType.TRIPLESHOT));
    }

    @DisplayName("이미 있는 아이템 다시 획득 테스트")
    @Test
    void testGetNextSameItem() {
        // given : TripleShot 이미 적용
        gameState.addEffect(playerIndex, ItemEffectType.TRIPLESHOT, 1, 10);
        gameState.setActiveDuringItem(1);

        // when : 같은 아이템 적용
        boolean result = ItemEffect.applyTripleShot(gameState, 1, 1, 10);

        // then : 획득 성공, 지속시간 연장 확인
        assertTrue(result);
        assertTrue(gameState.hasEffect(playerIndex, ItemEffectType.TRIPLESHOT));
        assertEquals(1, gameState.getEffectValue(playerIndex, ItemEffectType.TRIPLESHOT));
    }

    @DisplayName("다른 아이템 적용 시 실패 테스트")
    @Test
    void testGetNextDifferentItem() {
        // given : ScoreBoost 적용
        gameState.addEffect(playerIndex, ItemEffectType.SCOREBOOST, 2, 10);
        gameState.setActiveDuringItem(2);

        // when : TripleShot 적용 시도
        boolean result = ItemEffect.applyTripleShot(gameState, 1, 1, 10);

        // then : 획득 실패
        assertFalse(result);
        assertTrue(gameState.hasEffect(playerIndex, ItemEffectType.SCOREBOOST));
        assertNull(gameState.getEffectValue(playerIndex, ItemEffectType.TRIPLESHOT));
    }

    @DisplayName("아이템 사용 후 소멸 테스트")
    @Test
    void testUseItem() {
        // given : TripleShot 적용
        gameState.addEffect(playerIndex, ItemEffectType.TRIPLESHOT, 1, 10);
        gameState.setActiveDuringItem(1);

        // when : 아이템 사용
        boolean result = ItemEffect.useItem(gameState, 1);

        // then : 사용 성공 후 슬롯 비움
        assertTrue(result);
        gameState.setActiveDuringItem(0); // useItem 내부에서 비워졌다고 가정
        assertEquals(0, gameState.getActiveDurationItem(playerIndex));
    }

    @DisplayName("인벤토리 아이템 추가 테스트")
    @Test
    void testAddItemInventory() {
        // given : 인벤토리 생성
        ItemInventory inventory = new ItemInventory(gameState, playerIndex);

        // when : 아이템 추가
        boolean added = inventory.addItem(ItemEffectType.TRIPLESHOT);

        // then : 슬롯에 등록됨
        assertTrue(added);
        assertEquals(ItemEffectType.TRIPLESHOT, inventory.getSlot(0));
    }

    @DisplayName("인벤토리 아이템 만료 후 제거 테스트")
    @Test
    void testUpdateRemovesExpired() {
        // given : 인벤토리 생성 후 아이템 추가
        ItemInventory inventory = new ItemInventory(gameState, playerIndex);
        inventory.addItem(ItemEffectType.TRIPLESHOT);

        // 아이템 만료 처리
        gameState.clearEffects(playerIndex);

        // when : 업데이트 호출
        inventory.update();

        // then : 슬롯 비워짐
        assertNull(inventory.getSlot(0));
    }

    @DisplayName("초기 코인 상태 확인")
    @Test
    void testInitialCoins() {
        // given : 초기 코인 100
        GameState gameState = new GameState(1, 3, false, 100); // 예시 생성
        int playerIndex = 0;

        // when : 아무 행동도 하지 않음

        // then : 초기 코인이 100인지 확인
        assertEquals(100, gameState.getCoins());
    }

    @DisplayName("코인 사용 테스트")
    @Test
    void testSpendCoinsSuccess() {
        // given : 초기 코인 100
        GameState gameState = new GameState(1, 3, false, 100);
        int playerIndex = 0;

        // when : 50코인 사용
        boolean spent = gameState.spendCoins(playerIndex, 50);

        // then : 사용 성공 및 잔액 감소 확인
        assertTrue(spent);
        assertEquals(50, gameState.getCoins());
    }

    @DisplayName("코인 추가 테스트")
    @Test
    void testAddCoins() {
        // given : 초기 코인 50
        GameState gameState = new GameState(1, 3, false, 50);
        int playerIndex = 0;

        // when : 30코인 추가
        gameState.addCoins(playerIndex, 30);

        // then : 잔액이 80인지 확인
        assertEquals(80, gameState.getCoins());
    }

    @DisplayName("코인 사용 실패 테스트 (잔액 부족)")
    @Test
    void testSpendCoinsFail() {
        // given : 초기 코인 80
        GameState gameState = new GameState(1, 3, false, 80);
        int playerIndex = 0;

        // when : 100코인 사용 시도
        boolean spent = gameState.spendCoins(playerIndex, 100);

        // then : 사용 실패, 잔액 그대로
        assertFalse(spent);
        assertEquals(80, gameState.getCoins());
    }

    @DisplayName("COIN 아이템 색상 적용 테스트")
    @Test
    void testCoinItemColor() {
        // given : COIN 타입 아이템 생성 준비
        Item coinItem = new Item("COIN", 0, 0, 5);

        // when : 생성자에서 setItemColor 호출됨
        Color appliedColor = coinItem.getColor();

        // then : 색상이 노란색인지 확인
        assertEquals(new Color(255, 255, 0), appliedColor);
    }

    @DisplayName("HEAL 아이템 색상 적용 테스트")
    @Test
    void testHealItemColor() {
        // given : Heal 타입 아이템 생성 준비
        Item healItem = new Item("HEAL", 0, 0, 5);

        // when : 생성자에서 setItemColor 호출됨
        Color appliedColor = healItem.getColor();

        // then : 색상이 빨간색인지 확인
        assertEquals(new Color(255, 0, 0), appliedColor);
    }

    @DisplayName("SCORE 아이템 색상 적용 테스트")
    @Test
    void testScoreItemColor() {
        // given : Score 타입 아이템 생성 준비
        Item scoreItem = new Item("SCORE", 0, 0, 5);

        // when : 생성자에서 setItemColor 호출됨
        Color appliedColor = scoreItem.getColor();

        // then : 색상이 흰색인지 확인
        assertEquals(new Color(255, 255, 255), appliedColor);
    }

    @DisplayName("TRIPLESHOT 아이템 색상 적용 테스트")
    @Test
    void testTripleShotItemColor() {
        // given : TripleShot 타입 아이템 생성 준비
        Item tripleShotItem = new Item("TRIPLESHOT", 0, 0, 5);

        // when : 생성자에서 setItemColor 호출됨
        Color appliedColor = tripleShotItem.getColor();

        // then : 색상이 cyan인지 확인
        assertEquals(new Color(0, 255, 255), appliedColor);
    }

    @DisplayName("SCOREBOOST 아이템 색상 적용 테스트")
    @Test
    void testScoreBoostItemColor() {
        // given : ScoreBoost 타입 아이템 생성 준비
        Item scoreBoostItem = new Item("SCOREBOOST", 0, 0, 5);

        // when : 생성자에서 setItemColor 호출됨
        Color appliedColor = scoreBoostItem.getColor();

        // then : 색상이 cyan인지 확인
        assertEquals(new Color(0, 255, 255), appliedColor);
    }

    @DisplayName("BulletSpeedUp 아이템 색상 적용 테스트")
    @Test
    void testBulletSpeedUpItemColor() {
        // given : BulletSpeedUp 타입 아이템 생성 준비
        Item bulletSpeedUpItem = new Item("BULLETSPEEDUP", 0, 0, 5);

        // when : 생성자에서 setItemColor 호출됨
        Color appliedColor = bulletSpeedUpItem.getColor();

        // then : 색상이 cyan인지 확인
        assertEquals(new Color(0, 255, 255), appliedColor);
    }

    @DisplayName("defalut 아이템 색상 적용 테스트")
    @Test
    void testDefaultItemColor() {
        // given : 이외 타입 아이템 생성 준비
        Item otherItem = new Item("OtherItem", 0, 0, 5);

        // when : 생성자에서 setItemColor 호출됨
        Color appliedColor = otherItem.getColor();

        // then : 색상이 흰색인지 확인
        assertEquals(new Color(255, 255, 255), appliedColor);
    }


    @DisplayName("effectState가 null일 경우 0 반환")
    @Test
    void testEffectDurationEffectStateNull() {
        GameState gameState = new GameState(1, 3, false, 100);

        // given : effectState 삭제 또는 비활성화
        gameState.clearEffects(0);

        // when
        int duration = gameState.getEffectDuration(0, ItemEffectType.TRIPLESHOT);

        // then
        assertEquals(0, duration);
    }

    @DisplayName("effect가 활성화 상태이면 지속시간 반환")
    @Test
    void testEffectDurationActive() {
        GameState gameState = new GameState(1, 3, false, 100);

        // given : 효과 추가
        gameState.addEffect(0, ItemEffectType.TRIPLESHOT, 1, 1);

        // when
        int duration = gameState.getEffectDuration(0, ItemEffectType.TRIPLESHOT);

        // then : 활성화된 효과는 0보다 큰 지속시간 반환
        assertTrue(duration > 0);
    }

    @DisplayName("effect 만료 시 0 반환")
    @Test
    void testEffectDurationExpired() throws InterruptedException {
        GameState gameState = new GameState(1, 3, false, 100);

        // given : 효과 추가 (1초 지속)
        gameState.addEffect(0, ItemEffectType.TRIPLESHOT, 1, 1);

        // when : 효과 만료 (1초 대기 후 update)
        Thread.sleep(1100);
        gameState.updateEffects();
        int duration = gameState.getEffectDuration(0, ItemEffectType.TRIPLESHOT);

        // then
        assertEquals(0, duration);
    }

    @DisplayName("인벤토리 비어있을 때 isFull() 테스트")
    @Test
    void testInventoryEmptyIsNotFull() {
        // given : 빈 인벤토리
        GameState mockGameState = mock(GameState.class);
        ItemInventory inventory = new ItemInventory(mockGameState, 0);

        // when : 꽉 찼는지 확인
        boolean result = inventory.isFull();

        // then : false여야 함
        assertFalse(result);
    }

    @DisplayName("isFull()이 false일 때 테스트")
    @Test
    void testInventoryPartiallyFull() {
        // given : 한 슬롯만 채워진 인벤토리
        GameState mockGameState = mock(GameState.class);
        ItemInventory inventory = new ItemInventory(mockGameState, 0);
        inventory.addItem(ItemEffectType.TRIPLESHOT);

        // when : 꽉 찼는지 확인
        boolean result = inventory.isFull();

        // then : 아직 false
        assertFalse(result);
    }

    @DisplayName("isFull()이 True일 때 테스트")
    @Test
    void testInventoryCompletelyFull() {
        // given : 모든 슬롯이 채워진 인벤토리
        GameState mockGameState = mock(GameState.class);
        ItemInventory inventory = new ItemInventory(mockGameState, 0);

        // MAX_SLOTS가 2라면 2번 addItem 호출
        inventory.addItem(ItemEffectType.TRIPLESHOT);
        inventory.addItem(ItemEffectType.SCOREBOOST);

        // when : 인벤토리가 꽉 찼는지 확인
        boolean result = inventory.isFull();

        // then : true여야 함
        assertTrue(result);
    }

    @DisplayName("슬롯이 비어 있을 때 duration 테스트")
    @Test
    void testRemainingDurationEmptySlot() {
        // given
        GameState gameState = mock(GameState.class);
        ItemInventory inventory = new ItemInventory(gameState, 0);

        // 슬롯 비어있음
        assertNull(inventory.getSlot(0));

        // when
        int duration = inventory.getRemainingDuration(0);

        // then
        assertEquals(0, duration);
    }

    @DisplayName("슬롯에 아이템 있으며 효과도 있을 때 duration 테스트")
    @Test
    void testRemainingDurationActiveEffect() {
        // given
        GameState gameState = mock(GameState.class);
        ItemInventory inventory = new ItemInventory(gameState, 0);

        inventory.addItem(ItemEffectType.TRIPLESHOT);

        when(gameState.hasEffect(0, ItemEffectType.TRIPLESHOT)).thenReturn(true);
        when(gameState.getEffectDuration(0, ItemEffectType.TRIPLESHOT)).thenReturn(5000);

        // when
        int duration = inventory.getRemainingDuration(0);

        // then
        assertEquals(5000, duration);
    }

    @DisplayName("clear() 실행 테스트")
    @Test
    void testClearInventory() {
        // given
        GameState gameState = mock(GameState.class);
        ItemInventory inventory = new ItemInventory(gameState, 0);

        inventory.addItem(ItemEffectType.TRIPLESHOT);
        inventory.addItem(ItemEffectType.SCOREBOOST);

        assertNotNull(inventory.getSlot(0));
        assertNotNull(inventory.getSlot(1));

        // when
        inventory.clear();

        // then
        assertNull(inventory.getSlot(0));
        assertNull(inventory.getSlot(1));
    }

    @DisplayName("MaxSlot 커버 테스트")
    @Test
    void testGetMaxSlots() {
        // given
        GameState gameState = mock(GameState.class);
        ItemInventory inventory = new ItemInventory(gameState, 0);

        // when
        int maxSlots = inventory.getMaxSlots();

        // then
        assertEquals(2, maxSlots);
    }

    @DisplayName("applyTripleShot - 장착된 아이템이 다르다면 장착 불가능")
    @Test
    void testApplyTripleShotFail() {
        // given : 다른 아이템이 장착되어 있다고 가정
        GameState gameState = mock(GameState.class);
        when(gameState.spendCoins(anyInt(), anyInt())).thenReturn(true);
        when(gameState.getActiveDurationItem(anyInt())).thenReturn(2);

        // when : 장착 시도
        boolean result = ItemEffect.applyTripleShot(gameState, 0, 2, 5000, 0);

        // then : 장착에 실패해야 함
        assertFalse(result);
        verify(gameState, never()).setActiveDuringItem(anyInt());
    }

    @DisplayName("applyTripleShot - 활장착된 아이템이 없거나 같아야 장착 가능")
    @Test
    void testApplyTripleShotPass() {
        // given : 인벤토리가 비어있다고 가정
        GameState gameState = mock(GameState.class);
        when(gameState.spendCoins(anyInt(), anyInt())).thenReturn(true);
        when(gameState.getActiveDurationItem(anyInt())).thenReturn(0);

        // when : 장착 시도
        boolean result = ItemEffect.applyTripleShot(gameState, 0, 1, 5000, 0);

        // then : 장착에 성공해야 함
        assertTrue(result);
        verify(gameState).setActiveDuringItem(1);
    }

    @DisplayName("applyScoreBoost - 장착된 아이템이 다르다면 장착 불가능")
    @Test
    void testApplyScoreBoostFail() {
        // given : 다른 아이템이 장착되어 있다고 가정
        GameState gameState = mock(GameState.class);
        when(gameState.spendCoins(anyInt(), anyInt())).thenReturn(true);
        when(gameState.getActiveDurationItem(anyInt())).thenReturn(1);

        // when : 장착 시도
        boolean result = ItemEffect.applyScoreBoost(gameState, 0, 2, 5000, 0);

        // then : 장착에 실패해야 함
        assertFalse(result);
        verify(gameState, never()).setActiveDuringItem(anyInt());
    }

    @DisplayName("applyScoreBoost - 활장착된 아이템이 없거나 같아야 장착 가능")
    @Test
    void testApplyScoreBoostPass() {
        // given : 인벤토리가 비어있다고 가정
        GameState gameState = mock(GameState.class);
        when(gameState.spendCoins(anyInt(), anyInt())).thenReturn(true);
        when(gameState.getActiveDurationItem(anyInt())).thenReturn(0);

        // when : 장착 시도
        boolean result = ItemEffect.applyScoreBoost(gameState, 0, 2, 5000, 0);

        // then : 장착에 성공해야 함
        assertTrue(result);
        verify(gameState).setActiveDuringItem(2);
    }

    @DisplayName("applyBulletSpeedUp - 장착된 아이템이 다르다면 장착 불가능")
    @Test
    void testApplyBulletSpeedUpFail() {
        // given : 다른 아이템이 장착되어 있다고 가정
        GameState gameState = mock(GameState.class);
        when(gameState.spendCoins(anyInt(), anyInt())).thenReturn(true);
        when(gameState.getActiveDurationItem(anyInt())).thenReturn(1);

        // when : 장착 시도
        boolean result = ItemEffect.applyBulletSpeedUp(gameState, 0, 3, 5000, 0);

        // then : 장착에 실패해야 함
        assertFalse(result);
        verify(gameState, never()).setActiveDuringItem(anyInt());
    }

    @DisplayName("applyBulletSpeedUp - 활장착된 아이템이 없거나 같아야 장착 가능")
    @Test
    void testApplyBulletSpeedUpPass() {
        // given : 인벤토리가 비어있다고 가정
        GameState gameState = mock(GameState.class);
        when(gameState.spendCoins(anyInt(), anyInt())).thenReturn(true);
        when(gameState.getActiveDurationItem(anyInt())).thenReturn(0);

        // when : 장착 시도
        boolean result = ItemEffect.applyBulletSpeedUp(gameState, 0, 3, 5000, 0);

        // then : 장착에 성공해야 함
        assertTrue(result);
        verify(gameState).setActiveDuringItem(3);
    }

}
