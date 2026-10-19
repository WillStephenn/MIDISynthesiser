package synth.tests;

import synth.ui.EnvelopeAxis;

/**
 * Tests the envelope graph's time axis: segments tile the plot, forward and inverse mappings agree,
 * and a handle can never leave its segment. Run with: java -cp target/classes synth.tests.EnvelopeAxisTest
 */
public class EnvelopeAxisTest {

    private static final double PLOT_WIDTH = 200.0;
    private static final int[] TIME_SEGMENTS = {EnvelopeAxis.ATTACK, EnvelopeAxis.DECAY, EnvelopeAxis.RELEASE};
    private static int failures = 0;

    public static void main(String[] args) {
        testSegmentsTilePlot();
        testRangeEndsLandOnSegmentEnds();
        testForwardThenInverseRoundTrips();
        testInverseThenForwardRoundTrips();
        testPointerOutsideSegmentClamps();
        testShortTimesGetRealWidth();
        testSliderTaperMatchesGraph();
        testTimeLabels();

        if (failures > 0) {
            System.err.println(failures + " test(s) FAILED.");
            System.exit(1);
        }
        System.out.println("All envelope axis tests passed.");
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            failures++;
            System.err.println("FAIL: " + message);
        }
    }

    private static boolean near(double a, double b, double tolerance) {
        return Math.abs(a - b) <= tolerance;
    }

    private static void testSegmentsTilePlot() {
        check(near(EnvelopeAxis.segmentStart(EnvelopeAxis.ATTACK, PLOT_WIDTH), 0, 1e-9), "attack starts at 0");
        check(near(EnvelopeAxis.segmentEnd(EnvelopeAxis.RELEASE, PLOT_WIDTH), PLOT_WIDTH, 1e-9), "release ends at plot width");
        for (int i = 0; i < 3; i++) {
            check(near(EnvelopeAxis.segmentEnd(i, PLOT_WIDTH), EnvelopeAxis.segmentStart(i + 1, PLOT_WIDTH), 1e-9), "segments touch at " + i);
        }
        double hold = EnvelopeAxis.segmentEnd(EnvelopeAxis.HOLD, PLOT_WIDTH) - EnvelopeAxis.segmentStart(EnvelopeAxis.HOLD, PLOT_WIDTH);
        double attack = EnvelopeAxis.segmentEnd(EnvelopeAxis.ATTACK, PLOT_WIDTH) - EnvelopeAxis.segmentStart(EnvelopeAxis.ATTACK, PLOT_WIDTH);
        check(hold < attack, "sustain hold is the narrow segment");
    }

    private static void testRangeEndsLandOnSegmentEnds() {
        for (int s : TIME_SEGMENTS) {
            check(near(EnvelopeAxis.timeToX(s, EnvelopeAxis.T_MIN, PLOT_WIDTH), EnvelopeAxis.segmentStart(s, PLOT_WIDTH), 1e-9), "tmin at start of segment " + s);
            check(near(EnvelopeAxis.timeToX(s, EnvelopeAxis.T_MAX, PLOT_WIDTH), EnvelopeAxis.segmentEnd(s, PLOT_WIDTH), 1e-9), "tmax at end of segment " + s);
        }
    }

    private static void testForwardThenInverseRoundTrips() {
        for (int s : TIME_SEGMENTS) {
            for (int k = 0; k <= 200; k++) {
                double t = EnvelopeAxis.T_MIN * Math.pow(EnvelopeAxis.T_MAX / EnvelopeAxis.T_MIN, k / 200.0);
                double back = EnvelopeAxis.xToTime(s, EnvelopeAxis.timeToX(s, t, PLOT_WIDTH), PLOT_WIDTH);
                check(near(back / t, 1.0, 1e-9), "time round trip " + t + " -> " + back);
            }
        }
    }

    private static void testInverseThenForwardRoundTrips() {
        for (int s : TIME_SEGMENTS) {
            double x0 = EnvelopeAxis.segmentStart(s, PLOT_WIDTH);
            double x1 = EnvelopeAxis.segmentEnd(s, PLOT_WIDTH);
            for (int k = 0; k <= 50; k++) {
                double x = x0 + (x1 - x0) * k / 50.0;
                check(near(EnvelopeAxis.timeToX(s, EnvelopeAxis.xToTime(s, x, PLOT_WIDTH), PLOT_WIDTH), x, 1e-7), "x round trip at " + x);
            }
        }
    }

    private static void testPointerOutsideSegmentClamps() {
        for (int s : TIME_SEGMENTS) {
            double x0 = EnvelopeAxis.segmentStart(s, PLOT_WIDTH);
            double x1 = EnvelopeAxis.segmentEnd(s, PLOT_WIDTH);
            check(near(EnvelopeAxis.xToTime(s, x0 - 500, PLOT_WIDTH), EnvelopeAxis.T_MIN, 1e-12), "far left clamps to tmin");
            check(near(EnvelopeAxis.xToTime(s, x1 + 500, PLOT_WIDTH), EnvelopeAxis.T_MAX, 1e-12), "far right clamps to tmax");
            check(EnvelopeAxis.timeToX(s, 1e6, PLOT_WIDTH) <= x1 + 1e-9, "huge time stays in segment");
            check(EnvelopeAxis.timeToX(s, -3, PLOT_WIDTH) >= x0, "negative time stays in segment");
            check(EnvelopeAxis.timeToX(s, Double.NaN, PLOT_WIDTH) >= x0, "NaN time stays in segment");
        }
    }

    private static void testShortTimesGetRealWidth() {
        // The 1 to 50 ms attacks that matter musically take at least a third of the segment
        check(EnvelopeAxis.timeToFraction(0.05) > 0.3, "50 ms is more than 30% across");
        check(near(EnvelopeAxis.timeToFraction(0.01) - EnvelopeAxis.timeToFraction(0.001),
                   EnvelopeAxis.timeToFraction(1.0) - EnvelopeAxis.timeToFraction(0.1), 1e-9), "every decade is the same width");
    }

    private static void testSliderTaperMatchesGraph() {
        check(near(EnvelopeAxis.positionToTime(0.0), EnvelopeAxis.T_MIN, 1e-12), "slider min is 1 ms");
        check(near(EnvelopeAxis.positionToTime(1.0), EnvelopeAxis.T_MAX, 1e-12), "slider max is 5 s");
        check(near(EnvelopeAxis.positionToTime(0.5), Math.sqrt(EnvelopeAxis.T_MIN * EnvelopeAxis.T_MAX), 1e-12), "slider midpoint is the geometric mean");
        for (double t : new double[] {0.001, 0.005, 0.1, 0.4, 3.0, 5.0}) {
            check(near(EnvelopeAxis.positionToTime(EnvelopeAxis.timeToPosition(t)) / t, 1.0, 1e-9), "slider round trip " + t);
        }
    }

    private static void testTimeLabels() {
        check(EnvelopeAxis.formatTime(0.005).equals("5ms"), "5 ms label");
        check(EnvelopeAxis.formatTime(0.4).equals("400ms"), "400 ms label");
        check(EnvelopeAxis.formatTime(1.0).equals("1.0s"), "1 s label");
        check(EnvelopeAxis.formatTime(5.0).equals("5.0s"), "5 s label");
    }
}
