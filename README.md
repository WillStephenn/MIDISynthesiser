# Java MIDI Synthesiser

This project is a polyphonic MIDI synthesiser built entirely in Java with a JavaFX GUI. It was developed as a personal exercise to gain a deeper understanding of Object-Oriented Programming (OOP), real-time audio processing, and digital signal processing (DSP) fundamentals.

The primary goal was to build a functional synthesiser from the ground up, intentionally avoiding third-party libraries for DSP and synthesis to engage directly with the core challenges of audio programming. Operating at 44.1 kHz (44,100 samples per second), this project taught me to build a super efficient and stable system capable of consistent, low-latency performance under real-time constraints.

![UI Screenshot](Repo%20Resources/UI%20Screenshot.png)
---

## Audio Samples

The following are performances synthesised by this application:

**Video Demonstration**: [Live Performance & Parameter Manipulation](https://youtu.be/QA0YQRSpx28) - Screen recording showing the synthesiser receiving MIDI from Logic Pro DAW with real-time parameter adjustments.

6 Voice Polyphony: [Arpeggiated Chord Stabs](https://drive.google.com/file/d/17FLlwFqXUtrMBFGSPvTjsKWTTUtj5Yr5/view?usp=drive_link)

1 Voice Monophic Synthesis: [Mono Bass Patch](https://drive.google.com/file/d/1jlSh9eo3OYC4EuEYe00UqcerP15NBXK_/view?usp=drive_link)

---

## Features

The synthesiser provides a complete synthesis signal path with a flexible, polyphonic voice architecture.

**Polyphonic Voice Architecture**: The synthesiser supports multiple voices (6 by default), with a voice-stealing algorithm to manage polyphony.

**Multi-Waveform Oscillators**: Each voice is equipped with an oscillator that can generate sine, square, sawtooth, and triangle waveforms.

**Resonant Low-Pass Filter**: A topology-preserving transform (TPT) state-variable filter is included in each voice, complete with frequency cutoff and resonance controls.

**Dual ADSR Envelopes**: Each voice has two independent ADSR (Attack, Decay, Sustain, Release) envelopes—one for modulating the amplitude and another for modulating the filter cutoff.

**Low-Frequency Oscillator (LFO)**: A global LFO with multiple waveforms (sine, saw, triangle, square) can be used to modulate parameters, including stereo panning.

**Full MIDI Control**: The synthesiser can be controlled in real-time via any standard MIDI input device. It handles Note On, Note Off, Velocity and Control Change (CC) messages to dynamically shape the sound.

**Graphical User Interface**: A complete GUI built with JavaFX allows for intuitive control over all synthesiser parameters, including device selection, oscillator and filter settings, envelopes, and global controls.

---

## Architecture

![Synth Architecture UML Diagram](Repo%20Resources/Synth%20Architechture.png)

The synthesiser's architecture is designed around a clear separation of concerns, promoting modularity and extensibility. At the core of the design are the **Synthesiser** and **Voice** classes, which work together to manage the entire audio signal path.

The `Voice` class is the fundamental building block of the synthesiser's sound-generating capabilities. Each `Voice` instance is a self-contained monophonic synthesiser, encapsulating an oscillator (sine, saw, square, or triangle), a resonant low-pass filter, and two ADSR envelope generators for amplitude and filter modulation. By encapsulating these components, the `Voice` class provides a simple interface for controlling the sound of a single note, while hiding the complexity of the underlying audio processing.

The `Synthesiser` class acts as the central point of control for the entire instrument, managing a pool of `Voice` objects to create a polyphonic synthesiser. It handles voice allocation, triggering, and releasing in response to MIDI input, implementing a voice-stealing algorithm to ensure new notes can be played even when all voices are active. The class also provides a unified interface for controlling global parameters that affect all voices simultaneously (master volume, LFO settings, patch configuration) and is responsible for mixing the audio output of all active voices into a single stereo stream.

This hierarchical architecture allows for a clean and efficient implementation of polyphony, with a clear separation between the high-level control logic and the low-level audio processing of individual voices.

---

## Technical Deep Dive: An Exercise in Optimisation

A core focus of this project was learning to write performant code suitable for real-time audio within the Java ecosystem. The nature of audio processing requires consistent, low-latency performance, which presents unique challenges for a managed environment like the JVM.

To achieve this, several optimisation strategies were employed:

**Minimising the Audio Thread Workload**: The most critical aspect was ensuring the audio processing loop is as efficient as possible, minimising floating-point operations and avoiding any operations that could introduce unpredictable delays.

**Pre-computation and Lookup Tables**: To avoid expensive calculations like `Math.sin()` or `Math.tan()` in the real-time audio thread, these values were pre-computed on startup and stored in large lookup tables. This includes all oscillator waveforms and the coefficients for the resonant filter at various cutoff and resonance settings.

**Eliminating Garbage Collection**: The audio processing loop is carefully designed to be garbage-free. All necessary memory, such as audio buffers, is allocated at initialisation and reused throughout the application's lifecycle. This is crucial for avoiding the unpredictable pauses that garbage collection can introduce, which would otherwise manifest as audible clicks or glitches.

**Efficient Operations**: Where possible, more efficient operations were used. For example, the oscillator's phase wrapping is handled with a bitwise `AND` operation (`& phaseMask`) instead of a conditional or modulo operation. This eliminated audible cracks and pops caused by CPU branch misprediction when using conditional statements. Constants, such as the reciprocal of the sample rate, were also pre-calculated to turn divisions into multiplications within the audio loop.

**Thread-Safe Concurrency Management**: The synthesiser handles concurrent access between the audio processing thread and the MIDI/UI threads through careful synchronization. All operations that modify or iterate over the voice array (`noteOn`, `noteOff`, `processBlock`, and `getActiveNotes`) use `synchronized` blocks on the voices collection, ensuring thread-safe voice stealing, note triggering, and audio rendering without race conditions. The `audioThreadRunning` flag in the UI controller is declared `volatile`, ensuring that changes to the thread's running state are immediately visible across threads and providing a reliable mechanism for gracefully stopping the audio processing thread.

---

## Development Notes

**UI Development**: The user interface components (everything in the `ui` folder, including FXML layouts and styling) were generated with Claude Sonnet 4 according to specific design requirements (colour scheme, fonts, theme, and button layouts/styles). All other code, including the core audio engine, DSP algorithms, and synthesis architecture, was written manually as a learning exercise.

---

## Limitations and Reflections

While this project was a success as a learning exercise, building a real-time synthesiser in Java highlighted the inherent challenges of the platform for this type of application. The Java Virtual Machine (JVM) with its automatic memory management and just-in-time compilation is not ideally suited for the demands of low-latency audio processing.

The non-deterministic nature of the garbage collector, even with careful coding practices, remains a potential source of instability. A language like **C++**, which offers manual memory management and closer-to-the-metal control, would be a more conventional and robust choice for a production-grade audio application. This project, therefore, stands not as a template for production software, but as a testament to the educational value of pushing a language to its limits to better understand the fundamentals of software engineering and digital audio.

---

## Getting Started

To run this project locally, you will need Java and Maven installed.

1.  **Clone the repository:**
    ```bash
    git clone https://github.com/WillStephenn/MIDISynthesiser
    ```
2.  **Navigate to the project directory:**
    ```bash
    cd MIDIsynthesiser
    ```
3.  **Build and run the application using Maven:**
    ```bash
    mvn clean javafx:run
    ```
    This will launch the synthesiser's graphical user interface. From there, you can select your preferred MIDI input and audio output devices to begin playing.
