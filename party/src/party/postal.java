package party;

import Jama.Matrix;

public class postal {
	
	private final String delimiter = ":;"; 
	public Matrix theta;
	public int id;
	public String str;

	public postal(String str) {
		String[] tokens = str.split(delimiter);
		
		id = Integer.valueOf(tokens[0]);
		theta = new Matrix(tokens.length - 1, 1);
		for(int i=0;i<tokens.length-1;i++) {
			double a = Double.valueOf(tokens[i+1]);
			theta.set(i, 0, a);
		}
	}
	
	public postal(Matrix m, int id) {
		theta = m;
		this.id = id;
		String[] token = new String[m.getRowDimension() + 1];
		token[0] = Integer.toString(id);
		for(int i=0;i<m.getRowDimension();i++) {
			token[i+1] = Double.toString(m.get(i, 0));
		}
		str = String.join(delimiter, token);
	}
	
	public String getString() {return str;} public int getId() {return id;} public Matrix getTheta() {return theta;}
}
