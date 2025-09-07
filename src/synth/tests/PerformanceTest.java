package synth.tests;

import synth.core.Synthesiser;
import synth.utils.AudioConstants;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class PerformanceTest {

    public static void main(String[] args) {
        // Setup
        int numberOfBlocksToProcess = 2000;
        Synthesiser synth = new Synthesiser(
                AudioConstants.NUMBER_OF_VOICES, //
                AudioConstants.SAMPLE_RATE,     //
                AudioConstants.BLOCK_SIZE);     //

        double[] audioBlock = new double[AudioConstants.BLOCK_SIZE * 2];
        Map<String, Long> totalTimings = new HashMap<>();

        // Activate voices
        System.out.println("Activating " + AudioConstants.NUMBER_OF_VOICES + " voices for the test...");
        for (int i = 0; i < AudioConstants.NUMBER_OF_VOICES; i++) {
            synth.noteOn((byte) (60 + i), 1.0);
        }

        // Run processing loop
        System.out.println("Processing " + numberOfBlocksToProcess + " audio blocks...");
        long totalTestTime = 0;
        for (int i = 0; i < numberOfBlocksToProcess; i++) {
            long blockStartTime = System.nanoTime();
            Map<String, Long> blockTimings = synth.processBlockInstrumented(audioBlock);
            long blockEndTime = System.nanoTime();
            totalTestTime += (blockEndTime - blockStartTime);

            // Aggregate timings from this block into the total
            blockTimings.forEach((key, value) -> totalTimings.merge(key, value, Long::sum));
        }
        System.out.println("Processing complete.\n");


        // Print results
        System.out.println("--- Synthesiser Performance Test Results ---");
        System.out.println("Total processing time: " + TimeUnit.NANOSECONDS.toMillis(totalTestTime) + " ms");
        System.out.println("\n--- Average Time Per Stage (in microseconds) ---");

        totalTimings.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed()) // Sort by time, descending
                .forEach(entry -> {
                    long timeInNanos = entry.getValue();
                    String stage = entry.getKey();
                    long averageTime = timeInNanos / numberOfBlocksToProcess;
                    System.out.printf("%-25s: %d µs%n", stage, TimeUnit.NANOSECONDS.toMicros(averageTime));
                });
        System.out.println("------------------------------------------");
    }
}