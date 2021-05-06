import java.math.BigInteger;
import java.util.Random;

public class RSA {

	public BigInteger e;
	public BigInteger d;
	public BigInteger N;

	public RSA() {
		key_generation(25);
	}

	// N = genPrime(n): returns a n-bit prime number N
	public static BigInteger genPrime(int n) {
		// probablePrime returns a prime number
		BigInteger N = BigInteger.probablePrime(n, new Random());
		return N;
	}

	// c = modexp(a, b, N): returns c = a^b (mod N)
	public static BigInteger modexp(BigInteger a, BigInteger b, BigInteger N) {
		BigInteger c;
		c = a.modPow(b, N);
		return c;
	}

	public BigInteger encrypt(int m) {
		return encrypt(BigInteger.valueOf(m));
	}

	public String encrypt(String m) {
		String res = "";
		for (char c : m.toCharArray()) {
			BigInteger enc = encrypt(getValueForChar(c));
			res += enc + " ";
		}
		return res.trim();
	}

	public String decrypt(String m, BigInteger d_d, BigInteger d_N) {
		String[] list = m.split(" ");
		String res = "";
		for (String s : list) {
			BigInteger t = new BigInteger(s);
			t = decrypt(t, d_d, d_N);
			res += getCharForNumber(t);
		}
		return res;
	}

	public BigInteger encrypt(BigInteger m) {
		BigInteger c = m.modPow(d, N);
		// BigInteger c = ModularArithmetic.modexp(m, d, N);
		return c;
	}

	// for an integer c < N, use the private key to return the decrypted message
	// m = c^d(mod N)
	public BigInteger decrypt(BigInteger c, BigInteger private_key, BigInteger decrypt_N) {
		BigInteger m = c.modPow(private_key, decrypt_N);
		return m;
	}

	// Function that generates key
	public void key_generation(int n) {
		BigInteger p, q, bi1, bi2, Phi;

		// gets a random prime p
		p = genPrime(n);

		// if p and q are same , q is generated again
		do {
			// gets a random prime q
			q = genPrime(n);
		} while (p.equals(q));

		// Calculates the value of N = p x q
		N = p.multiply(q);

		bi1 = new BigInteger("1");
		bi2 = new BigInteger("-1");

		// calculates the phi(N)as Phi = (p-1) x (q-1)
		Phi = (p.subtract(bi1).multiply(q.subtract(bi1)));
		//System.out.println("phi(N) is : " + Phi);

		// checks whether gcd(e,phi(N))=1 , 1 < e < phi(N)
		do {
			e = new BigInteger(n, new Random());
			if (e.gcd(Phi).equals(bi1) && e.compareTo(Phi) < 0 && !e.equals(bi1))
				break;
		} while (true);

		// calculates d = e^-1 mod phi(N)
		d = modexp(e, bi2, Phi);
	}

	private BigInteger getValueForChar(char c) {
		return BigInteger.valueOf((int) c);
	}

	@SuppressWarnings("unused")
	private String getCharForNumber(BigInteger i) {
		String r = "";
		// if less than 127, return the corresponding char value from the ASCII table
		if (i.compareTo(BigInteger.valueOf(127)) <= 0) {
			char t = (char) (i.intValue());
			return Character.toString(t);
		}
		// else return the number as such
		return i.toString();
	}
}