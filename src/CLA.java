// Central Legitimization Agency
// Could certify whether the voters can actually vote
// Provides voter with random validation number; and maintains list of verified voters
// Sends verified numbers to CTF
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Random;

public class CLA {
	String serverName = "localhost";
	static int port_ctf = 8881;
	static int port_voter= 8880;
	RSA rsa = new RSA();
	String key_pair = rsa.e+","+rsa.N;
	private ServerSocket voter_Serversocket;
	static HashMap<String , BigInteger> Validation_nos= new HashMap<String, BigInteger>();
	
	static BigInteger CTF_public_key;
	static BigInteger CTF_N;
	static BigInteger Voter_public_key;
	static BigInteger Voter_N;
		
	public void getKey(String key, int number) {
		String[] keys = key.split(",");

		// if number is 1 - CLA
		// if number is 2 - Voter
		if (number == 1) {
			System.out.println("getting key from CTF");
			CTF_public_key = new BigInteger(keys[0]);
			CTF_N = new BigInteger(keys[1]);
		} else {
			System.out.println("getting key from voter");
			Voter_public_key = new BigInteger(keys[0]);
			Voter_N = new BigInteger(keys[1]);
		}

	}
		
	public void sendKey(Socket s) {
		System.out.println("sending key ");
		try {
			ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
			out.writeObject(key_pair);
			out.flush();

		} catch (IOException ex) {
			System.err.println("Error: " + ex);
		}
	}
		
	public void startServers() {
		// server for voter
		new Thread() {
			public void run() {
				try {
					Boolean stop = false;
					voter_Serversocket = new ServerSocket(port_voter);
					Socket voter = null;
					while (!stop) {
						System.out.println("Waiting for Voter at port no:" + port_voter);
						voter = voter_Serversocket.accept();
						System.out.println("Voter is connected ...");
						String choice;
						
						ObjectInputStream input = new ObjectInputStream(voter.getInputStream());

						choice = (String) input.readObject();

						switch (choice.substring(0, 1)) {
						case "#":
							System.out.println("Voter wants to get a Validation No");
							try {
								sendKey(voter);
								ObjectInputStream in = new ObjectInputStream(voter.getInputStream());
								String key = (String) in.readObject();
								getKey(key, 2);

							} catch (Exception e) {
								e.printStackTrace();
							}

							String[] str = choice.split(",");

							String username = rsa.decrypt(str[1], Voter_public_key, Voter_N);

							ObjectOutputStream out_voter = new ObjectOutputStream(voter.getOutputStream());

							BigInteger val = null;

							if (Validation_nos.containsKey(username)) {
								val = (Validation_nos.get(username));
								System.out.println("Validation Number for this already exist ");
							} else {
								val =  new BigInteger(25, new Random());
								Validation_nos.put(username, val);
								System.out.println("Your validation number has generated: " + val);
								String User_validationNo= "\n" + username + "," + val.toString();
								
								WriteFile(User_validationNo);
								startSender();
							}

							val = rsa.encrypt(val);
							out_voter.writeObject(val);
							out_voter.flush();
					}
				} 
				}catch (Exception e) {
					e.printStackTrace();
				}
			}
		}.start();
	}
	
	public static void WriteFile( String content) {
		String filename =   "ValidatioNos.txt";
		try {
			File file = new File(filename);
			// create new if file doesnt exist
			FileWriter fw = new FileWriter(file, true);
			BufferedWriter bw = new BufferedWriter(fw);
			bw.write(content);
			bw.newLine();
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void startSender() {
		// connect to CTF server
		try {
			System.out.println("Connecting to Central Tabulating Facility (CTF) program on port " + port_ctf);
			Socket ctf = new Socket(serverName, port_ctf);

			ObjectInputStream in = new ObjectInputStream(ctf.getInputStream());
			String message = (String) in.readObject();
			getKey(message, 1);

			sendKey(ctf);
				try {
					ObjectOutputStream out_ctf = new ObjectOutputStream(ctf.getOutputStream());
					out_ctf.writeObject(Validation_nos);
				} catch (IOException e) {
					e.printStackTrace();
				}
			ctf.close();
		} catch (IOException | ClassNotFoundException e) {
			e.printStackTrace();
		}

	}
	public static String ReadFile() {
		String filename =  "../ValidatioNos.txt";
		// This will reference one line at a time
		String line = null;
		StringBuilder sb = new StringBuilder();
		try {
			// FileReader reads text files in the default encoding.
			FileReader fileReader = new FileReader(filename);

			// Always wrap FileReader in BufferedReader.
			BufferedReader br = new BufferedReader(fileReader);

			while ((line = br.readLine()) != null) {
				String[] Valid_nos_from_file = line.split(",");
				if (Valid_nos_from_file.length == 2) {
					Validation_nos.put(Valid_nos_from_file[0], new BigInteger(Valid_nos_from_file[1]));
				}
			}
			// Always close files.
			br.close();
		} catch (FileNotFoundException ex) {
			System.out.println("Unable to open file '" + filename + "'");
		} catch (IOException ex) {
			System.out.println("Error reading file '" + filename + "'");
		}
		String contents = sb.toString();
		return contents;

	}
	@SuppressWarnings("static-access")
	public static void main(String[] args) {
		CLA cla = new CLA();
		cla.ReadFile();
		cla.startServers();
		cla.startSender();
	}

}
