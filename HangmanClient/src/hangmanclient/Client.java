/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package hangmanclient;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Mark
 */
public class Client
{
    
    private DataInputStream in;  
    private DataOutputStream out;
    private Socket clientSocket;
    
    
    public void ConnectServer(String ip, int port) //connect to server
    {
        try
        {
            clientSocket = new Socket(ip, port);
            out = new DataOutputStream(clientSocket.getOutputStream());
            in = new DataInputStream(clientSocket.getInputStream());
            System.out.println("connect successfully!!");
        }
        catch (IOException ex)
        {
            ex.printStackTrace();
        }
    }
    
    
    
    public String StartNewGame()
    {
        String startReply = "";
        try
        {
            String startComm = "*start";
            byte[] toServer = startComm.getBytes();
            out.write(toServer, 0, toServer.length);
            
            //out.writeUTF("*start");
            byte[] fromServer = new byte[256];
            int n;
            n = in.read(fromServer, 0, 256);
            startReply = new String(fromServer);
            startReply = startReply.substring(0, n);
        } 
        catch (IOException ex)
        {
            ex.printStackTrace();
        }
        return startReply;
    }
    
    public String SendGuess(String word)
    {
        String guessReply = "";
        try
        {

            byte[] toServer = word.getBytes();
            out.write(toServer, 0, toServer.length);

            byte[] fromServer = new byte[256];
            int n;
            n = in.read(fromServer, 0, 256);
            guessReply = new String(fromServer);
            guessReply = guessReply.substring(0, n);
        } 
        catch (IOException ex)
        {
            ex.printStackTrace();
        }
        return guessReply;
    }
    
    public String getHint()
    {
        String hintReply = "";
        try
        {
            String hintComm = "*hint";
            byte[] toServer = hintComm.getBytes();
            out.write(toServer, 0, toServer.length);
            
            //out.writeUTF("*start");
            byte[] fromServer = new byte[256];
            int n;
            n = in.read(fromServer, 0, 256);
            hintReply = new String(fromServer);
            hintReply = hintReply.substring(0, n);
        } 
        catch (IOException ex)
        {
            ex.printStackTrace();
        }
        return hintReply;
    }
    
    public void Disconnect()
    {
        
        try
        {
            String str = "*close";
            byte[] toServer = str.getBytes();
            out.write(toServer, 0, toServer.length);
            
            out.close();
            in.close();
            clientSocket.close();
            
            
        } 
        catch (IOException ex)
        {
            ex.printStackTrace();
        }
        
    }
   
}
