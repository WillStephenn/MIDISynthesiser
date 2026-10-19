package synth.ui;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;

/**
 * A visual envelope designer component that displays an ADSR envelope curve
 * and allows interactive editing by dragging control points.
 * <p>
 * The plot is four fixed-width segments (attack, decay, sustain hold, release). Inside each
 * time segment a handle's x is log time (see {@link EnvelopeAxis}), so a handle can never leave
 * the plot and dragging one handle never moves another's time.
 * 
 * June's Logue - Visual Envelope Designer
 */
public class EnvelopeVisualizer extends Canvas {
    
    // Colors from the June's Logue theme
    private static final Color BACKGROUND_COLOR = Color.web("#0A0908");
    private static final Color GRID_COLOR = Color.web("#49111C");
    private static final Color CURVE_COLOR = Color.web("#FF9F1C");
    private static final Color CONTROL_POINT_COLOR = Color.web("#BA5624");
    private static final Color CONTROL_POINT_HIGHLIGHT = Color.web("#FF9F1C");
    private static final Color TEXT_COLOR = Color.web("#F2F4F3");
    
    // ADSR Properties (bindable)
    private final DoubleProperty attackTime = new SimpleDoubleProperty(0.005);
    private final DoubleProperty decayTime = new SimpleDoubleProperty(0.1);
    private final DoubleProperty sustainLevel = new SimpleDoubleProperty(0.4);
    private final DoubleProperty releaseTime = new SimpleDoubleProperty(0.4);
    
    // Control points for interaction
    private static final double CONTROL_POINT_RADIUS = 6.0;
    private enum ControlPoint { NONE, ATTACK, DECAY, SUSTAIN, RELEASE }
    private ControlPoint draggedPoint = ControlPoint.NONE;
    private boolean isDragging = false;
    // Handle position minus pointer position at press, so grabbing a handle off-centre doesn't move it
    private double grabOffsetX = 0.0;
    private double grabOffsetY = 0.0;
    
    // Padding for drawing area
    private static final double PADDING = 20.0;
    
    public EnvelopeVisualizer(double width, double height) {
        super(width, height);
        
        // Set up mouse interaction
        setupMouseHandlers();
        
        // Listen for property changes to redraw
        attackTime.addListener((obs, o, n) -> draw());
        decayTime.addListener((obs, o, n) -> draw());
        sustainLevel.addListener((obs, o, n) -> draw());
        releaseTime.addListener((obs, o, n) -> draw());
        
        // Initial draw
        draw();
    }
    
    private void setupMouseHandlers() {
        setOnMousePressed(this::handleMousePressed);
        setOnMouseDragged(this::handleMouseDragged);
        setOnMouseReleased(this::handleMouseReleased);
        setOnMouseMoved(this::handleMouseMoved);
    }
    
    private void handleMousePressed(MouseEvent event) {
        if (event.getButton() != MouseButton.PRIMARY) return; // only the primary button drags
        double x = event.getX();
        double y = event.getY();
        
        draggedPoint = getControlPointAt(x, y);
        if (draggedPoint != ControlPoint.NONE) {
            isDragging = true;
            double[] p = pointPosition(draggedPoint);
            grabOffsetX = p[0] - x;
            grabOffsetY = p[1] - y;
            draw();
        }
    }
    
    private void handleMouseDragged(MouseEvent event) {
        if (!isDragging || draggedPoint == ControlPoint.NONE) return;
        
        double drawWidth = getWidth() - 2 * PADDING;
        double drawHeight = getHeight() - 2 * PADDING;
        
        // Where the handle should sit: pointer plus the grab offset. xToTime clamps to the handle's own segment.
        double plotX = event.getX() + grabOffsetX - PADDING;
        double level = 1.0 - (event.getY() + grabOffsetY - PADDING) / drawHeight; // Flip Y axis
        level = Math.max(0.0, Math.min(1.0, level));
        
        switch (draggedPoint) {
            case ATTACK -> attackTime.set(EnvelopeAxis.xToTime(EnvelopeAxis.ATTACK, plotX, drawWidth));
            case DECAY -> {
                decayTime.set(EnvelopeAxis.xToTime(EnvelopeAxis.DECAY, plotX, drawWidth));
                sustainLevel.set(level);
            }
            case SUSTAIN -> sustainLevel.set(level); // X is fixed
            case RELEASE -> releaseTime.set(EnvelopeAxis.xToTime(EnvelopeAxis.RELEASE, plotX, drawWidth));
            default -> {
                // No action for NONE case
            }
        }
    }
    
    @SuppressWarnings("unused")
    private void handleMouseReleased(MouseEvent event) {
        isDragging = false;
        draggedPoint = ControlPoint.NONE;
        draw();
    }
    
