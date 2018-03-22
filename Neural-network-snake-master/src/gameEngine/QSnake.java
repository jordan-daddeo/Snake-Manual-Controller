package gameEngine;

import helpers.DoubleMath;

import java.awt.Color;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import qLearning.QTable;

public class QSnake implements Comparable {

	// Movement constants:
	public static final double maximumForwardSpeed = 5;
	public static final double maximumAngularSpeed = Math.PI / 32d;
	public static final double wallCollisionThreshold = 4;
	// view constants:
	public static final double maximumSightDistance = 600;
	public static final double fieldOfView = Math.PI * 2 / 3;
	// State division constants:
	public static final int FOV_DIVISIONS = 2; //number of divisions on half the snake's field of view
	public static final double[] DEPTH_DIVISIONS = {50,100,300,600};
	//public static final int DEPTH_DIVISIONS = 3; //number of divisions of depth perception
	public static final int OBJECT_TYPES = 3; //Number of things detectable

	//SARSA/Q Learning constants:
	private static final double ALPHA = 0.8;
	private static final double GAMMA = 0.8;
	private static final double EPSILON = 0.05;
	public static final double[] ACTIONS = {maximumAngularSpeed/2,0, -maximumAngularSpeed, -maximumAngularSpeed/2, maximumAngularSpeed};
	
	// scoring constants:
	public static final double nibblebonus = 20;
	public static final int healthbonus = 10; // Added each time snake eats
	public static final double healthdecrement = .02; // decremented each loop

	// misc:
	public final boolean displayCuteEyes = false; // try it out yourself :)
	public final boolean snakeInertia = false;

	// basic snake attributes:
	public ArrayList<PhysicalCircle> snakeSegments = new ArrayList<PhysicalCircle>(100);
	public QTable Q;
	public double age = 0;
	public double angle;
	public double score;
	public boolean isDead;
	public float hue;
	public double deathFade = 180;
	public double health;
	
	public Thing[] debugInputArray = null;
	public String debugOutput = "";

	public int State = -1;
	public int Action = 0;
	public int nibblesJustEaten = 0;
	/**
	 * Initializes a new snake with given DNA
	 * 
	 * @param Q
	 *            Q Table shared by all snakes. Cannot be null.
	 * @param world
	 *            reference to the world for spawn point
	 */

	public QSnake(QTable Q, World world) {
		double x = Math.random() * (world.width - 2 * wallCollisionThreshold - 2 * GameLoop.globalCircleRadius) + wallCollisionThreshold
				+ GameLoop.globalCircleRadius;
		double y = Math.random() * (world.height - 2 * wallCollisionThreshold - 2 * GameLoop.globalCircleRadius) + wallCollisionThreshold
				+ GameLoop.globalCircleRadius;

		if (Q == null) {
			throw new RuntimeException("Cannot initialize QSnake with null QTable");
		} else {
			this.Q = Q;
		}
		snakeSegments.clear();
		for (int i = 0; i < 1; i++) {
			snakeSegments.add(new PhysicalCircle(x, y, GameLoop.globalCircleRadius));
		}
		this.angle = Math.atan2(world.height / 2 - y, world.width / 2 - x);
		

		hue = (float) (Math.random());
		score = 0;
		deathFade = 180;
		isDead = false;
		health = healthbonus * 3 / 2;
		age = 0;
	}

