package qLearning;

import gameEngine.QSnake;

public class QTable {
	
	public int numStates;
	public int numActions;
	private double[][] data;
	
	public QTable(){
		//determine number of states
		numStates = (int) Math.pow((QSnake.DEPTH_DIVISIONS.length * QSnake.OBJECT_TYPES),QSnake.FOV_DIVISIONS*2);
		//determine number of actions
		numActions = QSnake.ACTIONS.length;
		data = new double[numStates][numActions];
		System.out.println("Created table of size "+numStates+" by "+numActions);
	}
	
	public synchronized double get(int State, int Action){
		return data[State][Action];
	}
	
	public synchronized void set(int State, int Action, double value){
		data[State][Action] = value;
		//System.out.println("Q("+State+","+Action+") := "+value);
	}
}
