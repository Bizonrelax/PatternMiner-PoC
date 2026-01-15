package gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class SimpleGUI extends JFrame {
    
    private JTextArea logArea;
    private JButton startButton;
    private JButton stopButton;
    private JLabel statusLabel;
    private JLabel coinsLabel;
    private JLabel patternsLabel;
    
    private int patternsFound = 0;
    private double coinsEarned = 0;
    private boolean mining = false;
    
    public SimpleGUI() {
        setTitle("PatternMiner v0.1 - GUI Demo");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(600, 400);
        setLayout(new BorderLayout());
        
        initComponents();
        layoutComponents();
        
        setLocationRelativeTo(null);
    }
    
    private void initComponents() {
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        
        startButton = new JButton("Старт майнинга");
        stopButton = new JButton("Стоп");
        stopButton.setEnabled(false);
        
        statusLabel = new JLabel("Статус: Остановлен");
        coinsLabel = new JLabel("PatternCoin: 0.0 PTC");
        patternsLabel = new JLabel("Паттернов найдено: 0");
        
        startButton.addActionListener(e -> startMining());
        stopButton.addActionListener(e -> stopMining());
    }
    
    private void layoutComponents() {
        JPanel controlPanel = new JPanel(new FlowLayout());
        controlPanel.add(startButton);
        controlPanel.add(stopButton);
        
        JPanel statsPanel = new JPanel(new GridLayout(1, 3));
        statsPanel.setBorder(BorderFactory.createTitledBorder("Статистика"));
        statsPanel.add(statusLabel);
        statsPanel.add(coinsLabel);
        statsPanel.add(patternsLabel);
        
        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Лог майнинга"));
        
        add(controlPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(statsPanel, BorderLayout.SOUTH);
    }
    
    private void startMining() {
        mining = true;
        startButton.setEnabled(false);
        stopButton.setEnabled(true);
        statusLabel.setText("Статус: Майнинг...");
        
        log("=== Майнинг начат ===");
        log("Поиск паттернов в данных...");
        
        new Thread(() -> {
            while (mining) {
                try {
                    Thread.sleep(2000);
                    
                    if (Math.random() > 0.3) {
                        SwingUtilities.invokeLater(() -> foundPattern());
                    }
                    
                } catch (InterruptedException e) {
                    break;
                }
            }
        }).start();
    }
    
    private void stopMining() {
        mining = false;
        startButton.setEnabled(true);
        stopButton.setEnabled(false);
        statusLabel.setText("Статус: Остановлен");
        
        log("=== Майнинг остановлен ===");
        log("Итог: найдено " + patternsFound + " паттернов");
        log("Заработано: " + String.format("%.2f", coinsEarned) + " PTC");
    }
    
    private void foundPattern() {
        patternsFound++;
        coinsEarned += 1.5 + Math.random();
        
        String[] patterns = {
            "Повторение символов: AAA{10}",
            "Числовая последовательность: [0-9]{6}",
            "Буквенный паттерн: [A-Z]{3}[0-9]{3}",
            "BASE64 группа: [A-Za-z0-9+/]{8}",
            "Текстовая константа: hello_world"
        };
        
        String pattern = patterns[(int)(Math.random() * patterns.length)];
        
        log("✅ Найден паттерн: " + pattern);
        log("   Награда: 1.5 PTC");
        
        patternsLabel.setText("Паттернов найдено: " + patternsFound);
        coinsLabel.setText(String.format("PatternCoin: %.2f PTC", coinsEarned));
    }
    
    private void log(String message) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(new java.util.Date() + ": " + message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            SimpleGUI gui = new SimpleGUI();
            gui.setVisible(true);
        });
    }
}