	/**
	 * Movement, aging and collisions
	 * 
	 * @param world
	 *            reference to the world
	 * @return true when snake died that round.
	 */
	public boolean update(World world, boolean updateAngle) {
		if (isDead) {
			deathFade -= .6;
			return true;
		}
		age += .1;
		
		double slowdown = 49d / (48d + snakeSegments.size());
		PhysicalCircle head = snakeSegments.get(0);
		// calculate neural net
		double angleIncrement;
		if(updateAngle)angleIncrement = brain(world);
		else angleIncrement = ACTIONS[Action];

		angle += slowdown * angleIncrement;
		angle = DoubleMath.doubleModulo(angle, Math.PI * 2);

		// collision with wall:
		if (head.x - head.rad < wallCollisionThreshold) {
			score /= 2;
			snakeDies();
			isDead = true;
		}
		if (head.x + head.rad > world.width - wallCollisionThreshold) {
			score /= 2;
			snakeDies();
			isDead = true;
		}
		if (head.y - head.rad < wallCollisionThreshold) {
			score /= 2;
			snakeDies();
			isDead = true;
		}
		if (head.y + head.rad > world.height - wallCollisionThreshold) {
			score /= 2;
			snakeDies();
			isDead = true;
		}
		// Main movement:
		head.vx = maximumForwardSpeed * slowdown * Math.cos(angle);
		head.vy = maximumForwardSpeed * slowdown * Math.sin(angle);

		PhysicalCircle previous = head;
		for (int i = 0; i < snakeSegments.size(); i++) {
			PhysicalCircle c = snakeSegments.get(i);
			if (snakeInertia){
				c.followBouncy(previous);
			} else {
				c.followStatic(previous);
			}
			
			c.updatePosition();
			for (int j = 0; j < i; j++) {
				c.collideStatic(snakeSegments.get(j));
			}
			previous = c;
			if (i > 1 && head.isColliding(c, 0)) {
				isDead = true;
				score /= 2;
				snakeDies();
				break;
			}
		}
		// Check eaten nibbles:
		LinkedList<PhysicalCircle> nibblesToRemove = new LinkedList<PhysicalCircle>();
		int nibbleEatCount = 0;
		for (PhysicalCircle nibble : world.getNibbles()) {
			if (head.isColliding(nibble, -10)) {
				score += world.calcValue(nibble);
				snakeSegments.add(new PhysicalCircle(snakeSegments.get(snakeSegments.size() - 1).x, snakeSegments.get(snakeSegments.size() - 1).y, nibble.rad));
				nibblesToRemove.add(nibble);
				nibbleEatCount++;
			}
		}
		nibblesJustEaten += nibbleEatCount;
		score += nibbleEatCount * nibblebonus;
		world.newNibble(nibbleEatCount);
		world.removeNibbles(nibblesToRemove);

		// health / hunger:
		health += nibbleEatCount * healthbonus;
		if (health > 3 * healthbonus) // saturate
			health = 3 * healthbonus;
		health -= healthdecrement;
		if (health <= 0) {
			isDead = true;
			score /= 2;
		}
		return !isDead;
	}

	/**
	 * Fitness function
	 * 
	 * @return a value representing the fitness of the snake
	 */
	public double getFitness() {
		return score + health / 4;
	}

	/**
	 * Struct for see-able objects No enum was used for simpler conversion to
	 * array
	 */
	public class Thing {
		public double distance = maximumSightDistance;
		public int type = 0;
		// Wall = 0;
		// Snake = 1;
		// Nibble = 2;
	}

	/**
	 * Main calculation
	 * 
	 * @param world
	 *            reference to the world for environment information
	 * @return angle increment to move
	 */
	public double brain(World world) {
		// init input vector:
		Thing input[] = new Thing[FOV_DIVISIONS * 2];
		for (int i = 0; i < FOV_DIVISIONS * 2; i++)
			input[i] = new Thing();
		// nibbles:
		input = updateVisualInput(input, world.getNibbles(), 2);
		// snake:
		input = updateVisualInput(input, snakeSegments, 1);
		// walls:
		/*
		 * (This should be replaced by a better algorithm) It is basically a
		 * brute force attempt converting the continuous border lines into many
		 * PhysicalCirle objects to apply the same algorithm When someone comes
		 * up with a better algorithm, please let me know :)
		 */
		int step = (int) (maximumSightDistance * Math.sin(fieldOfView / (FOV_DIVISIONS * 1d))) / 20;
		LinkedList<PhysicalCircle> walls = new LinkedList<PhysicalCircle>();
		for (int x = 0; x < world.width; x += step) {
			walls.add(new PhysicalCircle(x, 0, 1));
			walls.add(new PhysicalCircle(x, world.height, 1));
		}
		for (int y = 0; y < world.height; y += step) {
			walls.add(new PhysicalCircle(0, y, 1));
			walls.add(new PhysicalCircle(world.width, y, 1));
		}
		input = updateVisualInput(input, walls, 0);
		
		double angleIncrement = qLearningBrain(input);
		if (angleIncrement > maximumAngularSpeed)
			angleIncrement = maximumAngularSpeed;
		if (angleIncrement < -maximumAngularSpeed)
			angleIncrement = -maximumAngularSpeed;
		return angleIncrement;
	}

