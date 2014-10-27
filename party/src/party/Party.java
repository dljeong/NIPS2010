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
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.Semaphore;

import jsc.distributions.Laplace;
import Jama.Matrix;

public class Party implements Runnable {

	private final String server = "127.0.0.1";

	private int id, K;
	private ArrayList<Integer> perm;
	private int n, d,v;
	private ArrayList<String[]> localData;
	private ArrayList<double[]> localX;
	private ArrayList<String> localY;
	private Matrix theta;
	private Matrix eta;
	private Matrix combined = null;
	private Matrix eta_combined = null;
	//private String positiveClass = "A";
	private String positiveClass = " 3";
	private double lambda = 1;
	private Socket sc;
	private ArrayList<double[]> testX;
	private ArrayList<String> testY;
	private int testn;
	private int[] personal_k = {3,33};
	private int[] public_k = {7,33};
	private int[] public_k_charlie = {7,33};
	private double EPSILON = 0.1;
	private AtomicInteger shared;
	private Semaphore sema;
	
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


			int i=0;
			while((line = br.readLine()) != null) {

				String[] data = line.split(csvSpliter);
				
				//String y = data[0];
				String y = data[data.length - 1];
				double[] X = new double[data.length - 1];
				
				for(int j=0;j<data.length-1;j++) {
					X[j] = Float.valueOf(data[j]);
					//	X[j] = Float.valueOf(data[j+1]);
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
	

		for(int i=0;i<50;i++) { // 30 is iteration number, In many case, 10~15 is enough
			
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

		int true_positive = 0;
		int true_negative = 0;
		int has_positiveclass=0;
		for(int i=0;i<testn;i++) {
			double[] x = testX.get(i);
			
			double result = sigmoid( theta.transpose().getArray()[0], x );
			if(testY.get(i).equals(positiveClass))
				has_positiveclass++;
			if(result >= 0.5) {
				if(testY.get(i).equals(positiveClass)) {
				//if(testY.get(i).equals("A") || testY.get(i).equals("E") || testY.get(i).equals("O") || testY.get(i).equals("I") || testY.get(i).equals("U") ) {
					true_positive++;
				}
			} else if(!testY.get(i).equals(positiveClass)) {
			//} else if(!(testY.get(i).equals("A") || testY.get(i).equals("E") || testY.get(i).equals("O") || testY.get(i).equals("I") || testY.get(i).equals("U") )) {
				true_negative++;
			}
		}

		if(id == 0) {
			ServerSocket ss;
			Socket[] sc = new Socket[K - 1];
			BufferedReader in;
			
			
			double positive_ratio = (double) true_positive / (double)has_positiveclass;
			double negative_ratio = (double) true_negative / (double)(testn-has_positiveclass);
			double total_ratio = (double)(true_positive + true_negative)/testn;
			
			int clients = 0;
			
			try {
				ss = new ServerSocket(34444 + K);
				
				while(clients < K - 1) {
					sc[clients++] = ss.accept();
				}

				for(int i=0;i<K-1;i++) {
					in = new BufferedReader(new InputStreamReader(sc[i].getInputStream()));
					String inLine = in.readLine();
					total_ratio += Double.valueOf(inLine);
					inLine = in.readLine();
					positive_ratio += Double.valueOf(inLine);
					inLine = in.readLine();
					negative_ratio += Double.valueOf(inLine);
					in.close();
				}
				
				for(int i=0;i<K-1;i++) {
					sc[i].close();
				}
				ss.close();
				
				total_ratio /= K;
				positive_ratio /= K;
				negative_ratio /= K;
				
				//System.out.print(Integer.toString(K) + " " + msg + " ");
				if(msg.equals("combined")) {
					System.out.println("total ratio : "+Double.toString(total_ratio)+" positive ratio : "+Double.toString(positive_ratio)+" negative ratio : "+Double.toString(negative_ratio));
				} else {
				//	System.out.print(Double.toString(ratio) + ",");
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
					out.println(Double.toString( (double)(true_positive+true_negative) / (double)testn ));
					out.println(Double.toString( (double)(true_positive) / (double)has_positiveclass ));
					out.println(Double.toString( (double)(true_negative) / (double)(testn - has_positiveclass) ));
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
	
	public Party(int id, int K,ArrayList<Integer> p,AtomicInteger shared, Lock lock,Semaphore sema) {
		this.id = id; this.K = K;
		this.n = 0;
		this.localData = new ArrayList<String[]>();
		this.localX = new ArrayList<double[]>();
		this.localY = new ArrayList<String>();
		this.testX = new ArrayList<double[]>();
		this.testY = new ArrayList<String>();
		this.testn = 0;
		this.perm = p;
		this.shared = shared;
		this.sema = sema;
	}
	
	public int KK = 10000;
	

	public void send_to_charlie(String msg, int i) {
		Comm.send_to_charlie(msg, i);
	}
	public void send(String msg, int i) {
		Comm.send(msg, i);
	}
	
	public String recv() {
		return Comm.recv(id);
	}
	public void send2(String msg, int i) {
		Comm.send2(msg, i);
	}
	
	public String recv2() {
		return Comm.recv2(id);
	}
	
	public void sendMatrix(Matrix ma) {
		if(id >= KK) return;
		postal p = new postal(ma, id);
		String msg = p.getString();
		for(int i=0;i<K;i++) {
			if(id != i) {
				Comm.send(msg, i);
			}
		}
	}
	public void recvOthersMatrix(Matrix ma) {
		//to check whether one of parties send more than once;
		//boolean[] check = new boolean[K];
		if(id >= KK) return;
		for(int i=0;i<K;i++) {
			if(i != id) {
				String et=Comm.recv(id);
			//	System.out.println(et);
				postal p = new postal(et);
				Matrix others = p.getTheta();
				ma.plusEquals(others);
			}
		}
		
		//combined.timesEquals((double)1/(double)K);
	}

	private synchronized void sync() throws InterruptedException
	{
		if (shared.getAndIncrement() != K - 1)
		{	
			//System.out.println("shared "+shared.get()+ " "+Integer.toString(K));
			sema.acquire();
		
		}
		else
		{
			
			
			sema.release(K-1);
			shared.set(0);
			
			
		}
	}
	
	private void combining() {
		
		int a,b;
		combined = theta.copy();
		
		// step 1
		Random rand = new Random();
		a = rand.nextInt(n) + 1;
		b = n - a;
		//System.out.println("Send Im "+Integer.toString(id)+ " I send to "+Integer.toString(perm.get(id)) +" a : "+Integer.toString(a));
		//step1
		
		send2(Integer.toString(a),perm.get(id));
		
		send_to_charlie(Integer.toString(b),perm.get(id));
		Integer aa =  Integer.parseInt(recv2());
		send_to_charlie(Integer.toString(HomomorphicEncrypt.encrypt(public_k[0],public_k[1] , aa)),id);
		Integer a_plus_r = Integer.parseInt(recv());
		send_to_charlie(Integer.toString(HomomorphicEncrypt.encrypt(personal_k[0],personal_k[1] , a_plus_r)),id);
		
		//step2
		String vv = recv();
		//System.out.println("Got first " + id);
		//sync();

		send2(vv,perm.indexOf(id));
		

		
		
		v = Integer.parseInt(recv2());
		
		
		get_eta();
		sendMatrix(eta);
		
		try {
			sync();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		recvOthersMatrix(eta_combined);
		if(id ==0)
		{
			Matrix s = new Matrix(d,1,0);//should change random integer;
			for(int j=0;j<d;j++)
			{
				s.set(j, 0, HomomorphicEncrypt.encrypt(public_k_charlie[0], public_k_charlie[1],(int) s.get(j,0)));
			}
			Matrix send_value = theta.copy();
			send_value.plusEquals(s);
			for(int i =0; i < K;i++)
			{
				postal p = new postal(send_value, i);
				String msg = p.getString();
				send_to_charlie(msg,i);
			}
		}
		
		sendMatrix(theta);
		recvOthersMatrix(combined);
		combined.timesEquals((double)1/(double)K);
		combined.plusEquals(eta_combined);

	
		

	}
	
	private void get_eta()
	{
		Matrix a = new Matrix(d,1,0);
		Laplace l =new Laplace(0,2/(EPSILON*n*lambda));
		for(int i=0;i<d;i++)
		{
			a.set(i, 0, l.random());
		}
		eta = a.times((double)v);
		eta_combined = eta.copy();
	}

	public void run() {
		//readData("letter-recognition.data");
		readData("pendigits.tes.txt");
		train();
		test(theta, "individual");
		combining();
		//System.out.print("combine "+ id+ " : ");
		//for(int i =0;i<K;i++)
		//{
		//	System.out.print(eta_combined.get(i,0)+", ");
		//}
		//System.out.println("Done");
		try {
			sync();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		test(combined, "combined");
	}
}