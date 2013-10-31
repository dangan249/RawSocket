package ccs.neu.edu.andang ;

import java.net.Socket ;
import java.net.InetAddress ;
import java.net.UnknownHostException ;
import java.net.SocketException ;

import java.io.IOException ;
import java.io.OutputStreamWriter ;
import java.io.InputStream ;

import java.io.PrintWriter ;
import java.io.OutputStream ;

import javax.net.ssl.*;

// SocketClient is the low level class that deal with Socket and sending message
public class SocketClient{
        
    private String host ;
    private int port ;
    private Socket sock ;
    private boolean secure ;
    
	private SSLSocketFactory f;
    
    
    public SocketClient( String host, int port, boolean secure ){
		this.host = host ;
		this.port = port ;
		this.secure = secure ;
    }

    public void disconnect(){
		try{
			this.sock.close() ;
		}
		catch(IOException ex){
			System.out.println( "Unable to disconnect: " + ex.toString() ) ;
		}
    }
    
	// side-effect: create a socket and assign it to this.sock
    public boolean connect() throws UnknownHostException, SocketException, IOException{
		// establish connection
		// let the OS decide the local port, local address
		if ( ! this.secure )
			this.sock = new Socket( InetAddress.getByName(this.host),this.port,null,0) ;
		else {
			SSLSocketFactory f = (SSLSocketFactory) SSLSocketFactory.getDefault();
			this.sock = f.createSocket(InetAddress.getByName( this.host ), this.port);
			( (SSLSocket) this.sock ).startHandshake();
		}   
		return true ;	
    }
    
    
    // send the message to the remote server that we connect with
    // return: the InputStream from the server
    public InputStream sendMessage( String message ) throws IOException{

    	// need InputStreamReader + OutputStreamWriter to set character encoding    		
    	// no need to close these streams, since disconnect() will do it
    	PrintWriter writer = null ;	

    	// set auto-flush to true for PrintWriter, thus after writing the message
    	// all we need to do is call PrintWriter object's println() to send the message
		writer = new PrintWriter( new OutputStreamWriter(  this.sock.getOutputStream()  ,"US-ASCII" ), true );
		
		// send the message
		writer.write( message, 0, message.length() ) ;
		writer.println() ;
		
		return this.sock.getInputStream()  ;

    }
    
}