	private double qLearningBrain(Thing[] input) {
		
		int lastState = State;
		int lastAction = Action;
		
		//Determine new state and reward:
		State = 0;
		int m = 1;
		double Reward = 0;
		double closestWall = Double.MAX_VALUE;
		for(Thing t : input){
			//State: Value w/ one-to-one correspondence with environment as divided into sections
			//int dist = (int) Math.round(t.distance / maximumSightDistance * (DEPTHDIVISIONS-1));//even distribution of depth
			int dist = -1;
			for(int i = 0; i < DEPTH_DIVISIONS.length; i++){
				if(t.distance <= DEPTH_DIVISIONS[i]){
					dist = i;
					break;
				}
			}
			if(dist==-1)continue;
			//if(t.type==1)t.type=0;
			State+=(m * (dist*OBJECT_TYPES + t.type));
			m*= DEPTH_DIVISIONS.length * OBJECT_TYPES;
			
			//Reward: Increases with proximity to food.
			if(t.type == 2){
				Reward+= (maximumSightDistance - t.distance);
			}
			//decreases with proximity to closest walls or obstacles
			else{
				//Reward-=(maximumSightDistance - t.distance)*0.5;
				if(closestWall >= t.distance){
					closestWall = t.distance;
				}
			}
		}
		//penalty for closest wall
		if(closestWall!=Double.MAX_VALUE){
			Reward-=(maximumSightDistance - closestWall);
			//System.out.println(closestWall+" rew: "+Reward);
		}
		
		//bonus to reward if food was just eaten
		Reward+=nibblesJustEaten * maximumSightDistance*50;
		//if(nibblesJustEaten>0)System.out.println(nibblesJustEaten+" rew: "+Reward);
		nibblesJustEaten = 0;

		
		//select new action via selection policy (in this case, epsilon-greedy):
		double rand = Math.random();
		if(rand >= EPSILON){
			//a' = whichever a' gives max Q(s',a')
			double maxQ = Double.NEGATIVE_INFINITY;
			for(int a = 0; a < ACTIONS.length; a++){
				//System.out.println(a+" "+Q.get(State, a)+ "max Q:"+maxQ);
				if(Q.get(State, a) > maxQ){
					Action = a;
					maxQ = Q.get(State, a);
				}
			}
			//if(maxQ == 0) rand = EPSILON; //if they are all uninitialized goto random, breaks w/ negative rewards
		}
		if(rand < EPSILON){
			//select a' randomly
			Action = (int) (Math.random()*ACTIONS.length);
		}

		//record Q(lastState,lastAction) using SARSA
		//Q(s,a) := Q(s,a) + alpha(Reward(s') + gamma * Q(s',a') - Q(s,a))
		if(lastState != -1) {
			Q.set(lastState, lastAction, Q.get(lastState, lastAction) + ALPHA * (Reward + GAMMA * Q.get(State, Action) - Q.get(lastState, lastAction)));
		}
		
		return ACTIONS[Action];
	}

	//called when snake dies of collision (NOT hunger)
	private void snakeDies() {
		//record Q(lastState,lastAction) using SARSA
		//Q(s,a) := Q(s,a) + alpha(Reward(s') + gamma * Q(s',a') - Q(s,a))
		if(State != -1) {
			double Reward = maximumSightDistance * -5;
			Q.set(State, Action, Q.get(State, Action) + ALPHA * (Reward - Q.get(State, Action)));
			//System.out.println("Death "+Reward);
		}
		
	}

