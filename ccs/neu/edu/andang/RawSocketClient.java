package ccs.neu.edu.andang ;

// Util 
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Random ;
import java.util.Collections ;
import java.util.Enumeration;
import java.io.ByteArrayOutputStream ;
import java.nio.ByteBuffer ;
import java.util.Arrays ;

// Exceptions:
import java.net.SocketException ;
import java.net.UnknownHostException ;
import java.net.MalformedURLException ;
import java.io.IOException ;
import java.lang.RuntimeException ;

// Networking:
import java.net.URL ;
import java.net.InetAddress ;
import java.net.Inet4Address ;
import java.net.NetworkInterface ;
import java.net.ServerSocket ;
import com.savarese.rocksaw.net.RawSocket;
import static com.savarese.rocksaw.net.RawSocket.PF_INET;

public class RawSocketClient{

	private RawSocket rSock ;
	private final byte URG_FLAG = (byte) 32;
	private final byte FIN_FLAG = (byte) 1;
	private final byte SYN_FLAG = (byte) 2;
	private final byte ACK_SYN_FLAG = (byte) 18;
	private final byte ACK_FLAG = (byte) 16;
	private final byte ACK_FIN_FLAG = (byte) 17;
	private final int WINDOW_SIZE = 14600 ;
    private final int DATA_BUFFER_SIZE = 1460 ;
    private final long INITIAL_SEQUENCE_NUM = 0l ;
	private final long INITIAL_ACK_NUM = 0l ;

/* TCP functionalities supported:

Packet = IP Header + TCP Header + Data

-- Verify the checksums of incoming TCP packets (done)
-- Generate correct checksums for outgoing packets. (done)
-- Select a valid local port to send traffic on (done)
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
    private int sourcePort ;
    private InetAddress remoteAddress ;
    private InetAddress sourceAddress ;
    private long currentSeqNum ;
    private long currentACKNum ;

    // TODO: set up the sender and receiver raw socks
    public RawSocketClient( String remoteHost, int remotePort ){
		this.remoteHost = remoteHost ;
		this.remotePort = remotePort ;
		this.sourceAddress =  getSourceExternalIPAddress() ;	
		this.sourcePort = getAvailablePort() ;


        System.out.println("Remote host: " + this.remoteHost ) ;
        System.out.println("Remote port: " + this.remotePort) ;
        System.out.println("Source address: " + this.sourceAddress) ;
        System.out.println("Source port: " + this.sourcePort ) ;

        this.currentACKNum = INITIAL_ACK_NUM ;
        this.currentSeqNum = INITIAL_SEQUENCE_NUM ;
    }

    // TODO: handling TCP teardown process
    public void disconnect(){
		try{
			this.rSock.close() ;
		}
		catch(IOException ex){
			System.out.println( "Unable to disconnect: " + ex.toString() ) ;
		}
    }
    
    // TODO: connect to the remote server + doing the handshake
    public boolean connect() throws UnknownHostException, SocketException, IOException{

  	    this.remoteAddress = getIPAddress( this.remoteHost ) ;

  	    if( this.remoteAddress.isAnyLocalAddress() || this.remoteAddress.isLoopbackAddress() ){
  	    	throw new RuntimeException("Internal Error") ;
  	    }
    	this.rSock = new RawSocket () ;
		this.rSock.open( PF_INET, RawSocket.getProtocolByName("tcp")) ;

        handShake() ;
  	    return true ;
    }


    private byte[] read(){
        byte[] responseData = new byte[DATA_BUFFER_SIZE] ;
        TCPHeader header = null ;
        do{
            try {
                this.rSock.read( responseData , sourceAddress.getAddress()) ;
            } catch (IOException e) {
                System.out.println( "Failed to read data from remote server: " + e.toString() ) ;
            }
            // get the TCP header and ignore all optional fields
            header = new TCPHeader( Arrays.copyOfRange( responseData, 20, 40 ) ) ;

        }
        while ( header.getDestinationPort() != this.sourcePort && verifyChecksum( header ) );  // de-multiplexing

        System.out.println("*******************************************************") ;
        System.out.println("PACKET: ") ;
        System.out.println( "Source port: " + header.getSourcePort() ) ;
        System.out.println( "Destination port: " + header.getDestinationPort() ) ;
        System.out.println(  "ACK num: " + header.getACKNumber() ) ;
        System.out.println(  "Sequence num: " + header.getSequenceNumber() ) ;
        System.out.println(  "Window size: " + header.getWindowSize() ) ;
        System.out.println(  "Checksum: " + header.getChecksum() ) ;
        System.out.println(  "Header length: " + header.getHeaderLength() ) ;
        System.out.println( "ACK FLAG on: " + header.isACKFlagOn() ) ;
        System.out.println( "SYN FLAG on: " + header.isSYNFlagOn() ) ;
        System.out.println( "FYN FLAG on: " + header.isFINFlagOn() ) ;
        System.out.println("*******************************************************") ;

        return responseData ;
    }
    private  void handShake(){

        try {

            sendMessage( null, INITIAL_SEQUENCE_NUM, INITIAL_ACK_NUM, SYN_FLAG, WINDOW_SIZE );

            byte[] responseData = read() ;

            TCPHeader header = new TCPHeader( Arrays.copyOfRange( responseData, 20, 40 ) ) ;
            sendMessage( null, ++this.currentSeqNum, this.currentACKNum = header.getSequenceNumber() + 1l,
                    ACK_FLAG, WINDOW_SIZE );

        } catch (IOException e) {
            System.out.println( "Failed to connect to remote server: " + e.toString() ) ;
        }

    }


    // send the message to the remote server that we connect with
    // return: the InputStream from the server
    public InputStream sendMessage( String message ) throws IOException{

        sendMessage( message, this.currentSeqNum, this.currentACKNum, ACK_FLAG, WINDOW_SIZE );
        byte[] responseData = read() ;

        TCPHeader header = new TCPHeader( Arrays.copyOfRange( responseData, 20, 40 ) ) ;

        int dataIndex = header.getHeaderLength() * 4 ;

        return new ByteArrayInputStream( Arrays.copyOfRange( responseData, dataIndex, responseData.length ) ) ;

    }	

    // create the TCP packet and use RockSaw to send it
    private void sendMessage( String message, long sequenceNum, long  ackNum,
                              byte flags, int winSize) throws IOException{

        TCPHeader header = new TCPHeader( this.sourcePort, this.remotePort , sequenceNum ,
                                          ackNum, flags , winSize ) ;
        TCPPacket packet = new TCPPacket( header );

        if( message != null && ! message.isEmpty() )
            packet.setData( message.getBytes() ) ;

        int checksum = Util.generateChecksum( getChecksumData( packet ) );
        packet.getHeader().setCheckSum(checksum) ;

        System.out.println("*******************************************************") ;
        System.out.println("PACKET: ") ;
        System.out.println( "Source port: " + header.getSourcePort() ) ;
        System.out.println( "Destination port: " + header.getDestinationPort() ) ;
        System.out.println(  "ACK num: " + header.getACKNumber() ) ;
        System.out.println(  "Sequence num: " + header.getSequenceNumber() ) ;
        System.out.println(  "Window size: " + header.getWindowSize() ) ;
        System.out.println(  "Checksum: " + header.getChecksum() ) ;
        System.out.println(  "Header length: " + header.getHeaderLength() ) ;
        System.out.println( "ACK FLAG on: " + header.isACKFlagOn() ) ;
        System.out.println( "SYN FLAG on: " + header.isSYNFlagOn() ) ;
        System.out.println( "FYN FLAG on: " + header.isFINFlagOn() ) ;
        System.out.println("*******************************************************") ;

        this.rSock.write( this.remoteAddress , packet.toByteArray() ) ;

	}

    boolean isIPSupported() throws SocketException {
    	return this.rSock.getIPHeaderInclude() ;
    } 

    // parsing a URL string and give back the corresponding InetAddress object
    private InetAddress getIPAddress( String host ) throws UnknownHostException, MalformedURLException{
  	    return InetAddress.getByName( host );
    }

    // Strategy: pick a random port from [49152,65535]
    // use ServerSocket to verify that it iss open
    private int getAvailablePort() {
    	int port = 0;
        try {
            do {
                port = (new Random()).nextInt(65535 - 49152 + 1) + 49152;
            } while (!isPortAvailable(port));
        } catch (IOException e) {
            System.out.println( e.toString() ) ;
        }

        return port;
	}

	// return the pseudo header needed to calculate checksum
	private byte[] getChecksumData( TCPPacket packet ){
		
		byte[] sourceAddress = this.sourceAddress.getAddress() ;
		byte[] destAddres = this.remoteAddress.getAddress() ;
		byte[] reserved  = new byte[]{ (byte) 0 };
		byte[] protocol  = new byte[]{ (byte) 6 };

		short tcpSegmentLength = (short) packet.length() ;

		ByteBuffer b = ByteBuffer.allocate(2);
		b.putShort(tcpSegmentLength); 

		ByteArrayOutputStream out = new ByteArrayOutputStream( );

		try{
			out.write( sourceAddress );
			out.write( destAddres );
			out.write( reserved ) ;
			out.write( protocol );
			out.write( b.array() );
			out.write( packet.toByteArray() ) ;
		}
		catch(IOException ex){
			System.out.println( ex.toString() ) ;
		}
		return out.toByteArray();
	}

	// return external IP address of this machine, which hosts the program
	private InetAddress getSourceExternalIPAddress(){
		InetAddress result = null ;
		try{       

			Enumeration<NetworkInterface> allInterfaces = NetworkInterface.getNetworkInterfaces();	        
	        for (NetworkInterface anInterface : Collections.list( allInterfaces )) {
	            
	            Enumeration addresses = anInterface.getInetAddresses();	    		
	    		while(addresses.hasMoreElements()) {
	        		InetAddress ia= (InetAddress) addresses.nextElement();	        		
	        		
	        		if(! ia.isLoopbackAddress() )
	        			if( ia instanceof Inet4Address ){
	        				result = ia ;
	        			}	        			
	        	}

	    	}
	    }
		
		catch ( SocketException e) {
	    	System.out.println( e.toString() ) ;
	    }
	    
	    return result;
	}

    private boolean verifyChecksum( TCPHeader header ){

        return header.getChecksum()
                == Util.generateChecksum( getChecksumData( new TCPPacket( header )) ) ;
    }

	private boolean isPortAvailable( int port ) throws IOException {

	    ServerSocket sock = null;
	    try {
	        sock = new ServerSocket(port);
	        sock.setReuseAddress(true);
	        return true;
	    } catch ( IOException e) {
	    	System.out.println( e.toString() ) ;
	    } finally {
	        if (sock != null) {
	            sock.close();
	        }
	    }

	    return false;
	}



	public static void main( String args[] ){
				
		try{
			RawSocketClient client = new RawSocketClient( 
				"http://www.ccs.neu.edu/home/cbw/4700/project4.html",80 ) ;
			client.connect() ;
		}
		catch (SocketException ex){
			System.out.println( ex.toString() ) ;
		}
		catch( IOException ex ){
			System.out.println( ex.toString() ) ;
		}
	}
}