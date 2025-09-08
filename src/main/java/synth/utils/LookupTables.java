package synth.utils;

/**
 * Pre-Computes expensive math functions on startup in order to optimise real-time performance.
 */
public class LookupTables {

    public static final int TABLE_SIZE = AudioConstants.LOOKUP_TABLE_SIZE;
    public static final int RESONANCE_STEPS = 128;

    public static final double[] SINE = new double[TABLE_SIZE];
    public static final double[] COSINE = new double[TABLE_SIZE];
    public static final double[] SQUARE = new double[TABLE_SIZE];
    public static final double[] SAW = new double[TABLE_SIZE];
    public static final double[] TRIANGLE = new double[TABLE_SIZE];
    public static final double[] TAN_TABLE = new double[TABLE_SIZE];

    // Filter Coefficients
    public static final double[][] A1_TABLE = new double[TABLE_SIZE][RESONANCE_STEPS];
    public static final double[][] A2_TABLE = new double[TABLE_SIZE][RESONANCE_STEPS];
    public static final double[][] A3_TABLE = new double[TABLE_SIZE][RESONANCE_STEPS];

    // Midi to pitch
    public static final double[] MIDI_TO_HZ = new double[128];

    static {
        System.out.println("Pre-computing LUTs... (This may take a moment)");
        // Sine Table
        for (int i = 0; i < TABLE_SIZE; i++) {
            SINE[i] = Math.sin(2.0 * Math.PI * (double) i / TABLE_SIZE);
        }

        // Cosine Table
        for (int i = 0; i < TABLE_SIZE; i++) {
            COSINE[i] = Math.cos(2.0 * Math.PI * (double) i / TABLE_SIZE);
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

        // Filter Coefficients
        for (int cutoffIndex = 0; cutoffIndex < TABLE_SIZE; cutoffIndex++) {
            double g = TAN_TABLE[cutoffIndex];

            for (int resIndex = 0; resIndex < RESONANCE_STEPS; resIndex++) {
                // Map the index to a resonance value (from 1.0 to 20.0)
                double resonanceQ = 1.0 + (resIndex / (double)(RESONANCE_STEPS - 1)) * 19.0;
                double k = 1.0 / resonanceQ;

                // Calculate Filter Coefficients
                double a1 = 1.0 / (1.0 + g * (g + k));
                double a2 = g * a1;
                double a3 = g * a2;

                // Store the results in 2D LUTs
                A1_TABLE[cutoffIndex][resIndex] = a1;
                A2_TABLE[cutoffIndex][resIndex] = a2;
                A3_TABLE[cutoffIndex][resIndex] = a3;
            }
        }

        // Midi not to pitch
        for (int i = 0; i < 128; i++) {
            MIDI_TO_HZ[i] = 440.0 * Math.pow(2.0, (i - 69) / 12.0);
        }
        System.out.println("LUT pre-computation complete.");
    }
}
