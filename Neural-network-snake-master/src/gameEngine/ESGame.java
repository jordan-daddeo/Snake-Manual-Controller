package gameEngine;

import helpers.KeyboardListener;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JComponent;

import genetics.DNA;
import genetics.EDNA;

public class ESGame extends JComponent {
	// main update frequency:
	public static final long UPDATEPERIOD = 8;
	public double per = UPDATEPERIOD;

	// constants:
	public static final int globalCircleRadius = 20;
	public static final int numSnakes = 8;
	public static final int numParents = 6;
	public static final int numNibbles = 4;
	
	// Genetics parameter initialization:
	public double currentGeneration = 0;
	public int intGeneration = 0;
	public boolean newEra = true;
	

	// world and snakes initialization:
	public World world = new World();
	public LinkedList<ESnake> snakes = new LinkedList<ESnake>();
	public LinkedList<ESnake> backupSnakes = new LinkedList<ESnake>(); // to
																		// resume
																		// from
																		// single
																		// mode
	
	// Best:
	public EDNA bestDna = null;
	public double bestscore = 0;

	// Statistics:
	public LinkedList<Double> fitnessTimeline = new LinkedList<Double>();
	public double currentMaxFitness = 0;
	public double allTimeMaxFitnessNN = 0;
	public double currentMaxFitnessRuleBased = 0;

	// Mode control:
	public boolean displayStatisticsActive = false;
	public boolean simulationPaused = false;

	/**
	 * Component with the main loop This should be separated from the graphics,
	 * but I was to lazy.
	 */
	public ESGame(KeyboardListener keyb) {
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
									if (world.clock - statisticsLastMillis > 1000) {
										fitnessTimeline.addLast(currentMaxFitness);
										currentMaxFitness = 0;
										if (fitnessTimeline.size() >= world.width / 2) {
											fitnessTimeline.removeFirst();
										}
										statisticsLastMillis = world.clock;
									}
								}
								for (ESnake s : snakes) {
									if (!s.update(world)) {
										deadCount++;
									}
									if (s.getFitness() > currentMaxFitness)
										currentMaxFitness = s.getFitness();
										if(currentMaxFitness > allTimeMaxFitnessNN) allTimeMaxFitnessNN = currentMaxFitness;
									if (s.getFitness() > currentMaxFitnessRuleBased){
										currentMaxFitnessRuleBased = s.getFitness();
									}
									if (s.getFitness() > bestscore) {
										bestscore = s.getFitness();
										bestDna = s.dna;
									}
								}
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
										ArrayList<ESnake> snakesSorted = new ArrayList<>();
										snakesSorted.addAll(snakes);
										Collections.sort(snakesSorted);
										System.out.println("Generation: " + Integer.toString(intGeneration) + " Sigma: " + snakesSorted.get(0).dna.sigma + " Max Fitness: " + Double.toString(currentMaxFitness));
									}
									
								}
								Iterator<ESnake> it = snakes.iterator();
								while (it.hasNext()) {
									ESnake s = it.next();
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
			snakes.add(new ESnake(null, world));
		}
		world.reset();
	}

	/**
	 * Creates the mating pool out of the snake-list
	 * 
	 * @return Mating pool as list
	 */
	@SuppressWarnings("unchecked")
	public ArrayList<ESnake> makeMatingpool() {
		ArrayList<ESnake> matingpool = new ArrayList<ESnake>();
		// get maximum fitness:
		double maxscore = 0;
		for (ESnake s : snakes) {
			if (s.getFitness() > maxscore ) {
				maxscore = s.getFitness();
			}
		}

		//sort snakes by fitness
		ArrayList<ESnake> snakesSorted = new ArrayList<>();
		snakesSorted.addAll(snakes);
		Collections.sort(snakesSorted);
		
		//add p best snakes
		for(int i = snakesSorted.size()-1; i > snakesSorted.size() -1 - numParents; i--){
			//System.out.println(i+" "+snakesSorted.get(i).getFitness());
			matingpool.add(snakesSorted.get(i));
		}
		
		return matingpool;
	}

	/**
	 * Creates a new snake using the genetic algorithm and adds it to the
	 * snake-list
	 */
	public void newSnake() {
		//mutationrate = 10 / currentMaxFitness;
		ArrayList<ESnake> matingpool = makeMatingpool();
		int idx1 = (int) (Math.random() * matingpool.size());
		int idx2 = (int) (Math.random() * matingpool.size());
		EDNA parentA = matingpool.get(idx1).dna;
		EDNA parentB = matingpool.get(idx2).dna;
		ESnake s = new ESnake(parentA.crossoverWithMutation(parentB), world);
		snakes.add(s);
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

			g.drawString("g = " + Integer.toString(intGeneration), 20, 205);
			g.setFont(new Font("Arial", 0, 32));
			//g.drawString("Mut. Prob.: " + String.format("%1$,.3f", mutationrate), 20, 305);
			g.drawString("Max fitness: " + Integer.toString((int) currentMaxFitness), 20, 355);

			g.setFont(new Font("Arial", 0, 16));
			g.drawString("#: <Sigma> | <Fitness>", 330, 180);
			for(int i = 0; i < snakes.size(); i++){
				ESnake s = snakes.get(i);
				g.drawString("#"+i+": "+s.dna.sigma+ "|"+s.getFitness(), 330, 205+20*i);
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
		
		// snakes:
		synchronized (snakes) {
			for (ESnake s : snakes)
				s.draw(g);
			world.draw(g);
		}
	}

}

