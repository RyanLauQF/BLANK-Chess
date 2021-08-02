public class Clock {
    private long timeControl;
    private long endTime;
    private boolean isClockStarted;

    /**
     * Constructor
     */
    public Clock(){
        this.timeControl = 5;   // default time control is 5 seconds
        this.isClockStarted = false;
        this.endTime = 0;
    }

    /**
     * Sets the end timing to start time + duration specified
     */
    public void start(){
        endTime = System.currentTimeMillis() + convertTimeToMs(timeControl);
        isClockStarted = true;
    }

    /**
     * Checks if current time has exceeded end time after clock has started
     * @return true if it has, else return false
     */
    public boolean isTimeUp(){
        if(!isClockStarted){
            return false;   // clock has not been started
        }
        long currentTime = System.currentTimeMillis();
        return endTime - currentTime < 0;
    }

    /**
     * Sets the time duration for each turn
     * @param timeControl refers to the duration per turn on the clock
     */
    public void setTimeControl(long timeControl){
        this.timeControl = timeControl;
        isClockStarted = false; // resets the clock
    }

    /**
     * Converts time in seconds to milliseconds (10 ^ 3)
     * @param timeInSeconds refers to the time measured in seconds
     * @return the converted time in milliseconds
     */
    private static long convertTimeToMs(long timeInSeconds){
        return timeInSeconds * 1000;
    }
}
