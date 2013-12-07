package ccs.neu.edu.andang ;

// Programmer: An Dang

// Util 
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.*;
import java.io.ByteArrayOutputStream ;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays ;

// Exceptions:
import java.io.IOException ;
import java.lang.RuntimeException ;

// Networking:
import com.savarese.rocksaw.net.RawSocket;
import static com.savarese.rocksaw.net.RawSocket.PF_INET;

public class RawSocketClient{

    private RawSocket rSock ;
    private String remoteHost ;
    private int destPort;
    private int sourcePort ;
    private InetAddress destAddress;
    private InetAddress sourceAddress ;
    private long currentSeqNum ; // the number of bytes have been sent
    private long currentACKNum ; // the number of bytes have received
    private int numBytesReceived = 0 ;
    private boolean connected = false ;
    private int currentSize = 0 ;
    public RawSocketClient( String remoteHost, int destPort ){

        this.remoteHost = remoteHost ;
		this.destPort = destPort;
		this.sourceAddress =  Util.getSourceExternalIPAddress() ;
		this.sourcePort = Util.getAvailablePort() ;
        this.currentACKNum = this.INITIAL_ACK_NUM ;
        this.currentSeqNum = this.INITIAL_SEQUENCE_NUM ;

    }

    // TODO: implement sending keep alive
    public void disconnect(){
		try{
            if(connected){
                this.tearDown(true, null);
                this.rSock.close() ;
            }
		}
		catch(IOException ex){
			System.out.println( "Unable to disconnect: " + ex.toString() ) ;
		}
    }

    public boolean connect() throws  IOException{
        this.destAddress = Util.getIPAddress( this.remoteHost ) ;

  	    if( this.destAddress.isAnyLocalAddress() || this.destAddress.isLoopbackAddress() ){
  	    	throw new RuntimeException("Internal Error") ;
  	    }
    	this.rSock = new RawSocket () ;

        try{
            this.rSock.open( PF_INET, RawSocket.getProtocolByName("tcp")) ;
            // binding a raw socket to an address causes only packets with a destination
            // matching the address to be delivered to the socket.
            // Also, the kernel will set the source address of outbound packets to the bound address
            // (unless setIPHeaderInclude(true) has been called).
            this.rSock.bind(this.sourceAddress);

            connected = true ;
            handShake() ;

            return true ;
        } catch (IOException e) {
            System.out.println( "Failed to connect to remote server: " + e.toString() ) ;
            return false ;
        }
    }



    public InputStream sendMessage( String message ) throws IOException{

        sendMessage( message, this.getCurrentSeqNum(), this.getCurrentACKNum() , ACK_FLAG);

        this.setCurrentSeqNum( this.getCurrentSeqNum() + message.length() );

        byte[] data = readAll().toByteArray() ;

        InputStream is = new ByteArrayInputStream(  data );

        return is ;
    }

    // create the TCP packet and use RockSaw to send it
    // side-effect: none
    private void sendMessage(String message, long sequenceNum, long ackNum,
                             byte flags) throws IOException{

        TCPHeader header = new TCPHeader( this.sourcePort, this.destPort, sequenceNum ,
                ackNum, flags , AD_WINDOW_SIZE ) ;

        TCPPacket packet = new TCPPacket( header );

/*        System.out.println("******");
        System.out.println("HTTP message: \n" + message);
        System.out.println("******");*/

        if( message != null && ! message.isEmpty() ){
            //header.setFlags( (byte)(flags + 8)); // set PSH on, since we sending small request
            packet.setData( message.getBytes() ) ;
        }

        long checksum = Util.calculateChecksum(
                Util.getChecksumData( packet, this.sourceAddress, this.destAddress) );

        packet.getHeader().setCheckSum((int)checksum) ;
        this.rSock.write( this.destAddress, packet.toByteArray() ) ;

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

        int dataIndex = Util.getDataIndex( header , IP_HEADER_SIZE ) ;

        // check if there are options in TCP header
        if( dataIndex > TCP_IP_HEADERS_MIN_SIZE ){
            header.setOptions( Arrays.copyOfRange( responseData, TCP_IP_HEADERS_MIN_SIZE, dataIndex ) );
        }

        packet = new TCPPacket( header );

        header.getHeaderLength() ;
        if( packetSize - 1 >  dataIndex ){
            setNumBytesReceived(packetSize - dataIndex);

            byte[] receivedData = Arrays.copyOfRange( responseData, dataIndex , packetSize );
            packet.setData( receivedData );

        }

        // TODO: fix this,when packet has data, this check failed
        if( Util.verifyTCPChecksum(packet, this.sourceAddress, this.destAddress) ){
            return packet ;
        }
        System.out.println("checksum failed");
        return null ;
    }


