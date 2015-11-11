package com.client;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;

import com.chunkserver.ChunkServer;
import com.interfaces.ClientInterface;
import com.network.Network;

/**
 * implementation of interfaces at the client side
 * @author Shahram Ghandeharizadeh
 *
 */
public class Client implements ClientInterface {
	static int ServerPort = 0;
	static Socket ClientSocket;
	static ObjectOutputStream WriteOutput;
	static ObjectInputStream ReadInput;
	
	/**
	 * Initialize the client  FileNotFoundException
	 */
	public Client(){
		if (ClientSocket != null) return; //The client is already connected
		try {
			BufferedReader binput = new BufferedReader(new FileReader(ChunkServer.ClientConfigFile));
			String port = binput.readLine();
			port = port.substring( port.indexOf(':')+1 );
			ServerPort = Integer.parseInt(port);
			
			ClientSocket = new Socket("127.0.0.1", ServerPort);
			WriteOutput = new ObjectOutputStream(ClientSocket.getOutputStream());
			ReadInput = new ObjectInputStream(ClientSocket.getInputStream());
		}catch (FileNotFoundException e) {
			System.out.println("Error (Client), the config file "+ ChunkServer.ClientConfigFile +" containing the port of the ChunkServer is missing.");
		}catch (IOException e) {
			System.out.println("Can't find file.");
		}
	}
	
	/**
	 * Create a chunk at the chunk server from the client side.
	 */
	public String createChunk() {
		try {
			WriteOutput.writeInt(ChunkServer.PayloadSZ + ChunkServer.CMDlength);
			WriteOutput.writeInt(ChunkServer.CreateChunkCMD);
			WriteOutput.flush();
			
			int ChunkHandleSize =  Network.ReadIntFromInputStream("Client", ReadInput);
			ChunkHandleSize -= ChunkServer.PayloadSZ;  //reduce the length by the first four bytes that identify the length
			byte[] CHinBytes = Network.RecvPayload("Client", ReadInput, ChunkHandleSize); 
			return (new String(CHinBytes)).toString();
		} catch (IOException e) {
			System.out.println("Error in Client.createChunk:  Failed to create a chunk.");
			e.printStackTrace();
		} 
		return null;
	}
	
	/**
	 * Write a chunk at the chunk server from the client side.
	 */
	public boolean writeChunk(String ChunkHandle, byte[] payload, int offset) {
		if(offset + payload.length > ChunkServer.ChunkSize){
			System.out.println("The chunk write should be within the range of the file, invalide chunk write!");
			return false;
		}
		try {
			byte[] CHinBytes = ChunkHandle.getBytes();
			
			WriteOutput.writeInt(ChunkServer.PayloadSZ + ChunkServer.CMDlength + (2*4) + payload.length + CHinBytes.length);
			WriteOutput.writeInt(ChunkServer.WriteChunkCMD);
			WriteOutput.writeInt(offset);
			WriteOutput.writeInt(payload.length);
			WriteOutput.write(payload);
			WriteOutput.write(CHinBytes);
			WriteOutput.flush();
			
			int result =  Network.ReadIntFromInputStream("Client", ReadInput);
			if (result == ChunkServer.FALSE) return false;
			return true;
		} catch (IOException e) {
			System.out.println("Error in Client.createChunk:  Failed to create a chunk.");
			e.printStackTrace();
		}
		return false;
	}
	
	/**
	 * Read a chunk at the chunk server from the client side.
	 */
	public byte[] readChunk(String ChunkHandle, int offset, int NumberOfBytes) {
		if(NumberOfBytes + offset > ChunkServer.ChunkSize){
			System.out.println("The chunk read should be within the range of the file, invalide chunk read!");
			return null;
		}
		
		try {
			byte[] CHinBytes = ChunkHandle.getBytes();
			WriteOutput.writeInt(ChunkServer.PayloadSZ + ChunkServer.CMDlength + (2*4) + CHinBytes.length);
			WriteOutput.writeInt(ChunkServer.ReadChunkCMD);
			WriteOutput.writeInt(offset);
			WriteOutput.writeInt(NumberOfBytes);
			WriteOutput.write(CHinBytes);
			WriteOutput.flush();
			
			int ChunkSize =  Network.ReadIntFromInputStream("Client", ReadInput);
			ChunkSize -= ChunkServer.PayloadSZ;  //reduce the length by the first four bytes that identify the length
			byte[] payload = Network.RecvPayload("Client", ReadInput, ChunkSize); 
			return payload;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return null;
	}

	


}
