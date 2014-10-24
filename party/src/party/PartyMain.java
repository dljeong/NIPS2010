package party;
import java.util.ArrayList;
import java.util.List;

import jsc.distributions.Laplace;
import Jama.Matrix;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.Semaphore;

public class PartyMain {
	static AtomicInteger shared = new AtomicInteger(0);
	final private static  Lock lock = new ReentrantLock();
	private static Semaphore m_Semaphore;
	  
	private static void CreateParties(int k) {
		//System.out.print(Integer.toString(k) + ",");
		m_Semaphore = new Semaphore(0);
		ArrayList<Integer> permutation = new ArrayList<Integer>();
		for(int j=0;j<k;j++)
		{
			permutation.add(j);
		}
		java.util.Collections.shuffle(permutation);
		
		Thread[] th = new Thread[k+1];
		comm Comm = new comm(k); //commucate with charlie
		
		for(int i=0;i<k;i++) {
			Party p = new Party(i, k, permutation,shared,lock,m_Semaphore);
			p.setComm(Comm);
			
			th[i] = new Thread(p);
			th[i].start();
			//(new Thread(p)).start();
		}
		
		Charlie charlie = new Charlie(k,lock);
		charlie.setComm(Comm);
		th[k]=new Thread(charlie);
		th[k].start();

		
		for(int i=0;i<k;i++) {
			try {
				th[i].join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}


		
	}
	
	public static void main(String[] args) {
		/*int K = 120;
		
		System.out.println("Creating " + Integer.toString(K) + " parties");*/
		for(int j =0;j<=4;j++)
		{
			System.out.println("i = " +Integer.toString(j));
			for(int i=10;i<=100;i+=10) {
				CreateParties(i);
			}
			System.out.println("");
		}
	}
}