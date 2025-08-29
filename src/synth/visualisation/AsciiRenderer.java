package synth.visualisation;

import synth.core.Synthesiser;

public class AsciiRenderer {

    /**
     * Clears the console screen. This method is used to create an animation
     * effect by clearing the previous frame before drawing the new one.
     */
    public static void clearConsole() {
        try {
            // This works for most terminals/consoles
            final String os = System.getProperty("os.name");
            if (os.contains("Windows")) {
                new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
            } else {
                System.out.print("\033[H\033[2J");
                System.out.flush();
            }
        } catch (final Exception e) {
            // Handle exceptions, e.g., if the clear command fails
            System.err.println("Error clearing console: " + e.getMessage());
        }
    }

    /**
     * Renders the synthesizer's state as a formatted ASCII block.
     * @param synth The synthesiser instance to get the parameters from.
     */
    public static void render(Synthesiser synth) {
        // Clear the console before printing the new state
        clearConsole();

        // Using a Text Block for the UI template.
        // The .formatted() method replaces the specifiers with the provided values.
        String ui = """
            +================JUNE'S===LOGUE================+
            |OSCILLATOR: %-34s|
            +----------------------------------------------+
            |AMP ENVELOPE:                (time unit: s)   |
            | ATTACK   DECAY    SUSTAIN   RELEASE          |
            | %-8.2f %-8.2f %-9s %-8.2f         |
            +----------------------------------------------+
            |FILTER:                      (time unit: s)   |
            | CUTOFF(Hz)   RESONANCE(Q)   MOD RANGE(Hz)    |
            | %-12.0f %-6.1f         %-17.0f|
            |                                              |
            | ATTACK   DECAY    SUSTAIN   RELEASE          |
            | %-8.2f %-8.2f %-9s %-8.2f         |
            |                                              |
            | PRE-GAIN(db)      POST-GAIN(db)              |
            | %-17.1f %-10.1f                 |
            +----------------------------------------------+
            |LFO:                                          |
            | OSCILLATOR: %-10s FREQUENCY(Hz):%-8.1f|
            |                                              |
            | Pan Depth: %-34s|
            +----------------------------------------------+
            """.formatted(
                synth.getWaveform(),
                synth.getAmpAttackTime(),
                synth.getAmpDecayTime(),
                String.format("%.0f%%", synth.getAmpSustainLevel() * 100),
                synth.getAmpReleaseTime(),
                synth.getFilterCutoff(),
                synth.getFilterResonance(),
                synth.getFilterModRange(),
                synth.getFilterAttackTime(),
                synth.getFilterDecayTime(),
                String.format("%.0f%%", synth.getFilterSustainLevel() * 100),
                synth.getFilterReleaseTime(),
                synth.getPreFilterGainDB(),
                synth.getPostFilterGainDB(),
                synth.getLFOWaveform(),
                synth.getLFOFrequency(),
                String.format("%.0f%%", synth.getPanDepth() * 100)
        );

        System.out.println(ui);
    }
}