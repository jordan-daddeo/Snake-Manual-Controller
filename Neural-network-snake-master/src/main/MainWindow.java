package main;

import gameEngine.ESGame;
import gameEngine.GameLoop;
import gameEngine.QGame;
import helpers.KeyboardListener;

import javax.swing.JFrame;

public class MainWindow extends JFrame {
	/**
	 * main function of the whole simulation
	 */
	public static void main(String[] args) {
		new MainWindow();
	}
	/**
	 * Simple JFrame as user interface
	 */
	public MainWindow() {
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setSize( 1000, 600);
		setExtendedState(MAXIMIZED_BOTH);
		setTitle("Neural Net Snake Genetic Algorithm Except Better Because It's SARSA");
		KeyboardListener keyb = new KeyboardListener();
		addKeyListener(keyb);
		add(new QGame(keyb));
		setVisible(true);
	}

}

