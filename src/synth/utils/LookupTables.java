package synth.utils;

/**
 * Pre-Computes expensive math functions on startup in order to optimise real-time performance.
 */
public class LookupTables {

    public static final int TABLE_SIZE = AudioConstants.LOOKUP_TABLE_SIZE;

    public static final double[] SINE = new double[TABLE_SIZE];
    public static final double[] SQUARE = new double[TABLE_SIZE];
    public static final double[] SAW = new double[TABLE_SIZE];
    public static final double[] TRIANGLE = new double[TABLE_SIZE];
    public static final double[] TAN_TABLE = new double[TABLE_SIZE];

    static {
        // Sine Table
        for (int i = 0; i < TABLE_SIZE; i++) {
            SINE[i] = Math.sin(2.0 * Math.PI * (double) i / TABLE_SIZE);
        }

        // Square Table
        for (int i = 0; i < TABLE_SIZE; i++) {
            SQUARE[i] = (i < TABLE_SIZE / 2) ? 1.0 : -1.0;
        }

        // Saw Table (from -1 to 1)
        for (int i = 0; i < TABLE_SIZE; i++) {
            SAW[i] = 2.0 * ((double) i / TABLE_SIZE) - 1.0;
        }

        // Triangle Table (from -1 to 1)
        for (int i = 0; i < TABLE_SIZE; i++) {
            double value = 2.0 * ((double) i / TABLE_SIZE);
            if (value > 1.0) {
                value = 2.0 - value;
            }
            TRIANGLE[i] = 2.0 * value - 1.0;
        }

        // Tan Table (from 0 to PI)
        for (int i = 0; i < TABLE_SIZE; i++) {
            TAN_TABLE[i] = Math.tan(Math.PI * (double) i / TABLE_SIZE);
        }
    }
}
