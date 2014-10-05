package party;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

import jsc.distributions.Laplace;
import Jama.Matrix;

public class Party implements Runnable {

	private final String server = "127.0.0.1";

	private int id, K;
	private int n, d;
	private ArrayList<String[]> localData;
	private ArrayList<double[]> localX;
	private ArrayList<String> localY;
	private Matrix theta;
	private Matrix combined = null;
	private String positiveClass = "D";
	private double lambda = 00;
	private Socket sc;
	private ArrayList<double[]> testX;
	private ArrayList<String> testY;
	private int testn;
	
	
	//Acting like socket, temporary variable
	private comm Comm;
	
	public void setComm(comm a) {
		Comm = a;
	}

	private void readData(String filename) {
		
		String dataFile = filename;
		BufferedReader br = null;
		String line = "";
		String csvSpliter = ",";
		
		try {
			
			br = new BufferedReader(new FileReader(dataFile));
			//br.readLine();

			int i=0;
			while((line = br.readLine()) != null) {

				String[] data = line.split(csvSpliter);
				
				String y = data[0];
				
				double[] X = new double[data.length - 1];
				
				for(int j=0;j<data.length-1;j++) {
					X[j] = Float.valueOf(data[j+1]);
				}

				//String y = data[data.length - 1];
				
				testX.add(X);
				testY.add(y);
				testn++;
				if((i % K) == id) {
					localData.add(data);
					localX.add(X);
					localY.add(y);
					n++;
				}
				
				i++;
			}

		} catch(FileNotFoundException e) {
			e.printStackTrace();
		} catch(IOException e) {
			e.printStackTrace();
		} finally {
			if(br != null) {
				try {
					br.close();
				} catch(IOException e) {
					e.printStackTrace();
				}
			}
		}
		
		d = localX.get(0).length;
	}

	private double sigmoid(double r) {
		return 1.0/(1.0 + Math.exp(-r));
	}
	private double sigmoid(double[] a, double[] b) {
		double r = 0;
		for(int i=0;i<a.length;i++) {
			r += a[i] * b[i];
		}
		return sigmoid(r);
	}
	
	private void train() {
		// By logistic regression
	
		theta = new Matrix(d, 1, 0);

		double[][] X = (double[][])localX.toArray(new double[0][0]);

		for(int i=0;i<30;i++) { // 30 is iteration number, In many case, 10~15 is enough
			
			Matrix grad = new Matrix(d, 1);
			Matrix hessian = new Matrix(d, d);
			
			for(int j=0;j<n;j++) {
				
				double sig = sigmoid(theta.transpose().getArray()[0], X[j]);
				
				//temporally, D is class1, else class 0 
				double valuey;
				if(localY.get(j).equals(positiveClass)) {
				//if(localY.get(j).equals("A") || localY.get(j).equals("E") || localY.get(j).equals("O") || localY.get(j).equals("I") || localY.get(j).equals("U") ) {
					valuey = 1;
				} else {
					valuey = 0;
				}
				
				// calculating gradient with current theta
				double gcoef = (sig - valuey);
				for(int k=0;k<d;k++) {
					grad.getArray()[k][0] += gcoef * X[j][k];
				}
				
				
				// calculating hessian with current theta
				double hcoef = sig * (1 - sig);
				for(int k1=0;k1<d;k1++) {
					for(int k2=0;k2<d;k2++) {
						hessian.getArray()[k1][k2] += hcoef * X[j][k1] * X [j][k2];
					}
				}
			}
			
			// updating theta
			if(hessian.det() == 0) { //positive definite hessian?
				break;
			}

			//regularizaion
			grad.plusEquals(theta.times(2*lambda));
			Matrix temp = Matrix.identity(d, d).times(2*lambda); temp.set(0, 0, 0);
			hessian.plusEquals(temp);
			
			theta.minusEquals(hessian.inverse().times(grad));
		}
	}
	
	private void test(Matrix theta, String msg) {
		//Using training dataset as test dataset 

		int count = 0;
		
		for(int i=0;i<testn;i++) {
			double[] x = testX.get(i);
			
			double result = sigmoid( theta.transpose().getArray()[0], x );
		
			if(result >= 0.5) {
				if(testY.get(i).equals(positiveClass)) {
				//if(testY.get(i).equals("A") || testY.get(i).equals("E") || testY.get(i).equals("O") || testY.get(i).equals("I") || testY.get(i).equals("U") ) {
					count++;
				}
			} else if(!testY.get(i).equals(positiveClass)) {
			//} else if(!(testY.get(i).equals("A") || testY.get(i).equals("E") || testY.get(i).equals("O") || testY.get(i).equals("I") || testY.get(i).equals("U") )) {
				count++;
			}
		}

		if(id == 0) {
			ServerSocket ss;
			Socket[] sc = new Socket[K - 1];
			BufferedReader in;
			
			double ratio = (double) count / (double)testn;
			
			int clients = 0;
			
			try {
				ss = new ServerSocket(34444 + K);
				
				while(clients < K - 1) {
					sc[clients++] = ss.accept();
				}

				for(int i=0;i<K-1;i++) {
					in = new BufferedReader(new InputStreamReader(sc[i].getInputStream()));
					String inLine = in.readLine();
					ratio += Double.valueOf(inLine);
					in.close();
				}
				
				for(int i=0;i<K-1;i++) {
					sc[i].close();
				}
				ss.close();
				
				ratio /= K;
				
				//System.out.println(Integer.toString(K) + " " + msg + " " + Double.toString(ratio));
				if(msg.equals("combined")) {
					System.out.println(Double.toString(ratio));
				} else {
					System.out.print(Double.toString(ratio) + ",");
				}
				
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			//send to party 0
			
			sc = null;
			while(true) {
				try {
		 			sc = new Socket(server, 34444 + K);
					PrintWriter out = new PrintWriter(sc.getOutputStream(), true);
					out.println(Double.toString( (double)count / (double)testn ));
					sc.close();
					break;
					
				} catch(IOException e) {
					try {
						Thread.sleep(100);
					} catch (InterruptedException e1) {
						e1.printStackTrace();
					}
				}
			}
		}
	}
	
	public Party(int id, int K) {
		this.id = id; this.K = K;
		this.n = 0;
		this.localData = new ArrayList<String[]>();
		this.localX = new ArrayList<double[]>();
		this.localY = new ArrayList<String>();
		this.testX = new ArrayList<double[]>();
		this.testY = new ArrayList<String>();
		this.testn = 0;
	}
	
	public int KK = 10000;
	
	public void sendMytheta() {
		if(id >= KK) return;
		postal p = new postal(theta, id);
		String msg = p.getString();
		for(int i=0;i<K;i++) {
			if(id != i) {
				Comm.send(msg, i);
			}
		}
	}
	
	public void recvOtherstheta() {
		//to check whether one of parties send more than once;
//		boolean[] check = new boolean[K];
		if(id >= KK) return;
		for(int i=0;i<K;i++) {
			if(i != id) {
				postal p = new postal(Comm.recv(id));
			
				Matrix others = p.getTheta();
				combined.plusEquals(others);
			}
		}
		
		combined.timesEquals((double)1/(double)K);
	}
	
	private void combining() {
		combined = theta;
		sendMytheta();
		recvOtherstheta();
	}
	
	public void run() {
		readData("letter-recognition.data");
		train();
		test(theta, "individual");
		combining();
		test(combined, "combined");
	}
}