package pcd.ass01.View;

import akka.actor.typed.ActorRef;
import pcd.ass01.Model.P2d;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import pcd.ass03.BoidsSimulation;

public class ScalaBoidsView implements ChangeListener {

    private JFrame frame;
    private final ScalaBoidsPanel boidsPanel;
    private JSlider cohesionSlider, separationSlider, alignmentSlider, boidSlider;
    private JButton pauseResumeButton, simulationModeButton;
    private int width, height;
    private volatile List<P2d> boids;
    private ActorRef<BoidsSimulation.Command> actorRef;
    private Boolean isRunning = true;


    public ScalaBoidsView(int width, int height) {
        this.width = width;
        this.height = height;
        boids = new ArrayList<P2d>();
        frame = new JFrame("Boids Simulation");
        frame.setSize(width, height);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel cp = new JPanel(new BorderLayout());

        boidsPanel = new ScalaBoidsPanel(this, boids);
        cp.add(BorderLayout.CENTER, boidsPanel);

        cp.add(BorderLayout.SOUTH, createBottomPanel());

        frame.setContentPane(cp);
        frame.setVisible(true);
    }

    public void setActorRef(ActorRef<BoidsSimulation.Command> actorRef) {
        this.actorRef = actorRef;
    }

    public synchronized void updateAllMapPositions(List<P2d> newMap) {
        boids = new ArrayList<>(newMap);
        boidsPanel.setBoids(boids);
    }

    private JPanel createSlidersPanel() {
        JPanel slidersPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        cohesionSlider = createSlider();
        separationSlider = createSlider();
        alignmentSlider = createSlider();
        boidSlider = createBoidSlider();

        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridx = 0;
        gbc.gridy = 0;
        slidersPanel.add(new JLabel("Separation"), gbc);
        gbc.gridx = 1;
        slidersPanel.add(separationSlider, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        slidersPanel.add(new JLabel("Alignment"), gbc);
        gbc.gridx = 1;
        slidersPanel.add(alignmentSlider, gbc);

        gbc.gridx = 2;
        gbc.gridy = 0;
        gbc.gridheight = 2;
        slidersPanel.add(Box.createHorizontalStrut(40), gbc);

        gbc.gridheight = 1; // Reset
        gbc.gridx = 3;
        gbc.gridy = 0;
        slidersPanel.add(new JLabel("Cohesion"), gbc);
        gbc.gridx = 4;
        slidersPanel.add(cohesionSlider, gbc);

        gbc.gridx = 3;
        gbc.gridy = 1;
        slidersPanel.add(new JLabel("Boids"), gbc);
        gbc.gridx = 4;
        slidersPanel.add(boidSlider, gbc);

        return slidersPanel;
    }

    private JPanel createButtonsPanel() {
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));

        pauseResumeButton = new JButton("Pause");
        pauseResumeButton.addActionListener(e -> toggleSimulationState());

        simulationModeButton = new JButton("Stop");
        simulationModeButton.addActionListener(e -> toggleStopSimulation());

        buttonsPanel.add(pauseResumeButton);
        buttonsPanel.add(simulationModeButton);

        return buttonsPanel;
    }

    private JPanel createBottomPanel() {
        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.Y_AXIS));
        bottomPanel.add(createSlidersPanel());
        bottomPanel.add(createButtonsPanel());
        return bottomPanel;
    }

    private void toggleSimulationState() {
        if (isRunning) {
            actorRef.tell(BoidsSimulation.PauseSimulation$.MODULE$);
        } else {
            actorRef.tell(BoidsSimulation.ResumeSimulation$.MODULE$);
        }
        isRunning = !isRunning;
        pauseResumeButton.setText(!isRunning ? "Resume" : "Pause");
    }

    private void toggleStopSimulation() {
        /*if (isRunning) {
            actorRef.tell(new BoidsSimulation.PauseSimulation);
        } else {
            if (boidsProperty.isSuspended()) {
                toggleSimulationState();
            }
            simulationStateHandler.start();
        }
        isRunning = !isRunning;

        simulationModeButton.setText(boidsProperty.isRunning() ? "Stop" : "Start");*/
    }

    private JSlider createSlider() {
        JSlider slider = new JSlider(JSlider.HORIZONTAL, 0, 20, 10);
        slider.setMajorTickSpacing(10);
        slider.setMinorTickSpacing(1);
        slider.setPaintTicks(true);
        slider.setPaintLabels(true);

        Hashtable<Integer, JLabel> labelTable = new Hashtable<>();
        labelTable.put(0, new JLabel("0"));
        labelTable.put(10, new JLabel("1"));
        labelTable.put(20, new JLabel("2"));
        slider.setLabelTable(labelTable);

        slider.addChangeListener(this);
        return slider;
    }

    private JSlider createBoidSlider() {
        JSlider slider = new JSlider(JSlider.HORIZONTAL, 0, 4000, 1500);
        slider.setMajorTickSpacing(1000);
        slider.setMinorTickSpacing(500);
        slider.setPaintTicks(true);
        slider.setPaintLabels(true);

        Hashtable<Integer, JLabel> labelTable = new Hashtable<>();
        labelTable.put(0, new JLabel("0"));
        labelTable.put(2000, new JLabel("2000"));
        labelTable.put(4000, new JLabel("4000"));
        slider.setLabelTable(labelTable);

        slider.addChangeListener(this);
        return slider;
    }

    public synchronized void update(int frameRate) {
        boidsPanel.setFrameRate(frameRate);
        boidsPanel.repaint();
    }

    @Override
    public void stateChanged(ChangeEvent e) {
        /*if (e.getSource() == separationSlider) {
            boidsProperty.setSeparationWeight(0.1 * separationSlider.getValue());
        } else if (e.getSource() == cohesionSlider) {
            boidsProperty.setCohesionWeight(0.1 * cohesionSlider.getValue());
        } else if (e.getSource() == alignmentSlider) {
            boidsProperty.setAlignmentWeight(0.1 * alignmentSlider.getValue());
        } else if (e.getSource() == boidSlider) {
            boidsProperty.setNumberOfBoids(boidSlider.getValue());
        }*/
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }
}