    private void handleMouseMoved(MouseEvent event) {
        double x = event.getX();
        double y = event.getY();
        
        ControlPoint hoverPoint = getControlPointAt(x, y);
        
        // Change cursor when hovering over control points
        if (hoverPoint != ControlPoint.NONE) {
            getScene().setCursor(javafx.scene.Cursor.HAND);
        } else {
            getScene().setCursor(javafx.scene.Cursor.DEFAULT);
        }
    }
    
    /** Canvas position {x, y} of a handle; the same numbers the curve and handles are drawn at. */
    private double[] pointPosition(ControlPoint point) {
        double drawWidth = getWidth() - 2 * PADDING;
        double drawHeight = getHeight() - 2 * PADDING;
        double sustainY = PADDING + (1.0 - sustainLevel.get()) * drawHeight;
        return switch (point) {
            case ATTACK -> new double[] {PADDING + EnvelopeAxis.timeToX(EnvelopeAxis.ATTACK, attackTime.get(), drawWidth), PADDING};
            case DECAY -> new double[] {PADDING + EnvelopeAxis.timeToX(EnvelopeAxis.DECAY, decayTime.get(), drawWidth), sustainY};
            case SUSTAIN -> new double[] {PADDING + EnvelopeAxis.segmentEnd(EnvelopeAxis.HOLD, drawWidth), sustainY};
            case RELEASE -> new double[] {PADDING + EnvelopeAxis.timeToX(EnvelopeAxis.RELEASE, releaseTime.get(), drawWidth), PADDING + drawHeight};
            default -> new double[] {PADDING, PADDING + drawHeight};
        };
    }
    
    /** The control point nearest to (x, y) within the hit radius (ties go to the later of A, D, S, R). */
    private ControlPoint getControlPointAt(double x, double y) {
        ControlPoint best = ControlPoint.NONE;
        double bestDistance = CONTROL_POINT_RADIUS * 3; // Increased hit area for easier grabbing
        for (ControlPoint point : new ControlPoint[] {ControlPoint.ATTACK, ControlPoint.DECAY, ControlPoint.SUSTAIN, ControlPoint.RELEASE}) {
            double[] p = pointPosition(point);
            double distance = Math.hypot(x - p[0], y - p[1]);
            if (distance <= bestDistance) {
                best = point;
                bestDistance = distance;
            }
        }
        return best;
    }
    
    private void draw() {
        GraphicsContext gc = getGraphicsContext2D();
        
        // Clear background
        gc.setFill(BACKGROUND_COLOR);
        gc.fillRect(0, 0, getWidth(), getHeight());
        
        double drawWidth = getWidth() - 2 * PADDING;
        double drawHeight = getHeight() - 2 * PADDING;
        
        // Draw grid
        drawGrid(gc, drawWidth, drawHeight);
        
        // Draw envelope curve
        drawEnvelopeCurve(gc, drawWidth, drawHeight);
        
        // Draw control points
        drawControlPoints(gc, drawWidth, drawHeight);
        
        // Draw labels
        drawLabels(gc, drawWidth, drawHeight);
    }
    
    private void drawGrid(GraphicsContext gc, double drawWidth, double drawHeight) {
        gc.setStroke(GRID_COLOR);
        gc.setLineWidth(1.0);
        
        // Vertical grid lines (time): one per decade (10 ms, 100 ms, 1 s) inside each time segment
        for (int segment : new int[] {EnvelopeAxis.ATTACK, EnvelopeAxis.DECAY, EnvelopeAxis.RELEASE}) {
            for (double t : new double[] {0.01, 0.1, 1.0}) {
                double x = PADDING + EnvelopeAxis.timeToX(segment, t, drawWidth);
                gc.strokeLine(x, PADDING, x, PADDING + drawHeight);
            }
        }
        
        // Horizontal grid lines (level)
        for (int i = 0; i <= 10; i++) {
            double y = PADDING + (i / 10.0) * drawHeight;
            gc.strokeLine(PADDING, y, PADDING + drawWidth, y);
        }
        
        // Segment dividers
        gc.setStroke(CONTROL_POINT_COLOR.deriveColor(0, 1, 1, 0.55));
        gc.setLineDashes(3, 3);
        for (int segment = 1; segment < 4; segment++) {
            double x = PADDING + EnvelopeAxis.segmentStart(segment, drawWidth);
            gc.strokeLine(x, PADDING, x, PADDING + drawHeight);
        }
        gc.setLineDashes((double[]) null);
        
        // Draw border
        gc.setStroke(CONTROL_POINT_COLOR);
        gc.setLineWidth(2.0);
        gc.strokeRect(PADDING, PADDING, drawWidth, drawHeight);
    }
    
