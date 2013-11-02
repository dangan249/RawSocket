package ccs.neu.edu.andang ;

import java.net.URL ;
import java.net.InetAddress ;
import java.net.SocketException ;
import java.io.IOException ;

import com.savarese.rocksaw.net.RawSocket;
import static com.savarese.rocksaw.net.RawSocket.PF_INET;

public class RawSocketClient{

	private RawSocket rSockSender ;
	private RawSocket rSockReceiver ;

/* TCP functionalities supported:

Packet = IP Header + TCP Header + Data

-- Verify the checksums of incoming TCP packets
-- Generate correct checksums for outgoing packets.
-- Select a valid local port to send traffic on
-- Perform the three-way handshake
-- Handle connection teardown. 
-- Handle sequence and acknowledgement numbers. 
-- Manage the advertised window as you see fit. 
-- Include basic timeout functionality: 
==> if a packet is not ACKed within 1 minute, assume the packet is lost and retransmit it. 
-- Able to receive out-of-order incoming packets and put them back into the correct order
-- Identify and discard duplicate packets. 
-- Implement a basic congestion window: 
==> start with cwnd=1, 
==> increment the cwnd after each succesful ACK, up to a fixed maximum of 1000 
-- If your program observes a packet drop or a timeout, reset the cwnd to 1.

*/
    private String remoteHost ;
    private int remotePort ;

    // TODO: set up the sender and receiver raw socks
    public RawSocketClient( String remoteHost, int remotePort ){
		this.host = host ;
		this.port = port ;
		// ...
    }

    // TODO: handling TCP teardown process
    public void disconnect(){
		try{
			// ...
			this.rSockSender.close() ;
			this.rSockReceiver.close() ;
		}
		catch(IOException ex){
			System.out.println( "Unable to disconnect: " + ex.toString() ) ;
		}
    }
    
    // TODO: connect to the remote server + doing the handshake
    public boolean connect() throws UnknownHostException, SocketException, IOException{

    	this.rSock = new RawSocket () ;
		this.rSock.open( PF_INET, RawSocket.getProtocolByName("tcp")) ;

		URL destURL = new URL( this.remoteHost ) ;

  	    InetAddress address = InetAddress.getByName( destURL.getHost() );

  	    if( address.isAnyLocalAddress() || address.isLoopbackAddress()
  	     || address.isLinkLocalAddress() ){
  	    	return false ;
  	    }

  	    // ...

  	    return true ;
    }
    
   
    // send the message to the remote server that we connect with
    // return: the InputStream from the server
    public InputStream sendMessage( String message ) throws IOException{

    }	

    boolean isIPSupported(){
    	return client.rSockSender.getIPHeaderInclude() ;
    } 









/*
	public static void main( String args[] ){
		
		RawSocketClient client = new RawSocketClient() ;
		try{

		}
		catch (SocketException ex){
			System.out.println( ex.toString() ) ;
		}
		catch( IOException ex ){
			System.out.println( ex.toString() ) ;
		}
	}
	*/
}