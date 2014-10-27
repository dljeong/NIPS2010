package party;

public class HomomorphicEncrypt {

	static int encrypt(int e,int n,int m)
	{
		//return (int) (Math.pow(m, e) % n);
		return m;
	}
	static int decrypt(int d, int n, int c)
	{
		return c;
		//return (int)(Math.pow(c,d) %n);
	}
}
