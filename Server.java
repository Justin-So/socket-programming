import java.net.*;
import java.io.*;
import java.util.*;

public class Server {
	static int clientNumber = 0;

	public static void main(String[] args) throws Exception {
		System.out.println("Server is running");
		

		String fileName = "words.txt";
		String line = null;
		String gameWord = null;
		String[] dictionary = null;
		int portNo = 0;

		//Take in command line arguments
		if(args.length == 1) {
			portNo = Integer.parseInt(args[0]);
		} else if(args.length == 2) {
			portNo = Integer.parseInt(args[0]);
			fileName = args[1];
		} else {
			System.out.println("Insufficient arguments");
			System.exit(1);
		}


		//Create new Server Socket
		ServerSocket listener = new ServerSocket(portNo);

		//Iterate through the text document to pick a random word for the client to guess.
		try{
			FileReader fileReader = new FileReader(fileName);

			BufferedReader bufferedReader = new BufferedReader(fileReader);

			line = bufferedReader.readLine();
			String[] firstLine = line.split(" ");
			
			int wordLength = Integer.parseInt(firstLine[0]);
			int wordCount = Integer.parseInt(firstLine[1]);

			dictionary = new String[wordCount];
			int i = 0;

			while((line = bufferedReader.readLine()) != null) {
				dictionary[i] = line;
				i++;
			}

			int random = (int)(Math.random() * wordCount);
			gameWord = dictionary[random];

			bufferedReader.close();

		}

		//Exception handling
		catch(FileNotFoundException ex) {
			System.out.println(fileName + " could not be found");
		}
		catch(IOException ex) {
			System.out.println("Error reading file");
		}


		//Begin the game process
		try {
			while (true) {
				Game game = new Game(listener.accept(), clientNumber);
				clientNumber++;
				game.setDictionary(dictionary);
				game.start();
			}
		} finally {
			listener.close();
		}
	}

	private static class Game extends Thread {
		private Socket socket;
		private String[] dictionary;
		private boolean isPlaying = false;
		private boolean wonGame = false;
		private int cn;

		private String incorrectGuesses = "";

		//Sets the dictionary for a single Game instance.
		public void setDictionary(String[] dictionary) {
			this.dictionary = dictionary;
		}

		
		public Game(Socket socket, int clientNumber) {
			this.socket = socket;
			this.cn = clientNumber;
			System.out.println("Current "+ (Server.clientNumber+1) +" client(s) connected");
		}



		public void run() {
			try {
				//Set-up for communicating with the Client-side code
				BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

				String word = chooseWord(dictionary);
				int wordLength = word.length();
				int penalty = 0;
				String[] badGuesses = new String[6];

				//"blank" used as a empty string to track the user's progress
				System.out.println(word);
				String blank = "";

				//HashSet used to determine whether a letter has already been guessed or not.
				HashSet<String> set = new HashSet<String>();

				for (int i = 0; i < word.length(); i++) {
					blank = blank + "_";
				}

				//If the initial message from the client is " ", then the server will check if there is enough space on to handle a new game (Limit 3 games).
				String input = in.readLine();
				if (input.equals(" ") && !isPlaying) {
					if(Server.clientNumber > 3) {
						out.println(sendMessage("Server Overloaded"));
					} else {
						isPlaying = true;
						char[] c = createPacket(blank, badGuesses, penalty);
						String str = String.valueOf(c);
						System.out.println(str);
						out.println(c);
					}
				}

				// Majority of the gameplay logic is in this while loop.
				// Handles correct guesses, incorrect guessees, repeated guesses, and records the number of 
				// incorrect guesses. If the number of incorrect guesses exceeds 6, then the server will notify 
				// the client that they have lost the game.
				while (isPlaying) {
                    String currentClientInput = in.readLine();
                    String clientInput = Character.toString(currentClientInput.charAt(1));

                    if (word.indexOf(clientInput.charAt(0)) >= 0 ){ 
                    	blank = check(blank, word, clientInput.charAt(0));
                    	if(set.contains(clientInput)) {
                    		System.out.println("already guessed");
                    		char[] c = createPacket(blank, badGuesses, penalty);
							String str = String.valueOf(c);
							System.out.println(str);
                    		out.println(c);
                    	} else if(wonGame == true) {
                    		set.add(clientInput);
                    		out.println(sendMessage("You Win"));
                    		out.println(sendMessage("Game Over"));
                    	} else {
                    		set.add(clientInput); 
                    		char[] c = createPacket(blank, badGuesses, penalty);
							String str = String.valueOf(c);
							System.out.println(str);
                    		out.println(c);
                    	}
                    } else {
                    	if (penalty < 6) {
                    		if(set.contains(clientInput)) {
                    			char[] c = createPacket(blank, badGuesses, penalty);
								String str = String.valueOf(c);
								System.out.println(str);
                    			out.println(c);
                    		} else {
                    			set.add(clientInput);
                    			badGuesses[penalty] = clientInput;
                    			incorrectGuesses = incorrectGuesses + clientInput + " ";
                    			penalty++;
                    			if(penalty < 6) {
									char[] c = createPacket(blank, badGuesses, penalty);
									String str = String.valueOf(c);
									System.out.println(str);
                    				out.println(c);
                    			}
                    		}
                    	}
                    	
                    }


                    if(penalty >= 6) {      
                    	out.println(sendMessage("You Lose"));
                    	out.println(sendMessage("Game Over"));
                    } 
                    
                }
			} catch (IOException e) {
				System.out.println(e);
			} finally {
				try {
					socket.close();
				} catch (IOException e) {
					System.out.println(e);
				}
				System.out.println("Client number " + cn + "'s connection has closed");
				Server.clientNumber--;
			}
		}

		//Takes the string and compresses it down to a character array to minimize the amount of bytes needed to send the message.
		private char[] sendMessage(String s) {
			int messageLength = s.length();
			char[] array = new char[messageLength + 1];
			array[0] = (char) (messageLength);
			for(int i = 0; i < messageLength; i++) {
				array[i + 1] = s.charAt(i);
			}			
			return array;
		}

		//Takes all of the information and reduces it down to a Character Array to be sent to the client, using the least amount of bytes possible.
		private char[] createPacket(String blank, String[] badGuesses, int wrong) {
			int wordLength = blank.length();
			int numIncorrect = wrong;
			char[] array = new char[(wordLength + numIncorrect + 3)];
			array[0] = (char) (0);
			array[1] = (char) (wordLength);
			array[2] = (char) (numIncorrect);
			for(int i = 0; i < wordLength; i++) {
				array[i + 3] = blank.charAt(i);
			}
			for(int j = 0; j < wrong; j++) {
				array[j + 3 + wordLength] = badGuesses[j].charAt(0);
			}
			return array;
		}

		//Randomly selects a word out of the String[] from a text document.
		private String chooseWord(String[] dictionary) {
			int random = (int)(Math.random() * dictionary.length);
			String word = dictionary[random];
			word.toLowerCase();
			return word;
		}

		//Method used to check whether a guess is correct and can handle if the client has won the game or not.
		private String check(String blank, String word, char guess) {
			char[] blankArray = blank.toCharArray();
			char[] wordArray = word.toCharArray();
			for(int i = 0; i < wordArray.length; i++) {
				if(word.charAt(i) == guess){
					blankArray[i] = guess;				
				}
			}

			if(contains(blankArray) == false) {
				wonGame = true;
			}

			blank = String.valueOf(blankArray);
			return blank;
		}

		//Checks if the Client's input is already guessed.
		private boolean contains(char[] input) {
			for (char x : input) {
				if(x == '_') {
					return true;
				}
			}
			return false;
		}
	}
}