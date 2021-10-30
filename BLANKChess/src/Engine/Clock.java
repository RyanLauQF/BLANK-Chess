public class Clock {
    private double startTime;
    private double endTime;
    private double remainingTime;
    private double timeIncrementPerMove;

    private boolean isClockStarted;

    // account for lag time per move in milliseconds (when breaking out of search / processing inputs)
    private static final int LAG_TIME = 150;

    // divides the total time left by 30.
    private static final int MOVES_TO_GO = 30;

    /**
     * Constructor
     */
    public Clock() {
        this.isClockStarted = false;
        this.startTime = 0;
        this.endTime = 0;
        this.remainingTime = 0;
        this.timeIncrementPerMove = 0;
    }

    /**
     * Sets the time duration for the clock to run
     *
     * @param duration refers to the time set on clock to run (in seconds)
     */
    public void setTime(double duration) {
        this.remainingTime = convertTimeToMs(duration);

        // resets the clock
        isClockStarted = false;
    }

    /**
     * Used for managing time control in a chess game
     * @param gameDuration refers to the total time allocated to each player for the game (in seconds)
     * @param incrementPerMove refers to the bonus time awarded to player after each move (in seconds)
     */
    public void setTimeControl(double gameDuration, double incrementPerMove){
        setTime(gameDuration);
        this.timeIncrementPerMove = convertTimeToMs(incrementPerMove);
    }

    /**
     * Sets the end timing to start time + remaining time
     */
    public void start() {
        startTime = System.currentTimeMillis();
        endTime = startTime + remainingTime;
        isClockStarted = true;
    }

    public void pause(){
        if(!isTimeUp()){
            remainingTime = getRemainingTime();
        }
    }

    public void incrementTime(){
        remainingTime += timeIncrementPerMove;
    }

    /**
     * Checks if current time has exceeded end time after clock has started
     *
     * @return true if it has, else return false
     */
    public boolean isTimeUp() {
        if (!isClockStarted) {
            return false;   // clock has not been started
        }
        double currentTime = System.currentTimeMillis();
        return endTime - currentTime < 0;
    }

    /**
     * Calculates the remaining time left on the clock
     *
     * @return the remaining time in milliseconds
     */
    public double getRemainingTime() {
        if (isTimeUp()) {
            return 0;
        }
        double currentTime = System.currentTimeMillis();
        return endTime - currentTime;
    }

    /**
     * @return start time of the clock. If the time has not started, return 0;
     */
    public double getStartTime() {
        if (!isClockStarted) {
            return 0;   // clock has not been started
        }
        return startTime;
    }

    /**
     * Gets the time per move allocated to an AI based on time left and bonus timing (if any)
     * @param totalTimeLeft refers to the time left for the AI (in seconds)
     * @param incrementPerMove refers to the bonus time awarded at the end of each turn (in seconds)
     * @return the time allocated for the AI to think based on the time conditions (seconds)
     */
    public static double getTimePerMove(double totalTimeLeft, double incrementPerMove) {
        totalTimeLeft = convertTimeToMs(totalTimeLeft);
        incrementPerMove = convertTimeToMs(incrementPerMove);

        totalTimeLeft -= LAG_TIME;
        double timePerMove = (totalTimeLeft / MOVES_TO_GO) + incrementPerMove;

        // ensure that the AI does not lose based on time
        if(incrementPerMove > 0 && totalTimeLeft < (5 * incrementPerMove)){
            // use only 75% of the increment time to play each move to account for lag time
            return ((incrementPerMove * 75) / 100) / 1000;
        }

        return timePerMove / 1000;
    }

    /**
     * Converts time in seconds to milliseconds (10 ^ 3)
     *
     * @param timeInSeconds refers to the time measured in seconds
     * @return the converted time in milliseconds
     */
    private static double convertTimeToMs(double timeInSeconds) {
        return timeInSeconds * 1000;
    }
}