    // handle the TCP's 3 way handshake
    // side-effect: change this.currentSeqNum and this.currentACKNum
    private  void handShake() throws  IOException{

        System.out.println( "HAND SHAKE START") ;
        // send the SYN packet
        sendMessage(null, this.getCurrentSeqNum(), this.getCurrentACKNum(), SYN_FLAG);

        TCPPacket returnedPacket = waitForAck( SYN_FLAG ) ;

        // ACK-ing the SYN/ACK from server and also:
        // -- increase currentSeqNum since the previous SYN packet is successfully received
        // -- capture the server chosen starting sequence number

        // update our seq num
        this.setCurrentSeqNum( this.getCurrentSeqNum() + 1 ) ;

        ackPacket( returnedPacket );   // XXX - what if this ACK does not get to other side

        // get remote host's starting seq num
        this.setCurrentACKNum( returnedPacket.getHeader().getSequenceNumber() + 1);


        System.out.println( "HAND SHAKE COMPLETE") ;


    }

    // tear down process
    // param: byUs: we are the one who send out the first FIN packet
    private void tearDown( boolean byUs, TCPPacket receivedPacket) throws IOException{

        System.out.println( "TEAR DOWN START") ;

        if( byUs ){
            sendMessage( null,
                    this.getCurrentSeqNum() ,
                    this.getCurrentACKNum() ,
                    ACK_FIN_FLAG);

            TCPPacket returnedPacket = waitForAck( ACK_FIN_FLAG ) ;
            this.setCurrentSeqNum(  this.getCurrentSeqNum() + 1  );
            ackPacket(returnedPacket);
            connected = false ;
            System.out.println( "TEAR DOWN COMPLETE") ;
            return;
        }
        else{ // receiving a FIN from the server

            sendMessage( null,
                    this.getCurrentSeqNum() ,
                    receivedPacket.getHeader().getSequenceNumber() + 1 ,
                    ACK_FIN_FLAG);
            waitForAck( ACK_FLAG ) ;
            rSock.close();
            connected = false ;
            System.out.println( "TEAR DOWN COMPLETE") ;
            return;

        }


    }

    // we send out a request , receive an ACK for it
    // and now waiting for packet with data
    private TCPPacket waitForData( ){

        TCPPacket returnedPacket = null ;

        // read from socket until we see the SYN/ACK
        while(true){
            //System.out.println( "looping" ) ;
            returnedPacket = read() ;
            //TODO: take care of potential NullPointerException here

            if( returnedPacket.isFinPacket() ) {
                System.out.println("receiving FIN from server");
                break ;
            }

            if( returnedPacket.getData() != null ){
                break ;
            }
            continue;
        }
        return  returnedPacket ;
    }

    // we send out something and is waiting for an ACK packet
    private TCPPacket waitForAck( byte type ){

        TCPPacket returnedPacket = null ;

        // read from socket until we see the SYN/ACK
        while(true){
            returnedPacket = read() ;
            if( returnedPacket == null ){
                System.out.println("bad packet");
                continue;
            }
            if ( returnedPacket.isAckPacket() ){

                if ( type == ACK_FLAG){
                    break ;
                }
                else if ( type == ACK_FIN_FLAG ){   // TODO: create a flag for FYN
                    if( returnedPacket.isFinPacket() ){
                        // is this the FIN/ACK we are waiting for
                        if( returnedPacket.getHeader().getACKNumber() == this.getCurrentSeqNum() + 1 )
                            break ;
                    }
                }
                else if ( type == SYN_FLAG ){
                    if( returnedPacket.isSynPacket() ){
                        // is this the SYN/ACK we are waiting for
                        if( returnedPacket.getHeader().getACKNumber() == this.getCurrentSeqNum() + 1 )
                            break ;
                    }
                }
            }
            continue;
        }
        return  returnedPacket ;
    }

    // ack a Packet sent from sender
    // we ack all good packet
    // side-effect: NONE  .  It is up to the method that call this message to update the seq and ack num
    private void ackPacket( TCPPacket receivedPacket ) throws IOException{

        sendMessage( null,
                this.getCurrentSeqNum() ,
                receivedPacket.getHeader().getSequenceNumber() + 1 ,
                ACK_FLAG);
    }

    // used when receiving data
    private void ackPacket()  throws IOException{
        sendMessage( null,
                this.getCurrentSeqNum() ,
                this.getCurrentACKNum() ,
                ACK_FLAG);
    }

    // read all incoming packets until it get a complete HTML request for the server
    // the method will send back an ACK to the server if there is no error
    // Assumption: we have finished the handshake and send out the request message
    // side-effect:
    // -- changing this.currentACKNum: when receiving data (NOTE: we need to + 1 with this value when ACKING)
    // -- changing this.currentSeqNum:

