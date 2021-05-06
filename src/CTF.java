import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;

//Central Tabulating Facility
//Should count the votes
//Checks voters' validation by checking voter's validation number vs the list provided by the CLA
//CTF publishes the results of the election
public class CTF {
	static int port_voter = 8880;
	static int port_ctf = 8881;
	
	HashSet<Candidate> candidatesList = new HashSet<Candidate>();
	
	private ServerSocket Voter_ServerSocket;
	private Socket voter_socket;
	private ServerSocket CLA_ServerSocket;
	
	static BigInteger voter_public_key;
	static BigInteger voter_N;
	static BigInteger CLA_public_key;
	static BigInteger CLA_N;

	public void getKey(Socket client, String which_server) {
		try {
			ObjectInputStream in = new ObjectInputStream(client.getInputStream());
			String key = (String) in.readObject();

			String[] keys = key.split(",");
			if (which_server.equals("CLA")) {
				System.out.println("getting the keys from cla");
				CLA_public_key = new BigInteger(keys[0]);
				CLA_N = new BigInteger(keys[1]);
				String cla_key_pair = "CLA Public Key"+"{"+CLA_public_key+","+CLA_N+"}";
				writeFile("Raw__Data.txt",cla_key_pair);
			} else {
				System.out.println("getting key from voter");
				voter_public_key = new BigInteger(keys[0]);
				voter_N = new BigInteger(keys[1]);
				String Voter_key_pair = "Voter Public Key"+"{"+voter_public_key+","+voter_N+"}";
				writeFile("Raw__Data.txt",Voter_key_pair);
			}
		} catch (IOException | ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

	HashMap<String, String> ValidationNumber = new HashMap<String, String>();
	HashMap<String, Integer> Getcandidate = new HashMap<String, Integer>();
	HashMap<String, Boolean> hasVoted = new HashMap<String, Boolean>();
	HashMap<String, String> Login = new HashMap<String, String>();
	HashMap<String, Boolean> Login_vote = new HashMap<String, Boolean>();
	
	private void vote_caste(String choice, Socket voter) {
		String[] list = choice.split(",");
		writeFile("Raw__Data.txt",choice);
		BigInteger validationNo = new BigInteger(list[2]);
		String username = list[1];
		username = rsa.decrypt(username, voter_public_key, voter_N);
		validationNo = rsa.decrypt(validationNo, voter_public_key, voter_N);

		BigInteger Cand_id = new BigInteger(list[3]);
		Cand_id = rsa.decrypt(Cand_id, voter_public_key, voter_N);

		String res = "";
		try {
			ObjectOutputStream out = new ObjectOutputStream(voter.getOutputStream());
			if (hasVoted.containsKey(username)) {
				res = "You have already voted !!";
			} else if (ValidationNumber.containsKey(validationNo.toString())) {
				res = "Invalid Validation Number";
			} else {
				Boolean flag = true;
				for (Candidate Cand : candidatesList) {
					if (Cand.id == Cand_id.intValue()) {
						System.out.println("updating vote for " + Cand.candidate_name);
						Cand.increaseVote();
						flag = false;

						res = "You have Successfully Voted for the :" + Cand.candidate_name;
						hasVoted.put(username, true);
						Write_username_password();
						update_candidate_list();
						break;
					}
				}
				if (flag) {
					res = "This choice of Candidate does not exist ";
				}
			}

			out.writeObject(res);
			out.flush();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void getCandidateList() {
		BufferedReader br = null;
		String path = "../candidateList.txt";
		try {
			FileInputStream f = new FileInputStream(path);
			String line;

			br = new BufferedReader(new InputStreamReader(f));

			while ((line = br.readLine()) != null) {
				String[] line_array = line.split(",");
				Candidate cand = new Candidate(line_array[1],line_array[0],line_array[2]);
				candidatesList.add(cand);
				Getcandidate.put(cand.getCandidate_name(), cand.getId());
			}
		} catch (IOException ex) {
			ex.printStackTrace();
		} finally {
			try {
				if (br != null)
					br.close();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
	}
	RSA rsa = new RSA();
	String rsa_key = rsa.e + "," + rsa.N;
	Boolean stop = false;

	// Start servers for the CLA and Voter
	public void startServer() {
		// server for CLA
		(new Thread() {
			@SuppressWarnings("unchecked")
			public void run() {
				try {
					CLA_ServerSocket = new ServerSocket(port_ctf);
					System.out.println("Waiting for Central Legitimization Agency (CLA) at port no: " +port_ctf);
					Socket cla = null;
					while (true && !stop) {
					cla = CLA_ServerSocket.accept();
					System.out.println("CLA is connected ....");

					try {
						ObjectOutputStream out = new ObjectOutputStream(cla.getOutputStream());
						out.writeObject(rsa_key);
						out.flush();
						
					} catch (IOException ex) {
						System.err.println("Error: " + ex);

					}
					getKey(cla, "CLA");

					ObjectInputStream input = new ObjectInputStream(cla.getInputStream());
					ValidationNumber = (HashMap<String, String>) input.readObject();
					System.out.println("Got available validation numbers from CLA");

					cla.close();
					}
				} catch (Exception e) {
					e.printStackTrace();
				}

				CLASocket_close();
			}
		}).start();

		// server for Voter
		(new Thread() {
			@Override
			public void run() {
				try {
					Voter_ServerSocket = new ServerSocket(port_voter);
					System.out.println("Waiting for Voter at port no: " +port_voter );
					while (true && !stop) {
						Socket voter = Voter_ServerSocket.accept();
						System.out.println("Voter is connected ...");
						String choice;

						ObjectInputStream input = new ObjectInputStream(voter.getInputStream());

						choice = (String) input.readObject();

						switch (choice.substring(0, 1)) {
						case "1":
							System.out.println("Voter wants to validate himself");
							try {
								ObjectOutputStream out = new ObjectOutputStream(voter.getOutputStream());
								out.writeObject(rsa_key);
								out.flush();

							} catch (IOException ex) {
								System.err.println("Error: " + ex);

							}
							getKey(voter, "VOT");
							System.out.println("sending the candidates");
							try {
								ObjectOutputStream out_list = new ObjectOutputStream(voter.getOutputStream());
								System.out.println(Getcandidate);
								out_list.writeObject(Getcandidate);
							} catch (IOException e) {
								e.printStackTrace();
							}

							String[] list = choice.split(",");

							String username = rsa.decrypt(list[1], voter_public_key, voter_N);
							String password = rsa.decrypt(list[2], voter_public_key, voter_N);

							Boolean res = false;

							try {
								ObjectOutputStream out = new ObjectOutputStream(voter.getOutputStream());
								for (Entry<String, String> log : Login.entrySet()) {
									if (log.getKey().equals(username) && log.getValue().equals(password)) {
										res = true;
										break;
									}
								}

								out.writeObject(res);
								out.flush();

							} catch (IOException ex) {
								ex.printStackTrace();
							}
							break;
							case "2":
								System.out.println("Voter wants  to vote");
								vote_caste(choice, voter);
								break;
								
							case "3":
								System.out.println("Voter wants  to see the result:");
								try {
									ObjectOutputStream out = new ObjectOutputStream(voter.getOutputStream());

									HashMap<String, Integer> result_final = new HashMap<String, Integer>();
									for (Candidate cand : candidatesList) {
										result_final.put(cand.candidate_name, cand.vote_count);
									}
									out.writeObject(result_final);
									out.flush();
								} catch (IOException e) {
									e.printStackTrace();
								}
								break;
						}
					}
					voter_socket.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}).start();
	}
	
	public void Write_username_password() {
		try {
			File file = new File("CTF_Login.txt");

			// create new if file doesnt exist
			if (!file.exists()) {
				file.createNewFile();
			}

			// delete the file if it already exists
			else {
				file.delete();
			}

			FileWriter fw = new FileWriter(file.getAbsoluteFile());
			BufferedWriter bw = new BufferedWriter(fw);
			for (String user : Login.keySet()) {
				String line = user + "," + Login.get(user) + "," + hasVoted.get(user);
				bw.write(line);
				bw.newLine();
				bw.flush();
			}
			bw.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void ReadUsernamePassword() {
		BufferedReader br = null;
		String path = "../CTF_Login.txt";
		System.out.println("Testing");
		System.out.println(System.getProperty("user.dir"));
		try {
			FileInputStream f = new FileInputStream(path);
			String sCurrentLine;

			br = new BufferedReader(new InputStreamReader(f));

			while ((sCurrentLine = br.readLine()) != null) {
				String[] line = sCurrentLine.split(",");
				Login.put(line[0], line[1]);
				Login_vote.put(line[0], new Boolean(line[2]));
			}

		} catch (IOException ex) {
			ex.printStackTrace();
		} finally {
			try {
				if (br != null)
					br.close();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}

	}
	public void update_candidate_list() {
		try {
			File file = new File("candidateList.txt");

			// create new if file doesnt exist
			if (!file.exists()) {
				file.createNewFile();
			}

			// delete the file if it already exists
			else {
				file.delete();
			}

			FileWriter fw = new FileWriter(file.getAbsoluteFile());
			BufferedWriter bw = new BufferedWriter(fw);
			for (Candidate cand : candidatesList) {
				String line = cand.candidate_name + "," + cand.id + "," + cand.vote_count;
				bw.write(line);
				bw.newLine();
				bw.flush();
			}
			bw.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void CLASocket_close() {
		try {
			CLA_ServerSocket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		CTF ctf = new CTF();
		ctf.ReadUsernamePassword();
		ctf.getCandidateList();
		ctf.startServer();
	}
	
	// Create file for private and public keys
	public static void writeFile(String filename, String content) {
		try {
			File file = new File(filename);

			// create new if file doesnt exist
			if (!file.exists()) {
				file.createNewFile();
			}
			FileWriter fw = new FileWriter(file,true);
			BufferedWriter bw = new BufferedWriter(fw);
			bw.write(content);
			bw.newLine();
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