    private void drawEnvelopeCurve(GraphicsContext gc, double drawWidth, double drawHeight) {
        gc.setStroke(CURVE_COLOR);
        gc.setLineWidth(3.0);
        
        // Every handle stays inside its own segment, so the curve always fits the plot
        double[] a = pointPosition(ControlPoint.ATTACK);
        double[] d = pointPosition(ControlPoint.DECAY);
        double[] s = pointPosition(ControlPoint.SUSTAIN);
        double[] r = pointPosition(ControlPoint.RELEASE);
        
        // Start to Attack
        gc.strokeLine(PADDING, PADDING + drawHeight, a[0], a[1]);
        // Attack to Decay
        gc.strokeLine(a[0], a[1], d[0], d[1]);
        // Sustain phase
        gc.strokeLine(d[0], d[1], s[0], s[1]);
        // Release phase
        gc.strokeLine(s[0], s[1], r[0], r[1]);
    }
    
    private void drawControlPoints(GraphicsContext gc, double drawWidth, double drawHeight) {
        drawControlPoint(gc, pointPosition(ControlPoint.ATTACK), "A", draggedPoint == ControlPoint.ATTACK);
        drawControlPoint(gc, pointPosition(ControlPoint.DECAY), "D", draggedPoint == ControlPoint.DECAY);
        drawControlPoint(gc, pointPosition(ControlPoint.SUSTAIN), "S", draggedPoint == ControlPoint.SUSTAIN);
        drawControlPoint(gc, pointPosition(ControlPoint.RELEASE), "R", draggedPoint == ControlPoint.RELEASE);
    }
    
    private void drawControlPoint(GraphicsContext gc, double[] position, String label, boolean highlighted) {
        double x = position[0];
        double y = position[1];
        Color fillColor = highlighted ? CONTROL_POINT_HIGHLIGHT : CONTROL_POINT_COLOR;
        
        // Draw control point circle
        gc.setFill(fillColor);
        gc.fillOval(x - CONTROL_POINT_RADIUS, y - CONTROL_POINT_RADIUS, 
                   CONTROL_POINT_RADIUS * 2, CONTROL_POINT_RADIUS * 2);
        
        // Draw border
        gc.setStroke(TEXT_COLOR);
        gc.setLineWidth(1.0);
        gc.strokeOval(x - CONTROL_POINT_RADIUS, y - CONTROL_POINT_RADIUS, 
                     CONTROL_POINT_RADIUS * 2, CONTROL_POINT_RADIUS * 2);
        
        // Draw label
        gc.setFill(TEXT_COLOR);
        gc.setFont(javafx.scene.text.Font.font("Orbitron", 10));
        double textWidth = gc.getFont().getSize() * 0.6; // Approximate text width
        gc.fillText(label, x - textWidth/2, y + 3);
    }
    
    private void drawLabels(GraphicsContext gc, double drawWidth, double drawHeight) {
        gc.setFill(TEXT_COLOR);
        gc.setFont(javafx.scene.text.Font.font("Orbitron", 9));
        
        // Segment labels: each segment's name with its time
        gc.setTextAlign(TextAlignment.CENTER);
        String[] names = {
            "A " + EnvelopeAxis.formatTime(attackTime.get()), "D " + EnvelopeAxis.formatTime(decayTime.get()), "S", "R " + EnvelopeAxis.formatTime(releaseTime.get())
        };
        for (int i = 0; i < names.length; i++) {
            double centre = PADDING + (EnvelopeAxis.segmentStart(i, drawWidth) + EnvelopeAxis.segmentEnd(i, drawWidth)) / 2;
            gc.fillText(names[i], centre, PADDING + drawHeight + 14);
        }
        gc.setTextAlign(TextAlignment.LEFT);
        
        // Level axis labels
        gc.fillText("0", PADDING - 15, PADDING + drawHeight + 3);
        gc.fillText("1", PADDING - 15, PADDING + 3);
    }
    
    // Property getters for binding
    public DoubleProperty attackTimeProperty() { return attackTime; }
    public DoubleProperty decayTimeProperty() { return decayTime; }
    public DoubleProperty sustainLevelProperty() { return sustainLevel; }
    public DoubleProperty releaseTimeProperty() { return releaseTime; }
    
    // Setters for external control
    public void setAttackTime(double value) { attackTime.set(value); }
    public void setDecayTime(double value) { decayTime.set(value); }
    public void setSustainLevel(double value) { sustainLevel.set(value); }
    public void setReleaseTime(double value) { releaseTime.set(value); }
    
    // Getters
    public double getAttackTime() { return attackTime.get(); }
    public double getDecayTime() { return decayTime.get(); }
    public double getSustainLevel() { return sustainLevel.get(); }
    public double getReleaseTime() { return releaseTime.get(); }
    
    @Override
    public boolean isResizable() {
        return false;
    }
}