package synth.core;

import synth.utils.AudioConstants;

public interface AudioComponent {
    /**
     * Processes a block of audio samples.
     *
     * @param inputBuffer An array containing the input signal. Can be null if the component generates its own signal (like an oscillator).
     * @param outputBuffer The array to write the processed output signal to.
     * @param blockSize The number of samples to process in this block.
     */
    void processBlock(double[] inputBuffer, double[] outputBuffer, int blockSize);
}