    private ByteArrayOutputStream readAll() throws IOException{

        // wait for the ack of our request
        waitForAck( ACK_FLAG ) ;

        ByteArrayOutputStream out = new ByteArrayOutputStream( DATA_BUFFER_SIZE * 2 ) ;

        // read the first packet
        TCPPacket packet = waitForData() ;

        if( packet.getHeader().getSequenceNumber() != this.currentACKNum ){
            System.out.println("current ack: " + this.getCurrentACKNum());
            System.out.println("packet seq: " + packet.getHeader().getSequenceNumber());
            System.out.println("bytes: " + numBytesReceived);
        }

        this.setCurrentACKNum( this.getCurrentACKNum() + this.getNumBytesReceived() );
        this.setNumBytesReceived(0);
        out.write( packet.getData() );
        ackPacket() ;
        // reading the rest
        while(true){
            try {

                packet = waitForData() ;
                /*
                if( numBytesReceived < currentSize ){ // receive the last PDU
                    this.setCurrentACKNum( packet.getHeader().getSequenceNumber() );
                    ackPacket();
                    this.setCurrentACKNum( packet.getHeader().getSequenceNumber() + numBytesReceived );
                    this.setNumBytesReceived(0);
                    out.write( packet.getData() );
                    tearDown( true, null );
                    break ;
                }
                  */
                //System.out.println( "RECEIVING DATA: " + this.getNumBytesReceived() ) ;
                if( packet.isFinPacket() ){

                    if( packet.getData() != null ){
                        if ( isInOrder(packet) ){

                            currentSize = packet.getData().length ;
                            ackPacket(packet); // acking the last piece

                            this.setCurrentACKNum(this.getCurrentACKNum() + this.getNumBytesReceived());
                            this.setNumBytesReceived(0);
                            out.write( packet.getData() );

                        }
                        else{
                            continue;
                        }
                    }
                    tearDown( false, packet );
                    break ;
                }

                if ( isInOrder(packet) ){

                    currentSize = packet.getData().length ;
                    this.setCurrentACKNum(this.getCurrentACKNum() + this.getNumBytesReceived());
                    this.setNumBytesReceived(0);

                    out.write( packet.getData() );
                }

                ackPacket() ;

            } catch (IOException e) {
                System.out.println( "Failed to get data from remote server: " + e.toString() ) ;
            }
        }

        return out ;

    }

     // return true if this packet is in order: it is the packet we expect next
     private boolean isInOrder(TCPPacket packet){

         if( !(packet.getHeader().getSequenceNumber()
                 == this.getCurrentACKNum() )){
             System.out.println("current ack: " + this.getCurrentACKNum());
             System.out.println("packet seq: " + packet.getHeader().getSequenceNumber());
             System.out.println("bytes: " + numBytesReceived);
         }
         return packet.getHeader().getSequenceNumber()
                == this.getCurrentACKNum() ;
    }

    boolean isIPSupported() throws SocketException {
    	return this.rSock.getIPHeaderInclude() ;
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

    private final byte SYN_FLAG = (byte) 2;
    private final byte ACK_FLAG = (byte) 16;
    private final byte ACK_FIN_FLAG = (byte) 17;
    private final int AD_WINDOW_SIZE = 2000;
    private final int DATA_BUFFER_SIZE = 2000 ;
    private final int IP_HEADER_SIZE = 20 ;
    private final int TCP_IP_HEADERS_MIN_SIZE = 40 ;
    private final long INITIAL_SEQUENCE_NUM = 0l ;
    private final long INITIAL_ACK_NUM = 0l ;

    public long getCurrentSeqNum() {
        return currentSeqNum;
    }

    public void setCurrentSeqNum(long currentSeqNum) {

     /*   System.out.println( "**********" ) ;
        System.out.println( "SETTING SEQ NUM" ) ;
        System.out.println( "old: "  + this.getCurrentSeqNum()) ;
        System.out.println( "new: "  + currentSeqNum) ;
        System.out.println( "**********" ) ;*/

        this.currentSeqNum = currentSeqNum;
    }

    public long getCurrentACKNum() {
        return currentACKNum;
    }

    public void setCurrentACKNum(long currentACKNum) {

/*        System.out.println( "**********" ) ;
        System.out.println( "SETTING ACK NUM" ) ;
        System.out.println( "old: "  + this.getCurrentACKNum()) ;
        System.out.println( "new: "  + currentACKNum) ;
        System.out.println( "**********" ) ;*/

        this.currentACKNum = currentACKNum;
    }

    public int getNumBytesReceived() {
        return numBytesReceived;
    }

    public void setNumBytesReceived(int numBytesReceived) {
/*        System.out.println( "**********" ) ;
        System.out.println( "RECEIVING bytes" ) ;
        System.out.println( "old: "  + this.getNumBytesReceived()) ;
        System.out.println( "new: "  + numBytesReceived) ;
        System.out.println( "**********" ) ;*/
        this.numBytesReceived = numBytesReceived;
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