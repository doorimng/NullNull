package engine;

import engine.ItemEffect.ItemEffectType ;

import entity.Item;
import org.junit.jupiter.api.BeforeEach ;
import org.junit.jupiter.api.Test ;
import org.junit.jupiter.api.DisplayName ;

import java.awt.*;

import static org.junit.jupiter.api.Assertions.* ;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.*;

public class ItemEffectTest {

    private GameState mockGameState ;
    private final int playerId = 1 ;
    private final int playerIndex = 0 ;

    @BeforeEach
    void setUp() {
        mockGameState = mock(GameState.class) ;
    }

    /**
     * [인벤토리 아이템 표시 UI]
     * 인벤토리에 아이템이 없을 때 사용할 경우 */
    @DisplayName("아이템 없을 때 사용 불가 테스트")
    @Test
    void UseItemFailTest() {
        // given : 기존에 아이템이 없다고 가정
        when(mockGameState.getActiveDurationItem(playerIndex)).thenReturn(0);

        // when : 아이템 사용 시도
        boolean result = ItemEffect.useItem(mockGameState, playerId);

        // then : 사용 실패해야 함
        assertFalse(result, "아이템이 없으면 사용될 수 없습니다.");
    }

    /**
     * [인벤토리 아이템 표시 UI]
     * 인벤토리에 아이템이 없을 때 획득했을 경우 */
    @DisplayName("첫 아이템 획득 후 등록 테스트")
    @Test
    void GetFirstItemTest() {
        // given : 기존에 아이템이 없다고 가정
        when(mockGameState.getActiveDurationItem(playerIndex)).thenReturn(0);

        // when : 아이템을 얻음
        boolean result = ItemEffect.applyTripleShot(mockGameState, playerId, 1, 10);

        // then : 아이템을 획득해야 함 (획득 결과가 True)
        assertTrue(result, "아이템 획득에 성공해야 합니다.");
        verify(mockGameState, times(1)).addEffect(playerIndex, ItemEffect.ItemEffectType.TRIPLESHOT, 1, 10);
    }

    /**
     * [인벤토리 아이템 표시 UI]
     * 인벤토리에 아이템이 있을 때 획득했을 경우 */
    @DisplayName("이후 아이템 획득 후 등록 테스트")
    @Test
    void GetNextSameItemTest() {
        // given : 기존에 1번 아이템이 있다고 가정
        when(mockGameState.getActiveDurationItem(playerIndex)).thenReturn(1) ;

        // when : 1번 아이템을 얻음
        boolean result = ItemEffect.applyTripleShot(mockGameState, playerId, 1, 10);

        // then : 추가 아이템을 획득해야 함 (획득 결과가 True)
        assertTrue(result, "아이템이 이미 있고 그 아이템과 똑같은 아이템을 얻었다면 획득에 성공해야 합니다.");
    }

    /**
     * [인벤토리 아이템 표시 UI]
     * 인벤토리에 아이템이 있을 때 그 아이템과 다른 아이템을 획득했을 경우 */
    @DisplayName("이후 아이템 획득 후 다른 아이템 등록 테스트")
    @Test
    void GetNextDifferentItemTest() {
        // given : 기존에 2번 아이템이 있다고 가정
        when(mockGameState.getActiveDurationItem(playerIndex)).thenReturn(2) ;

        // when : 1번 아이템을 얻음
        boolean result = ItemEffect.applyTripleShot(mockGameState, playerId, 1, 10);

        // then : 추가 아이템을 획득하지 않아야 함 (획득 결과가 False)
        assertFalse(result, "아이템이 이미 있고 그 아이템과 다른 아이템을 얻었다면 획득에 실패해야 합니다.");
    }

    /**
     * [인벤토리 아이템 표시 UI]
     * 아이템을 사용했을 경우 */
    @DisplayName("아이템 사용 후 소멸 테스트")
    @Test
    void UseItemTest() {
        // given : 기존에 아이템이 있다고 가정
        when(mockGameState.getActiveDurationItem(playerIndex)).thenReturn(1) ;

        // when : 아이템을 사용함
        boolean result = ItemEffect.useItem(mockGameState, playerId) ;

        // then : 아이템이 사용되었어야 함
        assertTrue(result, "아이템을 사용했다면 없어져야 합니다.");
        verify(mockGameState, never()).addEffect(anyInt(), any(ItemEffectType.class), anyInt(), anyInt());
    }