	/**
	 * Function to update input vector Input Vector contains distance and type
	 * of closest objects seen by each visual cell This function replaces those
	 * by "Things" closer to the head of the snake Objects further away or
	 * outside the FOV are ignored
	 * 
	 * @param input
	 *            Array of the current things seen by the snake
	 * @param objects
	 *            List of objects to be checked
	 * @param type
	 *            Thing-Type: 0: Wall, 1: Snake, 2: Nibble
	 * @return Updated input array
	 */
	private Thing[] updateVisualInput(Thing input[], List<PhysicalCircle> objects, int type) {
		PhysicalCircle head = snakeSegments.get(0);
		for (PhysicalCircle n : objects) {
			if (head == n)
				continue;
			double a = DoubleMath.signedDoubleModulo(head.getAngleTo(n) - angle, Math.PI * 2);
			double d = head.getDistanceTo(n);
			if (a >= 0 && a < fieldOfView) {
				if (d < input[(int) (a * FOV_DIVISIONS / fieldOfView)].distance) {
					input[(int) (a * FOV_DIVISIONS / fieldOfView)].distance = d;
					input[(int) (a * FOV_DIVISIONS / fieldOfView)].type = type;
				}
			} else if (a <= 0 && -a < fieldOfView) {
				if (d < input[(int) (-a * FOV_DIVISIONS / fieldOfView) + FOV_DIVISIONS].distance) {
					input[(int) (-a * FOV_DIVISIONS / fieldOfView) + FOV_DIVISIONS].distance = d;
					input[(int) (-a * FOV_DIVISIONS / fieldOfView) + FOV_DIVISIONS].type = type;
				}
			}
		}
		return input;
	}

	/**
	 * Draws the snake to Graphics
	 * 
	 * @param g
	 *            Graphics object to draw to
	 */
	public void draw(Graphics g) {
		// Snake body
		int alpha = (int) deathFade;
		for (int i = 0; i < snakeSegments.size(); i++) {
			Color c = new Color(Color.HSBtoRGB(hue, 1 - (float) i / ((float) snakeSegments.size() + 1f), 1));
			g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha));
			PhysicalCircle p = snakeSegments.get(i);
			g.fillOval((int) (p.x - p.rad), (int) (p.y - p.rad), (int) (2 * p.rad + 1), (int) (2 * p.rad + 1));
		}
		// Cute Eyes. A bit computationally expensive, so can be turned of
		if (displayCuteEyes) {

			PhysicalCircle p = snakeSegments.get(0); // get head
			double dist = p.rad / 2.3;
			double size = p.rad / 3.5;
			g.setColor(new Color(255, 255, 255, alpha));
			g.fillOval((int) (p.x + p.vy * dist / p.getAbsoluteVelocity() - size), (int) (p.y - p.vx * dist / p.getAbsoluteVelocity() - size),
					(int) (size * 2 + 1), (int) (size * 2 + 1));
			g.fillOval((int) (p.x - p.vy * dist / p.getAbsoluteVelocity() - size), (int) (p.y + p.vx * dist / p.getAbsoluteVelocity() - size),
					(int) (size * 2 + 1), (int) (size * 2 + 1));
			size = p.rad / 6;
			g.setColor(new Color(0, 0, 0, alpha));
			g.fillOval((int) (p.x + p.vy * dist / p.getAbsoluteVelocity() - size), (int) (p.y - p.vx * dist / p.getAbsoluteVelocity() - size),
					(int) (size * 2 + 1), (int) (size * 2 + 1));
			g.fillOval((int) (p.x - p.vy * dist / p.getAbsoluteVelocity() - size), (int) (p.y + p.vx * dist / p.getAbsoluteVelocity() - size),
					(int) (size * 2 + 1), (int) (size * 2 + 1));
		}
	}
	
	public int compareTo(Object other) {
		if(other instanceof QSnake){
			return new Double(this.getFitness()).compareTo(new Double(((QSnake) other).getFitness()));
		}
		return 0;
	}
}
