/**
 * 
 */
package cc.biomorphicvis.data;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.DatagramChannel;

import de.sciss.net.OSCListener;
import de.sciss.net.OSCMessage;
import de.sciss.net.OSCPacket;
import de.sciss.net.OSCReceiver;
import de.sciss.net.OSCTransmitter;

/**
 * @author carlos
 *	//  ---------------- OSC/stuff -----------------   //
 *		this class is for sending OSC messages to the
 *		Max/MSP sound patch
 *		It can also receive OSC messages
 */
public class OSCHandler extends Thread
{
 
	private OSCTransmitter oscTransmitter = null; // for sending messages
    private OSCReceiver oscReceiver = null; // for receiving messages
    private InetSocketAddress sendAddr;
    private InetSocketAddress rcvAddr;
    private DatagramChannel dchSend = null;
    private DatagramChannel dchRcv = null;
    private static final int DEFAULT_SEND_PORT = 50000;
    private static final int DEFAULT_RECEIVE_PORT = 50001;
    private String host;
    private int sendport;
    private int recvport;
    private boolean isConnected;
    
    public OSCHandler(String host)
    {
    	this(host, DEFAULT_SEND_PORT, DEFAULT_RECEIVE_PORT);
    }
    
    public OSCHandler(String host, int sendport, int recvport)
    {
    	this.host = host;
    	this.sendport = sendport;
    	this.recvport = recvport;
    	this.isConnected = false;
    }
    
	public void run()
	{
		System.out.println("*** OSCHandler started... ***");
		// Get socket via OSC/UDP
		while(!Thread.interrupted()) {
			if(!isConnected) {
				if(oscConnect() == false) {
					isConnected = false;
					System.out.println("Error making an OSC/UDP socket to " + this.host + ":" + this.sendport);
					System.out.println("Will try again in 10 seconds");
					try {
						Thread.sleep(10000);  // retry in 10 secs
					} catch (InterruptedException iex) {
						System.out.println("Thread " + this + " was interrupted: " + iex);
					}
				} else {
					isConnected = true;
				}
			}
		} // end while
		if(isConnected)
			oscDisconnect();
		
		System.out.println("*** OSCHandler stopped. ***");
	}
	
	public void sendOSC(OSCPacket oscPacket)
	{
		if(oscTransmitter != null && sendAddr != null) {
			try {
				oscTransmitter.send(oscPacket, sendAddr);
				//System.out.println("*** OSC Message sent... ***");
			} catch(IOException ioe) {
				System.out.println("*** Error sending OSC/UDP message! *** " + ioe);
			}
		}
	}
	
	private boolean oscConnect()
	{
		if(isConnected)
			return true;
		
		boolean success;
		try {
			InetAddress localhost = InetAddress.getLocalHost();
			dchRcv = DatagramChannel.open();
			dchSend = DatagramChannel.open();
			// assign an automatic local socket address
			rcvAddr = new InetSocketAddress(localhost, recvport);
			// the send address
			sendAddr = new InetSocketAddress(this.host, sendport);
			dchRcv.socket().bind(rcvAddr);
			oscReceiver = new OSCReceiver(dchRcv);
			oscReceiver.addOSCListener(new OSCListener() {
				// listen for and accept incoming OSC messages
				public void messageReceived(OSCMessage msg, SocketAddress sender, long time)
				{
					// get the address pattern of the msg
					String oscMsg = msg.getName();
					InetSocketAddress addr = (InetSocketAddress) sender;
					System.out.println("=== OSC message received - " + oscMsg + 
							" received from: " + addr.getAddress() + ":" + addr.getPort() + " ===");
					// not doing anything with received osc messages
					// but this is here for possible later use
				}
			});
			oscReceiver.startListening();
			oscTransmitter = new OSCTransmitter(dchSend);
			OSCMessage connect = new OSCMessage("/test", OSCMessage.NO_ARGS);
			sendOSC(connect);

			System.out.println("*** OSC connection successful ***");
			success = true;
		} catch(IOException ioe) {
			System.out.println("*** OSC connection error! ***");
			System.out.println(ioe);
			success = false;
		}
		return success;
	}
	
	// not really necessary but here for the sake of completeness
	void oscDisconnect()
	{
		// stop/close the OSC
		OSCMessage disconnect = new OSCMessage("/test", OSCMessage.NO_ARGS);
		sendOSC(disconnect);
		
		if(oscReceiver != null) {
			try {
				oscReceiver.stopListening();
            } catch(IOException e0) {
            }
        }
        if(dchRcv != null) {
        		try {
        			dchRcv.close();
        		} catch(IOException e1) {
        		}
        }
        if(dchSend != null) {
    		try {
    			dchSend.close();
    		} catch(IOException e2) {
    		}
    }
        System.out.println("*** OSC disconnection successful ***");
	}
	
}
