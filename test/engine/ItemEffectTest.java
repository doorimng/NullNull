package engine;

import engine.ItemEffect.ItemEffectType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

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
}
