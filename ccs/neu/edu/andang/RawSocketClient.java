package ccs.neu.edu.andang ;

// Programmer: An Dang

// Util 
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.*;
import java.util.Random ;
import java.util.Collections ;
import java.util.Enumeration;
import java.io.ByteArrayOutputStream ;
import java.nio.ByteBuffer ;
import java.util.Arrays ;

// Exceptions:
import java.io.IOException ;
import java.lang.RuntimeException ;

// Networking:
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
    private final int IP_HEADER_SIZE = 20 ;
    private final int TCP_IP_HEADERS_MIN_SIZE = 40 ;
    private final long INITIAL_SEQUENCE_NUM = 0l ;
	private final long INITIAL_ACK_NUM = 0l ;

/* TCP functionalities supported:

Packet = IP Header + TCP Header + Data

-- Verify the checksums of incoming TCP packets (done)
-- Generate correct checksums for outgoing packets. (done)
-- Select a valid local port to send traffic on (done)
-- Perform the three-way handshake (done)
-- Handle connection tear down.
-- Handle sequence and acknowledgement numbers. 
-- Manage the advertised window as you see fit. 
-- Include basic timeout functionality: 
==> if a packet is not ACKed within 1 minute, assume the packet is lost and retransmit it. 
-- Able to receive out-of-order incoming packets and put them back into the correct order
-- Identify and discard duplicate packets. 
-- Implement a basic congestion window: 
==> start with cwnd=1, 
==> increment the cwnd after each successful ACK, up to a fixed maximum of 1000
-- If your program observes a packet drop or a timeout, reset the cwnd to 1.

*/
    private String remoteHost ;
    private int destPort;
    private int sourcePort ;
    private InetAddress destAddress;
    private InetAddress sourceAddress ;
    private long currentSeqNum ;
    private long currentACKNum ;

    // TODO: set up the sender and receiver raw socks
    public RawSocketClient( String remoteHost, int destPort ){

        this.remoteHost = remoteHost ;
		this.destPort = destPort;
		this.sourceAddress =  getSourceExternalIPAddress() ;	
		this.sourcePort = getAvailablePort() ;
        this.currentACKNum = INITIAL_ACK_NUM ;
        this.currentSeqNum = INITIAL_SEQUENCE_NUM ;

    }

    // TODO: handling TCP teardown process
    public void disconnect(){
		try{
            tearDown() ;
			this.rSock.close() ;
		}
		catch(IOException ex){
			System.out.println( "Unable to disconnect: " + ex.toString() ) ;
		}
    }

    // TODO: connect to the remote server + doing the handshake
    public boolean connect() throws UnknownHostException, SocketException, IOException{
        this.destAddress = getIPAddress( this.remoteHost ) ;

  	    if( this.destAddress.isAnyLocalAddress() || this.destAddress.isLoopbackAddress() ){
  	    	throw new RuntimeException("Internal Error") ;
  	    }
    	this.rSock = new RawSocket () ;
        this.rSock.open( PF_INET, RawSocket.getProtocolByName("tcp")) ;
        // binding a raw socket to an address causes only packets with a destination
        // matching the address to be delivered to the socket.
        // Also, the kernel will set the source address of outbound packets to the bound address
        // (unless setIPHeaderInclude(true) has been called).
        this.rSock.bind(this.sourceAddress);

        handShake() ;
  	    return true ;
    }





    // send the message to the remote server that we connect with
    // return: the InputStream from the server
    public InputStream sendMessage( String message ) throws IOException{

        sendMessage( message, this.currentSeqNum, this.currentACKNum, (byte) 0, WINDOW_SIZE );

        TCPPacket returnedPacket = read() ;

        if( returnedPacket == null ){
            throw new SocketException("Unable to get data back from the remote server") ;
        }

        return new ByteArrayInputStream( returnedPacket.getData() ) ;

    }

    // handle the TCP's 3 way handshake
    private  void handShake(){
        System.out.println( "HANDSHAKING");
        try {

            sendMessage( null, INITIAL_SEQUENCE_NUM, INITIAL_ACK_NUM, SYN_FLAG, WINDOW_SIZE );

            TCPPacket returnedPacket = null ;
            while(true){
                returnedPacket = read() ;
                if( returnedPacket.getHeader().isACKFlagOn()
                 && returnedPacket.getHeader().isSYNFlagOn()
                 && returnedPacket.getHeader().getACKNumber() == this.currentSeqNum + 1l ){
                    break ;
                }
            }
            // ACK-ing the SYN/ACK from server
            sendMessage( null, ++this.currentSeqNum,
                         this.currentACKNum = returnedPacket.getHeader().getSequenceNumber() + 1l,
                         ACK_FLAG, WINDOW_SIZE );

            System.out.println( "FINISHING HANDSHAKE") ;

        } catch (IOException e) {
            System.out.println( "Failed to connect to remote server: " + e.toString() ) ;
        }

    }

    // TODO: understand the behavior of TCP connection tear down
    // -- what the server is sending
    // -- who send the first ACK
    // http://www.google.com/url?q=http%3A%2F%2Fpacketlife.net%2Fblog%2F2010%2Fjun%2F7%2Funderstanding-tcp-sequence-acknowledgment-numbers%2F&sa=D&sntz=1&usg=AFQjCNHwKGd6mciB3figZegTe_Ie5K5qcA
    private void tearDown(){

        try {
            // sending FYN packet with current sequence number and ACK number
            sendMessage( null, this.currentSeqNum, this.currentACKNum, FIN_FLAG, WINDOW_SIZE );
            TCPPacket returnedPacket = null ;

            // reading the ACK and FIN from server
            while(true){

                returnedPacket = read()  ;

                if( returnedPacket.getHeader().isACKFlagOn()
                 && returnedPacket.getHeader().isFINFlagOn()){
                 //&& returnedPacket.getHeader().getACKNumber() == this.currentSeqNum + 1l ){
                    break ;
                }

            }

            sendMessage( null, this.currentSeqNum ,
                    this.currentACKNum = returnedPacket.getHeader().getSequenceNumber() + 1l,
                    ACK_FLAG, WINDOW_SIZE );

            System.out.println( "FINISHING HANDSHAKE") ;
        } catch (IOException e) {
            System.out.println( "Failed to connect to remote server: " + e.toString() ) ;
        }

    }

    // TODO: fix the check for correct check-summed packet
    // read one incoming packet (It can be any packet, we need to filter out the correct TCP packet
    // that this program is using)
    // and verify the checksum
    private TCPPacket read(){

        byte[] responseData = new byte[DATA_BUFFER_SIZE] ;
        TCPPacket packet = null ;
        TCPHeader header = null ;
        int packetSize = 0 ;
        do{
            try {
                packetSize = this.rSock.read( responseData , sourceAddress.getAddress()) ;
            } catch (IOException e) {
                System.out.println( "Failed to read data from remote server: " + e.toString() ) ;
            }
            // get the TCP header and ignore all optional fields
            header =  new TCPHeader( Arrays.copyOfRange( responseData, IP_HEADER_SIZE, TCP_IP_HEADERS_MIN_SIZE ) );
        }
        while ( header.getDestinationPort() != this.sourcePort );  // de-multiplexing

        int dataIndex = getDataIndex( header ) ;

        // check if there are options in TCP header
        if( dataIndex > TCP_IP_HEADERS_MIN_SIZE ){
            header.setOptions( Arrays.copyOfRange( responseData, TCP_IP_HEADERS_MIN_SIZE, dataIndex ) );
        }

        System.out.println("INCOMING PACKET: ") ;
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


        packet = new TCPPacket( header );

        if( packetSize - 1 >  dataIndex ){
            //System.out.println("setting data") ;
            packet.setData( Arrays.copyOfRange( responseData, dataIndex , responseData.length ));
            this.currentACKNum += packet.getData().length + 1 ;
        }


        // TODO: fix this
        if( verifyChecksum( packet ) ){
            System.out.println( "check sum correctlly") ;
            return packet ;
        }

        return packet ;
    }


    // create the TCP packet and use RockSaw to send it
    private void sendMessage( String message, long sequenceNum, long  ackNum,
                              byte flags, int winSize) throws IOException{

        TCPHeader header = new TCPHeader( this.sourcePort, this.destPort, sequenceNum ,
                                          ackNum, flags , winSize ) ;
        TCPPacket packet = new TCPPacket( header );

        if( message != null && ! message.isEmpty() )
            packet.setData( message.getBytes() ) ;

        int checksum = Util.generateChecksum( getChecksumData( packet ) );

        packet.getHeader().setCheckSum(checksum) ;

        System.out.println("OUTGOING PACKET: ") ;
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

        this.rSock.write( this.destAddress, packet.toByteArray() ) ;

	}

    boolean isIPSupported() throws SocketException {
    	return this.rSock.getIPHeaderInclude() ;
    } 


    // return the index where the data portion of a packet start
    private  int getDataIndex(TCPHeader header){
        return IP_HEADER_SIZE + header.getHeaderLength() * 4 ;
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

	// return the pseudo header needed to calculate  checksum
	private byte[] getChecksumData( TCPPacket packet ){
		
		byte[] sourceAddress = this.sourceAddress.getAddress() ;
		byte[] destAddres = this.destAddress.getAddress() ;
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

    // return true if the checksum in the receiving header
    private boolean verifyChecksum( TCPPacket packet ){
        return packet.getHeader().getChecksum()
                == Util.generateChecksum( getChecksumData( packet ) ) ;
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

    // return true if the given port is available
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
				new URL("http://www.ccs.neu.edu/home/cbw/4700/project4.html").getHost() ,80 ) ;
            System.out.println("CONNECTING");
			client.connect() ;
            System.out.println("DISCONNECTING");
            client.disconnect() ;

        }
		catch (SocketException ex){
			System.out.println( ex.toString() ) ;
		}
		catch( IOException ex ){
			System.out.println( ex.toString() ) ;
		}
	}
}

/*
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
*/