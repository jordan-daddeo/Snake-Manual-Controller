package gameEngine;

import genetics.DNA;
import helpers.KeyboardListener;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;

import javax.swing.JComponent;

public class GameLoop extends JComponent {
	// main update frequency:
	public static final long UPDATEPERIOD = 8;
	public double per = UPDATEPERIOD;

	// constants:
	public static final int globalCircleRadius = 20;
	public static final int numSnakes = 8;
	public static final int numNibbles = 6;

	// Genetics parameter initialization:
	public static double mutationrate = .02;
	public double currentGeneration = 0;
	public int intGeneration = 0;
	public boolean newEra = true;

	// world and snakes initialization:
	public World world = new World();
	public LinkedList<Snake> snakes = new LinkedList<Snake>();
	public LinkedList<Snake> backupSnakes = new LinkedList<Snake>(); // to
																		// resume
																		// from
																		// single
																		// mode
	//player snakes
	public Snake currentSnake = null;
	public boolean manualControl = false;
	
	//controller snake
	public Snake ruleBased = null;
	public boolean runRuleBased = false;
	
	// Best:
	public DNA bestDna = null;
	public double bestscore = 0;

	// Statistics:
	public LinkedList<Double> fitnessTimeline = new LinkedList<Double>();
	public double currentMaxFitness = 0;
	public double allTimeMaxFitnessNN = 0;
	public double currentMaxFitnessRuleBased = 0;

	// Mode control:
	public boolean singleSnakeModeActive = false;
	public boolean displayStatisticsActive = false;
	public boolean simulationPaused = false;

	/**
	 * Component with the main loop This should be separated from the graphics,
	 * but I was to lazy.
	 */
	public GameLoop(KeyboardListener keyb) {
		world.height = 200;
		world.width = 300;
		new Thread(new Runnable() {
			private long simulationLastMillis;
			private long statisticsLastMillis;

			@SuppressWarnings("unchecked")
			public void run() {
				simulationLastMillis = System.currentTimeMillis() + 100; // initial
																			// wait
																			// for
																			// graphics
																			// to
																			// settle
				statisticsLastMillis = 0;
				while (true) {
					if (System.currentTimeMillis() - simulationLastMillis > UPDATEPERIOD) {
						synchronized (snakes) { // protect read
							long currentTime = System.currentTimeMillis();
							// Controls
							char keyCode = (char) keyb.getKey();
							switch (keyCode) {
							case ' ': // space
								if (!singleSnakeModeActive) {
									singleSnakeModeActive = true;
									displayStatisticsActive = false;
									backupSnakes.clear();
									backupSnakes.addAll(snakes);
									snakes.clear();
									snakes.add(new Snake(bestDna, world));
								}
								break;
							case 'A': // a = pause
								simulationPaused = true;
								break;
							case 'B': // b = resume
								simulationPaused = false;
								break;
							case 'C': // c = show stats
								displayStatisticsActive = true;
								break;
							case 'D': // d = hide stats
								displayStatisticsActive = false;
								break;
							case 'E':
								if(manualControl == true)
									manualControl = false;
								else
									manualControl = true;
								break;
							case 'F':
								if(runRuleBased == true)
									runRuleBased = false;
								else
									runRuleBased = true;
								break;
							case '%':
								if(manualControl && currentSnake != null)
									currentSnake.dir = -.08;
								break;
							case '\'':
								if(manualControl && currentSnake != null)
									currentSnake.dir = .08;
								break;
							default:
								if(manualControl & currentSnake != null)
									currentSnake.dir = 0;
							}
							// initilize first generation:
							if (snakes.isEmpty()) {
								firstGeneration(numSnakes);
								world.newNibble(numNibbles);
							}
							// computation:
							if (!simulationPaused) {
								int deadCount = 0;
								world.update(getWidth(), getHeight());
								synchronized (fitnessTimeline) {
									if (world.clock - statisticsLastMillis > 1000 && !singleSnakeModeActive) {
										fitnessTimeline.addLast(currentMaxFitness);
										currentMaxFitness = 0;
										if (fitnessTimeline.size() >= world.width / 2) {
											fitnessTimeline.removeFirst();
										}
										statisticsLastMillis = world.clock;
									}
								}
								for (Snake s : snakes) {
									if (!s.update(world)) {
										deadCount++;
									}
									if (s.getFitness() > currentMaxFitness && s!= ruleBased)
										currentMaxFitness = s.getFitness();
										if(currentMaxFitness > allTimeMaxFitnessNN) allTimeMaxFitnessNN = currentMaxFitness;
									if (s.getFitness() > currentMaxFitnessRuleBased && s == ruleBased){
										currentMaxFitnessRuleBased = s.getFitness();
									}
									if (s.getFitness() > bestscore) {
										bestscore = s.getFitness();
										bestDna = s.dna;
									}
								}
								if (deadCount > 0 && singleSnakeModeActive) {
									singleSnakeModeActive = false;
									snakes.clear();
									snakes.addAll(backupSnakes);

								} else {
									// new snakes
									for (int i = 0; i < deadCount; i++) {
										newSnake();
										currentGeneration += 1 / (double) numSnakes;
										if(((int) currentGeneration) > intGeneration) {
											intGeneration = (int) currentGeneration;
											newEra = true;
										}
										if((((int) currentGeneration) % 50 == 0) && newEra){
											newEra = false;
											ArrayList<Snake> snakesSorted = new ArrayList<>();
											snakesSorted.addAll(snakes);
											Collections.sort(snakesSorted);
											System.out.println("Generation: " + Integer.toString(intGeneration) + " Max Fitness: " + Double.toString(currentMaxFitness));
										}
									}
								}
								Iterator<Snake> it = snakes.iterator();
								while (it.hasNext()) {
									Snake s = it.next();
									if (s.deathFade <= 0) {
										it.remove();
									}
								}
							} else {
								// print status:
								snakes.get(0).brain(world);
							}

							repaint();
							per = System.currentTimeMillis() - currentTime;
							simulationLastMillis += UPDATEPERIOD;
						}
					}
				}
			}
		}).start();
	}

