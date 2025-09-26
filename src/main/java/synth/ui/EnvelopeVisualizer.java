package synth.ui;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;

/**
 * A visual envelope designer component that displays an ADSR envelope curve
 * and allows interactive editing by dragging control points.
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
    
    // Time scaling for visualization (max time to display)
    private double maxTimeSeconds = 3.0;
    
    // Control points for interaction
    private static final double CONTROL_POINT_RADIUS = 6.0;
    private enum ControlPoint { NONE, ATTACK, DECAY, SUSTAIN, RELEASE }
    private ControlPoint draggedPoint = ControlPoint.NONE;
    private boolean isDragging = false;
    
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
        double x = event.getX();
        double y = event.getY();
        
        draggedPoint = getControlPointAt(x, y);
        if (draggedPoint != ControlPoint.NONE) {
            isDragging = true;
        }
    }
    
    private void handleMouseDragged(MouseEvent event) {
        if (!isDragging || draggedPoint == ControlPoint.NONE) return;
        
        double x = event.getX();
        double y = event.getY();
        
        // Convert screen coordinates to envelope parameters
        double drawWidth = getWidth() - 2 * PADDING;
        double drawHeight = getHeight() - 2 * PADDING;
        
        // Normalize coordinates
        double normalizedX = (x - PADDING) / drawWidth;
        double normalizedY = 1.0 - (y - PADDING) / drawHeight; // Flip Y axis
        
        // Clamp values
        normalizedX = Math.max(0.0, Math.min(1.0, normalizedX));
        normalizedY = Math.max(0.0, Math.min(1.0, normalizedY));
        
        switch (draggedPoint) {
            case ATTACK -> {
                // Attack time is based on X position relative to total time
                double newAttackTime = normalizedX * maxTimeSeconds * 0.3; // Limit to 30% of total time
                attackTime.set(Math.max(0.001, newAttackTime));
            }
            case DECAY -> {
                // Decay time and sustain level
                double newDecayTime = Math.max(0.001, (normalizedX * maxTimeSeconds) - attackTime.get());
                decayTime.set(newDecayTime);
                sustainLevel.set(normalizedY);
            }
            case SUSTAIN -> {
                // Only sustain level (Y position) - X position is fixed
                sustainLevel.set(normalizedY);
            }
            case RELEASE -> {
                // For release, we want to control the release time based on how far right we drag
                // The release starts from the sustain point, so we calculate relative to that
                double sustainEndX = (attackTime.get() + decayTime.get()) / maxTimeSeconds;
                double sustainVisualizationWidth = 80.0 / (getWidth() - 2 * PADDING); // Normalize the 80px sustain visualization
                double releaseStartX = sustainEndX + sustainVisualizationWidth;
                
                // Only update if we're dragging to the right of the sustain phase
                if (normalizedX > releaseStartX) {
                    double newReleaseTime = (normalizedX - releaseStartX) * maxTimeSeconds;
                    releaseTime.set(Math.max(0.001, newReleaseTime));
                }
            }
            default -> {
                // No action for NONE case
            }
        }
    }
    
    @SuppressWarnings("unused")
    private void handleMouseReleased(MouseEvent event) {
        isDragging = false;
        draggedPoint = ControlPoint.NONE;
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
    
    private ControlPoint getControlPointAt(double x, double y) {
        double drawWidth = getWidth() - 2 * PADDING;
        double drawHeight = getHeight() - 2 * PADDING;
        
        // Calculate control point positions - make sure they match drawEnvelopeCurve exactly
        double attackX = PADDING + (attackTime.get() / maxTimeSeconds) * drawWidth;
        double attackY = PADDING; // Attack goes to max (top)
        
        double decayX = PADDING + ((attackTime.get() + decayTime.get()) / maxTimeSeconds) * drawWidth;
        double decayY = PADDING + (1.0 - sustainLevel.get()) * drawHeight;
        
        // Sustain point - positioned at the end of the sustain phase
        double sustainX = decayX + 80; // Match the sustain visualization length from drawEnvelopeCurve
        double sustainY = decayY;
        
        // Release point - positioned at the end of the release phase
        double releaseX = sustainX + (releaseTime.get() / maxTimeSeconds) * drawWidth;
        releaseX = Math.min(releaseX, PADDING + drawWidth); // Clamp to visible area
        double releaseY = PADDING + drawHeight; // Release goes to bottom
        
        // Check if mouse is near any control point - check in reverse order for overlapping points
        if (isNearPoint(x, y, releaseX, releaseY)) return ControlPoint.RELEASE;
        if (isNearPoint(x, y, sustainX, sustainY)) return ControlPoint.SUSTAIN;
        if (isNearPoint(x, y, decayX, decayY)) return ControlPoint.DECAY;
        if (isNearPoint(x, y, attackX, attackY)) return ControlPoint.ATTACK;
        
        return ControlPoint.NONE;
    }
    
    private boolean isNearPoint(double x, double y, double pointX, double pointY) {
        double distance = Math.sqrt(Math.pow(x - pointX, 2) + Math.pow(y - pointY, 2));
        return distance <= CONTROL_POINT_RADIUS * 3; // Increased hit area for easier grabbing
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
        
        // Vertical grid lines (time)
        for (int i = 0; i <= 10; i++) {
            double x = PADDING + (i / 10.0) * drawWidth;
            gc.strokeLine(x, PADDING, x, PADDING + drawHeight);
        }
        
        // Horizontal grid lines (level)
        for (int i = 0; i <= 10; i++) {
            double y = PADDING + (i / 10.0) * drawHeight;
            gc.strokeLine(PADDING, y, PADDING + drawWidth, y);
        }
        
        // Draw border
        gc.setStroke(CONTROL_POINT_COLOR);
        gc.setLineWidth(2.0);
        gc.strokeRect(PADDING, PADDING, drawWidth, drawHeight);
    }
    
    private void drawEnvelopeCurve(GraphicsContext gc, double drawWidth, double drawHeight) {
        gc.setStroke(CURVE_COLOR);
        gc.setLineWidth(3.0);
        
        // Calculate key points
        double attackX = PADDING + (attackTime.get() / maxTimeSeconds) * drawWidth;
        double attackY = PADDING; // Attack peak (top)
        
        double decayX = PADDING + ((attackTime.get() + decayTime.get()) / maxTimeSeconds) * drawWidth;
        double decayY = PADDING + (1.0 - sustainLevel.get()) * drawHeight;
        
        double sustainX = decayX + 80; // Sustain phase visualization
        double sustainY = decayY;
        
        double releaseX = sustainX + (releaseTime.get() / maxTimeSeconds) * drawWidth;
        releaseX = Math.min(releaseX, PADDING + drawWidth); // Clamp to visible area
        double releaseY = PADDING + drawHeight; // Release to bottom
        
        // Draw envelope segments
        // Start to Attack
        gc.strokeLine(PADDING, PADDING + drawHeight, attackX, attackY);
        
        // Attack to Decay
        gc.strokeLine(attackX, attackY, decayX, decayY);
        
        // Sustain phase
        gc.strokeLine(decayX, decayY, sustainX, sustainY);
        
        // Release phase
        gc.strokeLine(sustainX, sustainY, releaseX, releaseY);
    }
    
    private void drawControlPoints(GraphicsContext gc, double drawWidth, double drawHeight) {
        // Calculate control point positions
        double attackX = PADDING + (attackTime.get() / maxTimeSeconds) * drawWidth;
        double attackY = PADDING;
        
        double decayX = PADDING + ((attackTime.get() + decayTime.get()) / maxTimeSeconds) * drawWidth;
        double decayY = PADDING + (1.0 - sustainLevel.get()) * drawHeight;
        
        double sustainX = decayX + 80;
        double sustainY = decayY;
        
        double releaseX = sustainX + (releaseTime.get() / maxTimeSeconds) * drawWidth;
        releaseX = Math.min(releaseX, PADDING + drawWidth);
        double releaseY = PADDING + drawHeight;
        
        // Draw control points
        drawControlPoint(gc, attackX, attackY, "A", draggedPoint == ControlPoint.ATTACK);
        drawControlPoint(gc, decayX, decayY, "D", draggedPoint == ControlPoint.DECAY);
        drawControlPoint(gc, sustainX, sustainY, "S", draggedPoint == ControlPoint.SUSTAIN);
        drawControlPoint(gc, releaseX, releaseY, "R", draggedPoint == ControlPoint.RELEASE);
    }
    
    private void drawControlPoint(GraphicsContext gc, double x, double y, String label, boolean highlighted) {
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
        
        // Time axis labels
        gc.fillText("0s", PADDING - 5, PADDING + drawHeight + 15);
        gc.fillText(String.format("%.1fs", maxTimeSeconds), PADDING + drawWidth - 15, PADDING + drawHeight + 15);
        
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
    
    // Utility method to set max time scale
    public void setMaxTimeSeconds(double maxTime) {
        this.maxTimeSeconds = maxTime;
        draw();
    }
    
    @Override
    public boolean isResizable() {
        return false;
    }
}