package synth.tests;

import synth.core.Synthesiser;
import synth.utils.AudioConstants;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class PerformanceTest {

    public static void main(String[] args) {
        runStandardTest();
        System.out.println("\n");
        runContentionStressTest();
    }

    private static void runStandardTest() {
        // Setup
        int numberOfBlocksToProcess = 2000;
        Synthesiser synth = new Synthesiser(
                AudioConstants.NUMBER_OF_VOICES,
                AudioConstants.SAMPLE_RATE,
                AudioConstants.BLOCK_SIZE);

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

    private static void runContentionStressTest() {
        int numberOfBlocksToProcess = 2000;
        Synthesiser synth = new Synthesiser(
                AudioConstants.NUMBER_OF_VOICES,
                AudioConstants.SAMPLE_RATE,
                AudioConstants.BLOCK_SIZE);

        double[] audioBlock = new double[AudioConstants.BLOCK_SIZE * 2];
        Map<String, Long> totalTimings = new HashMap<>();

        // Activate voices
        System.out.println("=== Contention Stress Test ===");
        System.out.println("Activating " + AudioConstants.NUMBER_OF_VOICES + " voices...");
        for (int i = 0; i < AudioConstants.NUMBER_OF_VOICES; i++) {
            synth.noteOn((byte) (60 + i), 1.0);
        }

        // Start a setter-spam thread simulating rapid MIDI CC messages
        AtomicBoolean running = new AtomicBoolean(true);
        Thread setterThread = new Thread(() -> {
            double v = 0.0;
            while (running.get()) {
                v += 0.01;
                if (v > 1.0) v = 0.0;
                synth.setFilterCutoff(20.0 + v * 19980.0);
                synth.setFilterResonance(1.0 + v * 14.0);
                synth.setAmpAttackTime(v * 2.0);
                synth.setAmpReleaseTime(v * 2.0);
                synth.setPreFilterGainDB(v * 24.0 - 12.0);
                synth.setMasterVolume(0.5 + v * 0.5);
                synth.setLFOFrequency(0.1 + v * 9.9);
                synth.setPanDepth(v);
            }
        });
        setterThread.setDaemon(true);
        setterThread.start();

        // Run processing loop under contention
        System.out.println("Processing " + numberOfBlocksToProcess + " blocks under contention...");
        long totalTestTime = 0;
        for (int i = 0; i < numberOfBlocksToProcess; i++) {
            long blockStartTime = System.nanoTime();
            Map<String, Long> blockTimings = synth.processBlockInstrumented(audioBlock);
            long blockEndTime = System.nanoTime();
            totalTestTime += (blockEndTime - blockStartTime);
            blockTimings.forEach((key, value) -> totalTimings.merge(key, value, Long::sum));
        }

        running.set(false);
        System.out.println("Contention test complete.\n");

        // Print results
        System.out.println("--- Contention Stress Test Results ---");
        System.out.println("Total processing time: " + TimeUnit.NANOSECONDS.toMillis(totalTestTime) + " ms");
        System.out.println("\n--- Average Time Per Stage (in microseconds) ---");
        totalTimings.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .forEach(entry -> {
                    long averageTime = entry.getValue() / numberOfBlocksToProcess;
                    System.out.printf("%-25s: %d µs%n", entry.getKey(),
                            TimeUnit.NANOSECONDS.toMicros(averageTime));
                });
        System.out.println("------------------------------------------");
    }
}