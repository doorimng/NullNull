package engine;

public class BossTimer {

    private static final int BOSS_LEVEL = 6;

    private final TimeProvider timeProvider;
    private long startTime;
    private long endTime;
    private boolean isRunning;

    public BossTimer(TimeProvider timeProvider) {
        this.timeProvider = timeProvider;
        this.isRunning = false;
        this.startTime = 0;
        this.endTime = 0;
    }

    /**
     * 타이머 시작
     * TODO: 6스테이지(BOSS_LEVEL)일 때만 타이머가 시작되도록 구현해야 함
     */
    public void start(int currentLevel) {
        if(currentLevel == BOSS_LEVEL) {
            this.startTime = timeProvider.getCurrentTime();
            this.isRunning = true;
        }
    }

    /**
     * 타이머 종료
     * TODO: 실행 중일 때만 종료 시간을 기록하고 멈추도록 구현해야 함
     */
    public void stop() {
        if(this.isRunning) {
            this.endTime = timeProvider.getCurrentTime();
            this.isRunning = false;
        }
    }

    /**
     * 경과 시간 반환
     * TODO: 시작 시간과 종료 시간을 계산하여 반환해야 함
     */
    public long getDuration() {
        if(this.isRunning) {
            return timeProvider.getCurrentTime() - this.startTime;
        }
        if (startTime == 0) {
            return 0;
        }
        return endTime - startTime;
    }

    /**
     * 작동 여부 확인
     */
    public boolean isRunning() {
        return isRunning;
    }
}