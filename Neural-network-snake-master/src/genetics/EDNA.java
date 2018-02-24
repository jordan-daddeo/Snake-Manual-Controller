package genetics;

import java.util.Arrays;
import java.util.Random;

public class EDNA {
	/**
	 * Class to model DNA strands, mutation and crossover
	 * Two components: object parameters and strategy parameters
	 * 	op encodes coeffs of EStage
	 *  sp encodes mutation rates for op
	 */
	public Random random = new Random();
	public byte op[];
	public byte sigma;
	
	public static final byte alpha = 2;
	
	public EDNA(boolean random, int size){
		op = new byte[size];
		//sp = new byte[size];
		
		for (int i = 0; i < op.length; i++){
			op[i] = random?(byte)Math.floor(Math.random()*256d):0;
		}
		sigma = random?(byte)Math.floor(Math.random()*256d):0;
	}
	/**
	 * Crossover function which combines this DNA with another DNA object.
	 * Process is done byte-wise via whole arithmetic crossover
	 * 
	 */
	public EDNA crossoverWithMutation(EDNA other){  //byte-wise, noise applied to each value
		EDNA newdna = new EDNA(false, op.length);

		//whole arithmetic crossover
		for(int i = 0; i < other.op.length; i++){
			newdna.op[i] = (byte) (other.op[i] / alpha + op[i] / alpha);
		}
		
		//mutation of op
		
		//mutation of sigma
		
		return newdna;
	}
}