    /**
     * [인벤토리 아이템 표시 UI]
     * 아이템을 사용했을 경우 */
    @DisplayName("아이템 추가 테스트")
    @Test
    void testAddItem() {
        // given : 인벤토리가 비어있다고 가정
        GameState gameState = mock(GameState.class);
        ItemInventory itemInventory = new ItemInventory(gameState, 0);

        // when : 아이템을 얻음
        boolean added = itemInventory.addItem(ItemEffectType.TRIPLESHOT);

        // then : 아이템이 추가되었어야 함
        assertTrue(added);
        assertEquals(ItemEffectType.TRIPLESHOT, itemInventory.getSlot(0));
    }

    /**
     * [인벤토리 아이템 표시 UI]
     * 아이템을 사용했을 경우 */
    @DisplayName("아이템 사용 테스트")
    @Test
    void testUpdateRemovesExpired() {
        // given : 아이템이 이미 있다고 가정
        GameState gameState = mock(GameState.class);
        ItemInventory itemInventory = new ItemInventory(gameState, 0);
        itemInventory.addItem(ItemEffectType.TRIPLESHOT);
        when(gameState.hasEffect(0, ItemEffectType.TRIPLESHOT)).thenReturn(false); // 만료된 상태

        // when : 아이템이 만료됨
        itemInventory.update();

        // then : 아이템이 없어야 함
        assertNull(itemInventory.getSlot(0));
    }

    @DisplayName("인벤토리 포화 확인 테스트")
    @Test
    void testInventoryFull() {
        // given : 인벤토리가 꽉 찬 상태라고 가정
        GameState gameState = mock(GameState.class);
        ItemInventory itemInventory = new ItemInventory(gameState, 0);
        itemInventory.addItem(ItemEffectType.TRIPLESHOT);
        itemInventory.addItem(ItemEffectType.SCOREBOOST);

        // when : 아이템이 꽉 찼는지 확인
        boolean result = itemInventory.isFull();

        // then : true 반환해야 함
        assertTrue(result);
    }

    @DisplayName("아이템 색 변경 테스트")
    @Test
    void testItemColorSet() {
        // given : TRIPLESHOT 아이템을 사용했다고 가정
        Item item = new Item("TRIPLESHOT", 0, 0, 1);

        // when : 사용했을 때의 색 가져오기
        Color color = item.getColor();

        // then : cyan색으로 지정되었어야 함
        assertEquals(Color.cyan, color);
    }

    @DisplayName("아이템 고유값 반환 테스트")
    @Test
    void testActiveItemId() {
        // given : GameState mock 생성, 1번 아이템 반환하도록 설정
        GameState mockGameState = mock(GameState.class);
        when(mockGameState.getActiveDurationItem(anyInt())).thenReturn(1);

        // when : 아이템 고유값 조회
        int itemId = mockGameState.getActiveDurationItem(0);

        // then : 반환값이 1인지 확인
        assertEquals(1, itemId);
    }

    @DisplayName("아이템 지속시간 반환 테스트")
    @Test
    void testItemDuration() {
        // given : TRIPLESHOT 지속시간 5000ms 이라고 가정
        GameState mockGameState = mock(GameState.class);
        when(mockGameState.getEffectDuration(anyInt(), eq(ItemEffectType.TRIPLESHOT))).thenReturn(5000);

        // when : 지속시간 조회
        int duration = mockGameState.getEffectDuration(0, ItemEffectType.TRIPLESHOT);

        // then : 5000ms가 반환되는지 확인
        assertEquals(5000, duration);
    }

    @DisplayName("아이템 상태 유지 확인 테스트")
    @Test
    void testItemRemainsWhenActive() {
        // given : 아이템 TRIPLESHOT이 사용되었다고 가정
        GameState mockGameState = mock(GameState.class);
        when(mockGameState.hasEffect(anyInt(), eq(ItemEffectType.TRIPLESHOT))).thenReturn(true);
        ItemInventory inventory = new ItemInventory(mockGameState, 0);
        inventory.addItem(ItemEffectType.TRIPLESHOT);

        // when : 아이템 업데이트
        inventory.update();

        // then : 슬롯에 아이템이 남아 있어야 함
        assertEquals(ItemEffectType.TRIPLESHOT, inventory.getSlot(0));
    }

    @DisplayName("아이템 만료 시 제거 확인 테스트")
    @Test
    void testItemRemovedWhenExpired() {
        // given : 아이템 TRIPLESHOT이 만료 상태라고 가정
        GameState mockGameState = mock(GameState.class);
        when(mockGameState.hasEffect(anyInt(), eq(ItemEffectType.TRIPLESHOT))).thenReturn(false);
        ItemInventory inventory = new ItemInventory(mockGameState, 0);
        inventory.addItem(ItemEffectType.TRIPLESHOT);

        // when : 아이템 업데이트
        inventory.update();

        // then : 슬롯이 비워져야 함
        assertNull(inventory.getSlot(0));
    }

}