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
	public byte sp[];
	
	public EDNA(boolean random, int size){
		op = new byte[size];
		sp = new byte[size];
		
		for (int i = 0; i < op.length; i++){
			op[i] = random?(byte)Math.floor(Math.random()*256d):0;
			sp[i] = random?(byte)Math.floor(Math.random()*256d):0;
		}
	}
	/**
	 * Crossover function which combines this DNA with another DNA object.
	 * Process is done byte-wise and a gaussian noise is added to each byte-value 
	 * Bits flip according to mutation probability
	 */
	public EDNA crossover(EDNA other){  //byte-wise, noise applied to each value
		EDNA newdna = new EDNA(false, op.length);
		int numswaps = op.length/10; 
		int swaps[] = new int[numswaps+1];
		for (int i = 0; i < swaps.length-1; i++){
			swaps[i] = (int)Math.floor(Math.random()*op.length);
		}
		swaps[numswaps] = op.length;  //save last
		Arrays.sort(swaps);
		int swapidx = 0;
		boolean that = true;
		for (int i = 0; i < op.length; i++){
			if (i >= swaps[swapidx]){
				swapidx++;
				that = !that;
			}
			byte d = 0;
			if (that){
				d = this.op[i];
			}
			else {
				d = other.op[i];
			}
			d += (byte)(random.nextGaussian()*256);
			newdna.op[i] = d;
		}
		return newdna;
	}
}