	/**
	 * initializes snake array with n fresh snakes
	 * 
	 * @param n
	 *            amount of snakes
	 */
	public void firstGeneration(int n) {
		snakes.clear();
		for (int i = 0; i < n; i++) {
			snakes.add(new Snake(null, world));
		}
		world.reset();
	}

	/**
	 * Creates the mating pool out of the snake-list
	 * 
	 * @return Mating pool as list
	 */
	public ArrayList<Snake> makeMatingpool() {
		ArrayList<Snake> matingpool = new ArrayList<Snake>();
		// get maximum fitness:
		double maxscore = 0;
		for (Snake s : snakes) {
			if (s.getFitness() > maxscore && ruleBased != s && currentSnake != s) {
				maxscore = s.getFitness();
			}
		}
		// Add snakes according to fitness
		for (Snake s : snakes) {
			if(ruleBased == s || currentSnake == s)
				continue;
			int amount = (int) (s.getFitness() * 100 / maxscore);
			for (int i = 0; i < amount; i++) {
				matingpool.add(s);
			}
		}
		return matingpool;
	}

	/**
	 * Creates a new snake using the genetic algorithm and adds it to the
	 * snake-list
	 */
	public void newSnake() {
		mutationrate = 10 / currentMaxFitness;
		ArrayList<Snake> matingpool = makeMatingpool();
		int idx1 = (int) (Math.random() * matingpool.size());
		int idx2 = (int) (Math.random() * matingpool.size());
		DNA parentA = matingpool.get(idx1).dna;
		DNA parentB = matingpool.get(idx2).dna;
		Snake s = new Snake(parentA.crossoverBytewise(parentB, mutationrate), world);
		snakes.add(s);
		if(manualControl && (currentSnake == null || currentSnake.deathFade <= 0)) {
			if(currentSnake != null) {
				System.out.println(currentSnake.score);
			}
			s.manual = true;
			currentSnake = s;
		} else if(runRuleBased && (ruleBased == null || ruleBased.deathFade <= 0 )) {
			if(ruleBased != null) {
				System.out.println("t: "+Long.toString(world.clock / 1000)+"\tRule-based: "+currentMaxFitnessRuleBased+"\t\tBest NN: "+allTimeMaxFitnessNN);
				currentMaxFitnessRuleBased = 0;
			}
			s.ruled = true;
			ruleBased = s;
		}
	}

	/**
	 * Show graphics
	 */
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		// Background:
		g.setColor(Color.black);
		g.fillRect(0, 0, getWidth(), getHeight());

		// Stats:
		if (displayStatisticsActive) {
			g.setColor(Color.DARK_GRAY);
			g.setFont(new Font("Arial", 0, 64));
			g.drawString("t = " + Long.toString(world.clock / 1000), 20, 105);

			g.drawString("g = " + Integer.toString((int) currentGeneration), 20, 205);
			g.setFont(new Font("Arial", 0, 32));
			g.drawString("Mut. Prob.: " + String.format("%1$,.3f", mutationrate), 20, 305);
			g.drawString("Max fitness: " + Integer.toString((int) currentMaxFitness), 20, 355);

			if(runRuleBased && ruleBased != null && ruleBased.debugInputArray != null){
				g.setFont(new Font("Arial", 0, 16));
				g.drawString(ruleBased.debugOutput, 320, 180);
				g.drawString("Rule based snake input (0 is wall, 1 is snake, 2 is food):", 320, 205);
				for(int i = 0; i < ruleBased.debugInputArray.length; i++){
					Snake.Thing t = ruleBased.debugInputArray[i];
					g.drawString("#"+i+"-Type "+t.type+"-Distance: "+t.distance, 330, 225+20*i);
				}
				
			}
			
			
			// print timeline:
			synchronized (fitnessTimeline) {
				if (!fitnessTimeline.isEmpty()) {
					double last = fitnessTimeline.getFirst();
					int x = 0;
					double limit = getHeight();
					if (limit < bestscore)
						limit = bestscore;
					for (Double d : fitnessTimeline) {
						g.setColor(new Color(0, 1, 0, .5f));
						g.drawLine(x, (int) (getHeight() - getHeight() * last / limit), x + 2, (int) (getHeight() - getHeight() * d / limit));
						last = d;
						x += 2;
					}
				}
			}
		}
		if (runRuleBased) {
			g.setColor(Color.DARK_GRAY);
			g.setFont(new Font("Arial", 0, 32));
			g.drawString("Rule Based Mode Enabled", 320, 105);
		}if (manualControl) {
			g.setColor(Color.DARK_GRAY);
			g.setFont(new Font("Arial", 0, 32));
			g.drawString("Manual Mode Enabled", 720, 105);
		}
		
		
		// neural net:
		if (singleSnakeModeActive) {
			snakes.getFirst().brainNet.display(g, 0, world.width, world.height);
		}
		// snakes:
		synchronized (snakes) {
			for (Snake s : snakes)
				s.draw(g);
			world.draw(g);
		}
	}

}
