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

import jsc.distributions.Laplace;
import Jama.Matrix;

public class Charlie implements Runnable{
	//Acting like socket, temporary variable
	private comm Comm;
	private int min;
	private int k;
	private ArrayList<Integer> perm;
	private ArrayList<Integer> u;	
	private int[] rand;
	private int[] public_k = {7,33};
	private int[] public_k_charlie = {7,33};
	private int[] personal_k_charlie = {7,33};
	private int[] personal_k = {3,33};
	private int min_j;
	private Lock lock;
	private Matrix eta = null;
	
	public void setComm(comm a) {
		Comm = a;
	}
	
	public Charlie(int k,Lock lock)
	{
	 this.k = k;
	 this.lock = lock;
	}
	public void set()
	{
		ArrayList<Integer> permutation = new ArrayList<Integer>();
		rand = new int[k];
		Random random = new Random();
		for(int j=0;j<k;j++)
		{
			permutation.add(j);
			rand[j] = random.nextInt(10) + 1;
		}
		java.util.Collections.shuffle(permutation);
		perm = permutation;
			
	}
	public void send(String msg,int i)
	{
	  Comm.send(msg,i);
	}
	private int get_min(int[] a, int[] b)
	{
		min = a[0] + b[0] ;
		min_j = 0;
		for(int i =0; i < a.length ; i ++)
		{
			if (min > a[i] + b[i])
			{
				min = a[i] + b[i];
				min_j = i;
			}
		}
		return min;
	}
	public void step1()
	{
		int[] b = new int[k];
		String first = null,second,third;
		int[] encrypted_a_plus_r = new int[k];
		int[] a = new int[k];
		for(int i=0;i<k;i++)
		{
			first = Comm.recv_charlie(i);
			second = Comm.recv_charlie(i);
			b[perm.get(i)]=Integer.parseInt(first)- rand[i];
			encrypted_a_plus_r[i]=Integer.parseInt(second)+HomomorphicEncrypt.encrypt(public_k[0], public_k[1], rand[i]);
		}
		
		for(int i = 0;i<k;i++)
		{
			send(Integer.toString(encrypted_a_plus_r[i]),perm.get(i));
		}
		for(int i=0;i<k;i++)
		{
			third = Comm.recv_charlie(i);
			a[i]=Integer.parseInt(third);
		}
		
		int min = get_min(a,b);
		
	}
	public void send_all(double[] uu)
	{
		String[] msg = new String[k];
		for(int i=0;i<k;i++)
		{
			msg[perm.indexOf(i)]=Integer.toString(HomomorphicEncrypt.encrypt(public_k_charlie[0], public_k_charlie[1], (int) uu[perm.indexOf(i)]));
		}
		
		Comm.send_all(msg);
	
	}
	public void step2()
	{
		Matrix u = new Matrix(k,1,0);
		u.set(min_j, 0, 1);
		double[] uu = (double []) u.transpose().getArray()[0]; 
		send_all(uu);
		
	}
	public void step3()
	{
		
		for(int i=0;i <k; i++)
		{
			String et=Comm.recv_charlie(i);
			postal p = new postal(et);
			Matrix others = p.getTheta();
			
			for(int j =0;j < others.getRowDimension(); j++){
				others.set(j, 0, HomomorphicEncrypt.decrypt(personal_k_charlie[0], personal_k_charlie[1],(int) others.get(j,0)));
			}
			if(eta==null)
			{
				eta = new Matrix(others.getRowDimension(),1,0);
			}
			eta.plusEquals(others);
		}
		
		
	}
	public void run() {
		set();
		step1();
		step2();
		step3();
	}
}
