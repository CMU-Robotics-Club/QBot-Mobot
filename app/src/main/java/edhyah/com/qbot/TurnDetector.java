package edhyah.com.qbot;

/**
 * Detect general direction of turn
 */
public class TurnDetector {

    public enum Turn {RIGHT, LEFT, STRAIGHT};
    private static final double STRAIGHT_ANGLE = 0;
    private static final double ANGLE_EPSILON = 5;
    private static final int NUM_PAST_VAL = 25;
    private double[] mPastVals = new double[NUM_PAST_VAL];
    private int mCurInd = 0;
    private Turn mCurTurn = Turn.STRAIGHT;

    public Turn updateTurn(double curVal) {
        mPastVals[mCurInd] = curVal;
        mCurInd = (mCurInd + 1) % NUM_PAST_VAL;

        double avgVal = 0;
        for (int i = 0; i < NUM_PAST_VAL; i++) {
            avgVal += mPastVals[i];
        }
        avgVal = avgVal / NUM_PAST_VAL;

        Turn turn;
        if (avgVal < STRAIGHT_ANGLE - ANGLE_EPSILON) {
            turn = Turn.LEFT;
        } else if (avgVal > STRAIGHT_ANGLE + ANGLE_EPSILON) {
            turn = Turn.RIGHT;
        } else {
            turn = Turn.STRAIGHT;
        }
        mCurTurn = turn;
        return turn;
    }

    public Turn getCurTurn() {
        return mCurTurn;
    }


}
