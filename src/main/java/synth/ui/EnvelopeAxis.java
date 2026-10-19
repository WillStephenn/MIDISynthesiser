package synth.ui;

import java.util.Locale;

/**
 * Time axis shared by the envelope graph and the envelope time sliders: log time inside
 * fixed-width segments, so no handle can leave the plot and moving one never moves another's time.
 * Pure maths (no JavaFX) so it can be tested without a toolkit. The site and the Rust port use the
 * same layout and constants.
 */
public final class EnvelopeAxis {

    /** Attack, decay, sustain-hold and release segments. */
    public static final int ATTACK = 0, DECAY = 1, HOLD = 2, RELEASE = 3;

    /** The A/D/R parameter range in seconds. */
    public static final double T_MIN = 0.001, T_MAX = 5.0;

    /** Share of the plot width taken by each segment (sums to 1). */
    private static final double[] SEGMENT_FRACTION = {0.28, 0.28, 0.16, 0.28};

    private EnvelopeAxis() {}

    /** Time to 0..1 along a time segment (log, clamped to the parameter range). */
    public static double timeToFraction(double t) {
        if (!(t > T_MIN)) return 0.0;
        return Math.min(1.0, Math.log(t / T_MIN) / Math.log(T_MAX / T_MIN));
    }

    /** Exact inverse of {@link #timeToFraction}. */
    public static double fractionToTime(double f) {
        double c = Math.max(0.0, Math.min(1.0, f));
        return T_MIN * Math.pow(T_MAX / T_MIN, c);
    }

    /** Start offset of a segment within a plot of width {@code plotWidth}. */
    public static double segmentStart(int segment, double plotWidth) {
        double x = 0;
        for (int i = 0; i < segment; i++) x += SEGMENT_FRACTION[i] * plotWidth;
        return x;
    }

    /** End offset of a segment within a plot of width {@code plotWidth}. */
    public static double segmentEnd(int segment, double plotWidth) {
        return segmentStart(segment, plotWidth) + SEGMENT_FRACTION[segment] * plotWidth;
    }

    /** Handle x offset (0..plotWidth) for a time in a time segment. */
    public static double timeToX(int segment, double t, double plotWidth) {
        double x0 = segmentStart(segment, plotWidth);
        return x0 + (segmentEnd(segment, plotWidth) - x0) * timeToFraction(t);
    }

    /** Time for an x offset in a time segment, clamped to the segment (inverse of {@link #timeToX}). */
    public static double xToTime(int segment, double x, double plotWidth) {
        double x0 = segmentStart(segment, plotWidth);
        return fractionToTime((x - x0) / (segmentEnd(segment, plotWidth) - x0));
    }

    /** Slider position (0..1) for a time: the same taper as the graph. */
    public static double timeToPosition(double t) { return timeToFraction(t); }

    /** Time in seconds for a slider position (0..1). */
    public static double positionToTime(double p) { return fractionToTime(p); }

    /** Short label for a time: "5ms", "250ms", "1.2s". */
    public static String formatTime(double t) {
        return t < 0.9995 ? Math.round(t * 1000) + "ms" : String.format(Locale.ROOT, "%.1fs", t);
    }
}
