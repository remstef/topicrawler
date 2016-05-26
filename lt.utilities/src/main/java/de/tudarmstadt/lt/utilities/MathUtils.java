package de.tudarmstadt.lt.utilities;

import java.util.Arrays;
import java.util.Comparator;

import org.apache.commons.lang.ArrayUtils;

public class MathUtils {

	private MathUtils(){ /* DO NOT INSTANTIATE */ }

	public static double log(double x, double base) {
		return Math.log(x) / Math.log(base);
	}

	public static int[] sortIdsByValue(final double[] values){
		return sortIdsByValue(values, false);
	}

	public static int[] sortIdsByValue(final double[] values, final boolean descending){
		Integer[] ids = new Integer[values.length];
		for(int id=0; id < ids.length; ids[id] = id++);
		Arrays.sort(ids, new Comparator<Integer>(){
			@Override
			public int compare(Integer o1, Integer o2) {
				int c = Double.compare(values[o1], values[o2]);
				return descending ? -c : c;
			}
		});
		return ArrayUtils.toPrimitive(ids);
	}

	public static int argMax(final double[] values){
		int maxid = 0;
		double maxvalue = -Double.MAX_VALUE;
		for(int i=0; i < values.length; i++){
			if(values[i] > maxvalue){
				maxvalue = values[i];
				maxid = i;
			}
		}
		return maxid;
	}

	public static double max(final double[] values){
		int maxid = argMax(values);
		return values[maxid];
	}

	public static double median(double[][] values){
		double[] flatvalues = new double[values.length * values.length];
		for(int i = 0; i < values.length; i++){
			for(int j = 0; j < values.length; j++){
				flatvalues[i*values.length+j] = values[i][j];
			}
		}
		if(flatvalues.length % 2 == 0)
			return flatvalues[flatvalues.length / 2];
		int lowerindex = flatvalues.length / 2;
		int upperindex = flatvalues.length / 2 + 1;
		return (flatvalues[lowerindex] + flatvalues[upperindex]) / 2;
	}

	public static double vector_norm(double[] vec){
		double sum = 0;
		for(double v : vec)
			sum += v * v;
		return Math.sqrt(sum);
	}

	public static double vector_dot(double[] vec1, double[] vec2) {
		double sum = 0;
		for(int i = 0; i < vec1.length && i < vec2.length; i++)
			sum += vec1[i] * vec2[i];
		return sum;
	}

	public static double euclidean_distance(double[] vec1, double[] vec2){
		double dist = 0;
		for(int i = 0; i < vec1.length && i < vec2.length; i++)
			dist += Math.pow(vec1[i] - vec2[i], 2);
		dist = Math.sqrt(dist);
		return dist;
	}

	public static double KL_sym(double[] p, double[] q){
		assert p.length == q.length : "p and q must be probabilities distributions of equal length!";
		double d = 0;
		for(int i = 0; i < p.length; i++){
			if(p[i] == 0 || q[i] == 0)
				continue;
			double d1 = p[i] * Math.log(p[i] / q[i]);
			double d2 = q[i] * Math.log(q[i] / p[i]);
			d += (d1 + d2) / 2;
		}
		return d;
	}

	public static double cosine_similarity(double[] vec1, double[] vec2){
		double cosim = vector_dot(vec1,vec2) / (vector_norm(vec1) * vector_norm(vec2));
		return cosim;
	}


}
