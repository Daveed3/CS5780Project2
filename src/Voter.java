import java.net.*;
import java.io.*;
import java.math.BigInteger;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeMap;

// Voter will send a message to CLA asking for validation number and CLA will return a random validation number to the user. 
// After recieving a valdidation number, the voter sends their vote and validation number to CTF
public class Voter {
	String username;
	String password;
	BigInteger validationNumber;

	static BigInteger CTF_public_key;
	static BigInteger CTF_N;
	static BigInteger CLA_public_key;
	static BigInteger CLA_N;

	static int port_CLA =  8882;
	static int port_CTF = 8881;

	private Socket CTF_socket;
	private Socket CLA_socket;

	String serverName = "localhost";

	public void getKey(String key, String which_server) {
		String[] keys = key.split(",");
		if (which_server.equals("CLA") ) {
			CLA_public_key = new BigInteger(keys[0]);
			CLA_N = new BigInteger(keys[1]);
		} else {
			CTF_public_key = new BigInteger(keys[0]);
			CTF_N = new BigInteger(keys[1]);
		}

	}
	public void closeCTFSocket() {
		try {
			CTF_socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void closeCLASocket() {
		try {
			CLA_socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	static RSA rsa = new RSA();
	static String public_key = rsa.e + "," + rsa.N;
	public void ValidationNumber() throws ClassNotFoundException{
		BigInteger val = null;
		try {
			System.out.println("Trying to connect to CLA at port " + port_CLA);
			CLA_socket = new Socket(serverName, port_CLA);
			
			String encrypt_username = rsa.encrypt(username);
			ObjectOutputStream out_CLA = new ObjectOutputStream(CLA_socket.getOutputStream());

			out_CLA.writeObject("#," + encrypt_username);
			out_CLA.flush();
			ObjectInputStream CLA_out = new ObjectInputStream(CLA_socket.getInputStream());

			String cla_key = (String) CLA_out.readObject();

			getKey(cla_key, "CLA");

			ObjectOutputStream out_key = new ObjectOutputStream(CLA_socket.getOutputStream());
			out_key.writeObject(public_key);
			out_key.flush();

			ObjectInputStream in = new ObjectInputStream(CLA_socket.getInputStream());
			val = (BigInteger) in.readObject();
			val = rsa.decrypt(val, CLA_public_key, CLA_N);
			
			this.validationNumber=val;
			out_key.close();
			in.close();
			
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		closeCLASocket();
		
	}
	public void Vote_caste(int candidateNumber) {
		BigInteger val = rsa.encrypt(validationNumber);
		BigInteger id = rsa.encrypt(candidateNumber);
		String name = rsa.encrypt(username);
		String vote = "2," + name + "," + val.toString() + "," + id;

		try {
			System.out.println("Trying to connect to CTF at port " + port_CTF);
			CTF_socket = new Socket(serverName, port_CTF);

			ObjectOutputStream out = new ObjectOutputStream(CTF_socket.getOutputStream());
			out.writeObject(vote);
			out.flush();

			ObjectInputStream in = new ObjectInputStream(CTF_socket.getInputStream());
			String res = (String) in.readObject();
			System.out.println(res);
		} catch (IOException | ClassNotFoundException e) {
			e.printStackTrace();
		}

		closeCTFSocket();

	}
	@SuppressWarnings("unchecked")
	public void View_Results() {
		try {
			System.out.println("Trying to connect to CTF at port " + port_CTF);
			CTF_socket = new Socket(serverName, port_CTF);

			System.out.println("Just connected to CTF");

			ObjectOutputStream out = new ObjectOutputStream(CTF_socket.getOutputStream());

			out.writeObject("3");
			out.flush();
			ObjectInputStream in = new ObjectInputStream(CTF_socket.getInputStream());

			HashMap<String, Integer> candidateResult = (HashMap<String, Integer>) in.readObject();

			for (String candidate : candidateResult.keySet()) {
				System.out.println(candidate + " has recieved " + candidateResult.get(candidate)+ "Votes");
			}

		} catch (IOException | ClassNotFoundException e) {
			e.printStackTrace();
		}

		closeCTFSocket();
	}
	
	//To validate the user
	public Boolean validateUser(String username, String password) {

		Boolean res = false;
		try {
			System.out.println("Trying to connect to CTF at port " + port_CTF);
			CTF_socket = new Socket(serverName, port_CTF);

			System.out.println("Just connected to CTF");

			username = rsa.encrypt(username);
			password = rsa.encrypt(password);

			ObjectOutputStream out = new ObjectOutputStream(CTF_socket.getOutputStream());

			out.writeObject("1," + username + "," + password);
			out.flush();
			ObjectInputStream in = new ObjectInputStream(CTF_socket.getInputStream());

			String ctf_key = (String) in.readObject();

			getKey(ctf_key, "CTF");
			
			//Sending the key to CTF_Socket
			ObjectOutputStream out_key = new ObjectOutputStream(CTF_socket.getOutputStream());
			out_key.writeObject(public_key);
			out.flush();
		    getCandidateList(CTF_socket);
			
		    //Check User:
			try {
				ObjectInputStream in_check = new ObjectInputStream(CTF_socket.getInputStream());
				res = (Boolean) in_check.readObject();
			} catch (IOException | ClassNotFoundException ex) {
				System.err.println("Error: " + ex);
			}
		} catch (IOException | ClassNotFoundException e) {
			e.printStackTrace();
		}

		closeCTFSocket();
		return res;

	}
	HashMap<String, Integer> candidateList = new HashMap<String, Integer>();
	@SuppressWarnings("unchecked")
	private void getCandidateList(Socket cTF_socket2) {
		ObjectInputStream in;
		try {
			in = new ObjectInputStream(cTF_socket2.getInputStream());
			candidateList = (HashMap<String, Integer>) in.readObject();
		} catch (IOException | ClassNotFoundException e) {
			e.printStackTrace();
		}
	}
	@SuppressWarnings("rawtypes")
	private void displayCandidateList() {
		System.out.println("Candidates are: \n");
		Map<String, Integer> treeMap = new TreeMap<String, Integer>(candidateList);
		Map sortedMap = sortByValues(treeMap);
		Set set = sortedMap.entrySet();
		 
		// Get an iterator
		Iterator i = set.iterator();
		 
		// Display elements
		while(i.hasNext()) {
			Map.Entry me = (Map.Entry)i.next();
			System.out.println(me.getValue() + ". " + me.getKey() + "\t");
		}	
	}
	
	public static <K, V extends Comparable<V>> Map<K, V> sortByValues(final Map<K, V> map) {
		Comparator<K> valueComparator = new Comparator<K>() {
			public int compare(K k1, K k2) {
				int compare = map.get(k1).compareTo(map.get(k2));
				if (compare == 0) 
					return 1;
				else 
					return compare;
			}
		};
	 
		Map<K, V> sortedByValues = new TreeMap<K, V>(valueComparator);
	    sortedByValues.putAll(map);
	    return sortedByValues;
	  }
	@SuppressWarnings("resource")
	public static void main(String[] args) throws NumberFormatException, IOException, ClassNotFoundException {
		System.out.println( "Hello User!!" );
		System.out.println( "Welcome to Secure Election System!!" );
		Scanner src = new Scanner(System.in);
		
		String option;
		do {
			printMainMenu();
			option = src.next();
			switch (option) {
			case "2":
				return;
			case "1":
				System.out.println("Username: ");
				String username = src.next();
				System.out.println("Password: ");
				String password = src.next();

				Voter voter = new Voter();
				if (!voter.validateUser(username, password)) {
					System.out.println("Invalid username or password");
					continue;
				}
				voter.username = username;
				voter.password = password;
				System.out.println("You have Successfully logged in!!!");
				String choice;
				do {
					printMenu();
					choice = src.next();
					switch (choice) {
					case "1": {
						if (voter.validationNumber == null) {
							voter.ValidationNumber();
						}
						System.out.println("Validation number is : " + voter.validationNumber);
						
						continue;
					}
					case "2": {
						if (voter.validationNumber == null) {
							System.out.println("No validation number");
							break;
						}
						voter.displayCandidateList();
						System.out.println("Enter the candiate number you want to vote");
						int vote = src.nextInt();
						voter.Vote_caste(vote);
						break;
					}
					case "3": {
						voter.View_Results();
						break;
					}
					case "4": {
						System.out.println("Logging out...");
						printMenu();
						break;
					}
					default: {
						System.out.println("Invalid input.. To Logout press 4");
						break;
					}
					}
				} while (!choice.equals("4"));
			}
		} while (!option.equals("2"));
	}

	// Helper method to print main menu
	public static void printMainMenu() {
		System.out.println( "Menu:" );
		System.out.println( "1. Login" );
		System.out.println( "2. Exit" );
	}
	// Helper method to print secondary menu
	public static void printMenu() {
		System.out.println( "Menu:" );
		System.out.println( "1. Obtain Validation Number" );
		System.out.println( "2. Cast Vote" );
		System.out.println( "3. Result" );
		System.out.println( "4. Exit" );
	}
}
