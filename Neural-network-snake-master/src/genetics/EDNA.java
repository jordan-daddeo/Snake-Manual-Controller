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
		
		newdna.sigma = (byte) (other.sigma / alpha + sigma / alpha);
		
		//mutation of op
		// implement formula (1/[sigma*(2*PI)^0.5])(e^(-0.5*(x/sigma)^2))
		if (newdna.sigma > 0) {
			for (byte val : newdna.op) {
				float expo = val/newdna.sigma;
				expo = (float) Math.pow(expo, 2);
				expo *= -0.5f;
				float eValue = (float) Math.exp(expo);
				float constValue = (float) (1 / (newdna.sigma*(Math.pow(2*Math.PI, 0.5f))));
				float mute = constValue * eValue;
				
				val += mute;
			}
			
			//mutation of sigma
			float expo = 1.0f;
			expo = (float) Math.pow(expo, 2);
			expo *= -0.5f;
			float eValue = (float) Math.exp(expo);
			float constValue = (float) (1 / (newdna.sigma*(Math.pow(2*Math.PI, 0.5f))));
			float mute = constValue * eValue;
			
			newdna.sigma += mute;
		}
		return newdna;
	}
}
