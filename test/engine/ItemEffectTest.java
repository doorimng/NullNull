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
    private final int PLAYER_ID = 1 ;
    private final int PLAYER_INDEX = 0 ;

    @BeforeEach
    void setUp() {
        mockGameState = mock(GameState.class) ;
    }

    /**
     * [인벤토리 아이템 표시 UI]
     * 인벤토리에 아이템이 없을 때 획득했을 경우 */
    @DisplayName("첫 아이템 획득 후 등록 로직")
    @Test
    void GetFirstItemTest() {
        // given : 기존에 아이템이 없다고 가정
        when(mockGameState.hasActiveDurationItem(PLAYER_INDEX)).thenReturn(false);

        // when : 아이템을 얻음
        boolean result = ItemEffect.applyTripleShot(mockGameState, PLAYER_ID, 1, 10);

        // then : 아이템을 획득해야 함 (획득 결과가 True)
        assertTrue(result, "아이템 획득에 성공해야 합니다.");
        verify(mockGameState, times(1)).addEffect(PLAYER_INDEX, ItemEffect.ItemEffectType.TRIPLESHOT, 1, 10);
    }

    /**
     * [인벤토리 아이템 표시 UI]
     * 인벤토리에 아이템이 있을 때 획득했을 경우 */
    @DisplayName("이후 아이템 획득 후 등록 로직")
    @Test
    void GetNextItemTest() {
        // given : 기존에 아이템이 있다고 가정
        when(mockGameState.hasActiveDurationItem(PLAYER_INDEX)).thenReturn(true) ;

        // when : 아이템을 얻음
        boolean result = ItemEffect.applyTripleShot(mockGameState, PLAYER_ID, 1, 10);

        // then : 추가 아이템을 획득하지 않아야 함 (획득 결과가 False)
        assertFalse(result, "아이템이 이미 있을 때는 획득에 실패해야 합니다.");
        verify(mockGameState, never()).addEffect(anyInt(), any(ItemEffectType.class), anyInt(), anyInt());
    }

    /**
     * [인벤토리 아이템 표시 UI]
     * 아이템을 사용했을 경우 */
    @DisplayName("아이템 사용 후 소멸 로직")
    @Test
    void UseItemTest() {
        // given : 기존에 아이템이 있다고 가정
        when(mockGameState.hasActiveDurationItem(PLAYER_INDEX)).thenReturn(true) ;

        // when : 아이템을 사용함
        boolean result = ItemEffect.useItem(mockGameState, PLAYER_ID, 1) ;

        // then : 아이템이 사용되었어야 함
        assertTrue(result, "아이템을 사용했다면 없어져야 합니다.");
        verify(mockGameState, never()).addEffect(anyInt(), any(ItemEffectType.class), anyInt(), anyInt());
    }

}