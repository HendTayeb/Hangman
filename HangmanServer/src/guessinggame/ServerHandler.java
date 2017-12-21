package guessinggame;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import static java.lang.String.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Random;

public class ServerHandler implements Runnable {

    private static final int DELAY_SECS = 0;
    private Socket clientSocket;
    private BufferedInputStream in;
    private BufferedOutputStream out;
    private String answer = "";
    private final File sourceFile = new File("src/guessinggame/words.txt");
    private final int min = 11;
    private final int max = 25143;
    private int failedAttemptCounter;
    private int scoreCounter = 0;//全局变量，整个过程中不再初始化
    private char[] template;
    private StringBuilder alphabet = new StringBuilder("abcdefghijklmnopqrstuvwxyz");

    ServerHandler(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }

    public void run() {
        int secsToMillis = 1000;
        String msg = null;

        //create connection to client
        connect();
        /////////////////////////////////////////////////////////////
        //server keeps listening to client
        while (true) {
            try {
                
                msg = receiveMsgFromClient();
                System.out.println(msg);
                Thread.sleep(DELAY_SECS * secsToMillis);
                //start game
                if (msg.equals("*start")) {
                    //initialize
                    initialization();
                    ////////////////////////////
                    System.out.println(answer);
                } //close game
                else if (msg.equals("*close")) {
                    in.close();
                    out.close();
                    break;
                }else if (msg.equals("*hint")) {
                    String reply = createHint();
                    sendReply(reply);
                } //msg is guess for the answer
                else {
                    String reply = evaluateGuess(msg);
                    sendReply(reply);
                }
                
            } catch (IOException | InterruptedException e) {
                System.out.println(e.toString());
                break;
            }
        }
        //////////////////////////////////////////////////////////////
    }

    //initialize in and out
    void connect() {
        try {
            in = new BufferedInputStream(clientSocket.getInputStream());
            out = new BufferedOutputStream(clientSocket.getOutputStream());
        } catch (IOException e) {
            System.out.println(e.toString());
        }
    }

    //initialize answer, template and failed attempt counter, and send template and failedAttemptCounter to client
    void initialization() throws IOException {
        String result = null;

        //initialize answer
        createAnswer();
        //initialize template(the current view)
        template = new char[answer.length()];
        for (int i = 0; i < answer.length(); i++) {
            template[i] = '-';
        }
        failedAttemptCounter = 10;

        result = "uncompleted" + "," + String.valueOf(template) + "," + String.valueOf(failedAttemptCounter) + "," + answer;
        sendReply(result);
    }

    void createAnswer() throws IOException {
        Random random = new Random();
        int lineNumber = random.nextInt(max) % (max - min + 1) + min;
        FileReader fr = new FileReader(sourceFile);
        LineNumberReader lnr = new LineNumberReader(fr);
        answer = lnr.readLine();
        while (answer != null) {
            if (lineNumber == lnr.getLineNumber()) {
                break;
            }
            answer = lnr.readLine();
        }
        lnr.close();
        answer = answer.toLowerCase();
    }

    String receiveMsgFromClient() throws IOException {
        String str = null;

        byte[] bt = new byte[256];
        int bytesRead = 0;
        int n;
        while ((n = in.read(bt, bytesRead, 64)) != -1) {
            bytesRead += n;
            if (bytesRead == 256) {
                break;
            }
            if (in.available() == 0) {
                break;
            }
        }
        str = new String(bt);
        //bytesRead is the real length of the bt[], the elements since bt[bytesRead] afterwards are empty.
        str = str.substring(0, bytesRead);
        return str;
    }

    String createHint() throws IOException {
        char[] answerLetters = answer.toCharArray();
        String result = null;
        outerloop:
        for (int i = 0; i < alphabet.length(); i++) {
            for(int j = 0; j < answer.length(); j++){
                if (alphabet.charAt(i) == answerLetters[j]) {
                template[j] = alphabet.charAt(i);
                alphabet.deleteCharAt(i);
                break outerloop;
                }
            }
        }
        result = String.valueOf(template);
        if (result.equals(answer)) {
            scoreCounter += 1;
            result = "win" + "," + answer + "," + String.valueOf(scoreCounter);
        } else {
            result = "uncompleted" + "," + String.valueOf(template) + "," + String.valueOf(failedAttemptCounter);
        }    
        
        return result;
    }
    
    String evaluateGuess(String guess) throws IOException {
        String result = null;

        //guess the whole word
        if (guess.length() > 1) {
            if (guess.equals(answer)) {
                scoreCounter += 1;
                result = "win" + "," + answer + "," + String.valueOf(scoreCounter);
            } else {
                failedAttemptCounter -= 1;
                if (failedAttemptCounter == 0) {
                    scoreCounter -= 1;
                    result = "loose" + "," + String.valueOf(scoreCounter);
                } else {
                    result = "uncompleted" + "," + String.valueOf(template) + "," + String.valueOf(failedAttemptCounter);
                }
            }
        } //guess a letter
        else {
            //guess letter is included in answer
            if (answer.indexOf(guess) != -1) {
                char guessLetter = guess.charAt(0);
                if(alphabet.indexOf(String.valueOf(guessLetter)) != -1)
                    alphabet.deleteCharAt(alphabet.indexOf(String.valueOf(guessLetter)));
                char[] answerLetters = answer.toCharArray();
                for (int i = 0; i < answer.length(); i++) {
                    if (guessLetter == answerLetters[i]) {
                        template[i] = guessLetter;
                    }
                }
                result = String.valueOf(template);

                if (result.equals(answer)) {
                    scoreCounter += 1;
                    result = "win" + "," + answer + "," + String.valueOf(scoreCounter);
                } else {
                    result = "uncompleted" + "," + String.valueOf(template) + "," + String.valueOf(failedAttemptCounter);
                }
            } //guess letter is not included in answer
            else {
                failedAttemptCounter -= 1;
                if (failedAttemptCounter == 0) {
                    scoreCounter -= 1;
                    result = "loose" + "," + String.valueOf(scoreCounter);
                } else {
                    result = "uncompleted" + "," + String.valueOf(template) + "," + String.valueOf(failedAttemptCounter);
                }
            }
        }
        return result;
    }

    void sendReply(String result) {
        try {
            byte[] toClient = result.getBytes();
            out.write(toClient, 0, toClient.length);
            out.flush();
        } catch (IOException e) {
            System.out.println(e.toString());
        }
    }

}
