package party;

public class PartyMain {

	private static void CreateParties(int k) {
		System.out.print(Integer.toString(k) + ",");
		Thread[] th = new Thread[k];
		comm Comm = new comm(k);
		for(int i=0;i<k;i++) {
			Party p = new Party(i, k);
			p.setComm(Comm);
			
			th[i] = new Thread(p);
			th[i].start();
			//(new Thread(p)).start();
		}
		
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
		
		for(int i=10;i<=780;i+=10) {
			CreateParties(i);
		}
	}
}