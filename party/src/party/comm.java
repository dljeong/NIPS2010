package party;

import java.util.ArrayList;
import java.util.LinkedList;

public class comm {
	
	boolean flag = false;
	ArrayList< LinkedList<String> > q;
	ArrayList< LinkedList<String> > q2;
	ArrayList< LinkedList<String> > charlie;
	
	public synchronized void send(String msg, int i) {
		LinkedList<String> uQ = q.get(i);
		uQ.offer(msg);
		notifyAll();
	}
	
	public synchronized void send2(String msg, int i) {
		LinkedList<String> uQ = q2.get(i);
		uQ.offer(msg);
		notifyAll();
	}
	
	public synchronized void send_all(String[] msg) {
		for(int i=0;i < msg.length;i++)
		{
			LinkedList<String> uQ = q.get(i);
			uQ.offer(msg[0]);
		}
		notifyAll();
	}

	public synchronized void send_to_charlie(String msg, int i) {
		LinkedList<String> uQ = charlie.get(i);
		uQ.offer(msg);
		notifyAll();
	}

	public synchronized String recv(int i) {
		
		LinkedList<String> myQ = q.get(i);
		
		while(myQ.isEmpty()) {
			try {
				wait();
			} catch(InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		String rv = myQ.poll();
		
		return rv;
	}
public synchronized String recv2(int i) {
		
		LinkedList<String> myQ = q2.get(i);
		
		while(myQ.isEmpty()) {
			try {
				wait();
			} catch(InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		String rv = myQ.poll();
		
		return rv;
	}
	public synchronized String recv_charlie(int i) {
		
		LinkedList<String> myQ = charlie.get(i);
		
		while(myQ.isEmpty()) {
			try {
				wait();
			} catch(InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		String rv = myQ.poll();
		
		return rv;
	}
	
	public comm(int n) {
		q = new ArrayList< LinkedList<String> >();
		q2 = new ArrayList< LinkedList<String> >();
		charlie = new ArrayList<LinkedList<String>>();
		for(int i=0;i<n;i++) {
			q.add(new LinkedList<String>() );
			q2.add(new LinkedList<String>() );
			charlie.add(new LinkedList<String>() );
		}
	}
}