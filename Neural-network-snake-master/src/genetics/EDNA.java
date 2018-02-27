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
	
	public static final boolean DISCRETE_RECOMBINATION = true;
	public static final float ALPHA = 0.5f;
	
	public static final boolean DEBUG_OUTPUT_DNA = false;
	
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
	 * Process is done byte-wise via whole arithmetic crossover or discrete recombination
	 * 
	 */
	public EDNA crossoverWithMutation(EDNA other){  //byte-wise, noise applied to each value
		EDNA newdna = new EDNA(false, op.length);

		
		//discrete recombination: randomly select one of two alleles
		if(DISCRETE_RECOMBINATION){
			for(int i = 0; i < op.length-1; i++){
				newdna.op[i] = (byte) (random.nextBoolean()?other.op[i]:op[i]);
				}
			
			newdna.sigma = (byte) (random.nextBoolean()?other.sigma:sigma);
		}
		//whole arithmetic crossover
		//used for hue, poor results if used for other genes
		else{
			for(int i = 0; i < op.length-1; i++){
				newdna.op[i] = (byte) (other.op[i] * ALPHA + op[i] * (1-ALPHA));
			}
			newdna.sigma = (byte) (other.sigma * ALPHA + sigma * (1-ALPHA));
		}
		newdna.op[op.length-1] = (byte) (other.op[op.length-1] * ALPHA + op[op.length-1] * (1-ALPHA));
		
		//mutation of op
		// implement formula (1/[sigma*(2*PI)^0.5])(e^(-0.5*(x/sigma)^2))
		if (newdna.sigma > 0) {
			for (int i = 0; i < newdna.op.length; i++) {
				float expo = newdna.op[i]/newdna.sigma;
				expo = (float) Math.pow(expo, 2);
				expo *= -0.5f;
				float eValue = (float) Math.exp(expo);
				float constValue = (float) (1 / (newdna.sigma*(Math.pow(2*Math.PI, 0.5f))));
				float mute = constValue * eValue;
				
				byte oldval = newdna.op[i];
				newdna.op[i] += mute * sigma * 10;
				//System.out.println("val"+oldval+" mutation: "+mute * sigma * 10+" new val:"+newdna.op[i]);
			}
			
			//mutation of sigma
			float expo = 1.0f;
			expo = (float) Math.pow(expo, 2);
			expo *= -0.5f;
			float eValue = (float) Math.exp(expo);
			float constValue = (float) (1 / (newdna.sigma*(Math.pow(2*Math.PI, 0.5f))));
			float mute = constValue * eValue;
			//System.out.println(mute * sigma * 50);
			newdna.sigma += mute * sigma * 10;
		}
		if (DEBUG_OUTPUT_DNA) {
			
		String oop1 = "["+sigma+"]", oop2 = "["+other.sigma+"]",nop = "["+newdna.sigma+"]";
		for (Byte b : op)oop1+=" "+b;
		for (Byte b : other.op)oop2+=" "+b;
		for (Byte b : newdna.op)nop+=" "+b;
		System.out.println("\nOne: "+oop1+"\nTwo: "+oop2+"\nNew: "+nop);
		}
		
		return newdna;
	}
}
