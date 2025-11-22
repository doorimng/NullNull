package engine;

import engine.ItemEffect.ItemEffectType ;

import org.junit.jupiter.api.BeforeEach ;
import org.junit.jupiter.api.Test ;
import org.junit.jupiter.api.DisplayName ;
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
    @DisplayName("아이템이 없을 때는 사용 실패해야 함")
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
    @DisplayName("첫 아이템 획득 후 등록 로직")
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
    @DisplayName("이후 아이템 획득 후 등록 로직")
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
    @DisplayName("이후 아이템 획득 후 다른 아이템 등록 로직")
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
    @DisplayName("아이템 사용 후 소멸 로직")
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

}