import java.io.*;
import java.net.*;
import java.util.*;
 
public class Client {
    public static void main(String[] args) throws IOException {
        boolean isPlaying = false;
        String incorrect;


        if(args.length != 2){ 
            System.out.println("Insufficient arugments");
            System.exit(1);
        }
        String ipAddr = args[0];
        int portNo =  Integer.parseInt(args[1]);

        //Try to connect to a socket, given that the server is running on that port.
        //If successful, client will be prompted to begin a game.
        //Any inputs after that will be used to play the game.
        try (
            Socket echoSocket = new Socket(ipAddr, portNo);
            PrintWriter out = new PrintWriter(echoSocket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(echoSocket.getInputStream()));
            BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));

        ) {
            System.out.println("Connected to server");
            System.out.print("Ready to start game? (y/n): ");
            while(!isPlaying){
                String answer = stdIn.readLine();
                if (answer.equals("y")) {
                    isPlaying = true;
                    char accept = ' ';
                    out.println(accept);
                } else if (answer.equals("n")){
                    System.exit(1);
                } else {
                    System.out.print("Please use a valid input (y/n): ");
                }                
            }

            HashSet<String> set = new HashSet<String>();

            String temp;
            temp = in.readLine();
            parseArray(temp);
            String userInput;
            System.out.print("Letter to guess: ");
            while ((userInput = stdIn.readLine()) != null && isPlaying) {
                if(set.contains(userInput)) {
                    System.out.println("Error! " + userInput + " has been guessed before. Please guess another letter");
                    System.out.print("Letter to guess: ");
                } else if (userInput.length() == 1 && Character.isLetter(userInput.charAt(0))) {
                    set.add(userInput);
                    char[] send = new char[2];
                    send[0] = 1;
                    send[1] = userInput.toLowerCase().charAt(0);
                    out.println(send);

                    temp = in.readLine();
                    int more = parseArray(temp);
                    if(more == 1) {
                        temp = in.readLine();
                        more = parseArray(temp);
                    }
                    System.out.print("Letter to guess: ");
                } else {
                    System.out.println("Error! Please guess one letter");
                    System.out.print("Letter to guess: ");
                }
            }
        } catch (UnknownHostException e) {
            System.err.println("Don't know about host local");
            System.exit(1);
        } catch (IOException e) {
            System.err.println("Couldn't get I/O for the connection to local");
            System.exit(1);
        } 
    }

    //Method used to handle message flags to know what the server is trying to communicate.
    private static int parseArray(String s) {
        char[] c = s.toCharArray();
        int messageHeader = c[0];
        if(messageHeader == 8) {
            System.out.println("You lose :(");
            return 1;
        } else if(messageHeader == 17) {
            System.out.println("System Overloaded");
            System.exit(1);
        } else if(messageHeader == 7) {
            System.out.println("You win!");
            return 1;
        } else if(messageHeader == 9) {
            System.out.println("Game Over!");
            System.exit(1);
        } else if(messageHeader == 0) {
            int blank = c[1];
            int incorrect = c[2];
            for(int i = 0; i < blank; i++) {
                System.out.print(c[i+3] + " ");
            }
            System.out.println();
            System.out.print("Incorrect Guesses: ");
            for(int j = 0; j < incorrect; j++) { 
                System.out.print(c[j + 3 + blank] + " ");
            }
            System.out.println();
        }
        return 0;
    }
}