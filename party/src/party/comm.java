package party;

import java.util.ArrayList;
import java.util.LinkedList;

public class comm {
	
	boolean flag = false;
	ArrayList< LinkedList<String> > q;
	
	public synchronized void send(String msg, int i) {
		LinkedList<String> uQ = q.get(i);
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
	
	public comm(int n) {
		q = new ArrayList< LinkedList<String> >();
		for(int i=0;i<n;i++) {
			q.add(new LinkedList<String>() );
		}
	}
}