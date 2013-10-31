package ccs.neu.edu.andang ;

// I/O classes
import java.io.InputStream ;
import java.io.InputStreamReader ;
import java.io.BufferedReader ;

// Exceptions 
import java.io.IOException ;
import java.net.UnknownHostException ;
import java.net.SocketException ;
import java.net.MalformedURLException;
import java.lang.RuntimeException ;

// Util classes
import com.google.common.collect.Multimap ;
import com.google.common.collect.HashMultimap ; 
import java.util.Map ;
import java.util.HashMap ;
import java.util.Iterator ;
import java.lang.StringBuilder ;
import java.net.URL ; // ONLY USED to represent an URL, 
                      //not used to handle any networking activities

// HTTP 1.0
public class HTTPClient{


	private HTTPRequest request;
	// without making the request this.response will be null
	// ==> cause NullPointerException (not a bad way to inform client :) )
	private HTTPResponse response ; 

	// TODO
	// parse the entire response message to create an HTTPResponse
	// side-effect: populate this.response
	private void deserializeResponse( InputStream input) throws RuntimeException, IOException{
		

		BufferedReader reader = null ;
		// use BufferedReader to take advantage of its "readLine" method
		reader = new BufferedReader(new InputStreamReader( input , "US-ASCII") )  ;


		// START POPULATING this.response 

		this.response = new HTTPResponse() ;
		this.response.setURL( this.getRequest().getURL() ) ;

		// PARSING THE FIRST LINE
		String firstLine = reader.readLine() ;
		if( firstLine == null || firstLine.isEmpty() ){
			throw new RuntimeException( this.request.getURL().getHost() + 
				" does not return proper HTTP message") ;
		}

		String[] firstLineElements = firstLine.split("\\s") ;

		try{
			int code = Integer.parseInt( firstLineElements[1] ) ;
			switch (code){
				case 200: 
					this.response.setStatusCode( StatusCode.OK ) ;
					break;
				case 301: 
					this.response.setStatusCode( StatusCode.MOVED_PERMANENTLY ) ;
					break;
				case 302: 
					this.response.setStatusCode( StatusCode.MOVED_TEMPORARILY ) ;				
					break;
				case 400: 
					this.response.setStatusCode( StatusCode.BAD_REQUEST ) ;
					break;
				case 403: 
					this.response.setStatusCode( StatusCode.FORBIDDEN ) ;
					break;
				case 500: 
					this.response.setStatusCode( StatusCode.INTERNAL_SERVER_ERROR ) ;
					break;	
				default:
					throw new RuntimeException( this.request.getURL().getHost() + 
						" return unknown HTTP status code: " + code) ;
			}

		}
		catch( NumberFormatException ex ){
			throw new RuntimeException( this.request.getURL().getHost() + 
				" does not return proper HTTP Status Code: " + ex.toString() ) ;
		}

			

		// PARSING HEADERS	
		Multimap<String,String> headers = HashMultimap.create() ;
		Map<String,String> cookies = new HashMap<String,String>() ;
		String line = reader.readLine() ;
		
		while( line != null && !line.isEmpty() ){
			int colonIndex = line.indexOf( ':' ) ;
			if( colonIndex < 0 ) break ; // this may signal the end of headers or server just mess up :)
			
			String tempkey = line.substring( 0, colonIndex ).trim();
			String tempvalue = line.substring( ++colonIndex ).trim();

			// POPULATE HTTPResponse.cookies
			if (tempkey.equals("Set-Cookie")) {

				int equalIndex = tempvalue.indexOf( '=' ) ;
				int semicolonIndex = tempvalue.indexOf( ';' ) ;
				cookies.put( tempvalue.substring( 0, equalIndex ) , 
						 tempvalue.substring( equalIndex+1 , semicolonIndex) ) ;
			}

			else
				headers.put(tempkey, tempvalue);			

			line = reader.readLine() ;
		}

		this.response.setHeaders( headers ) ;
		this.response.setCookies( cookies ) ;

		// PARSING BODY
		StringBuilder builder = new StringBuilder() ;
		while( (line = reader.readLine() ) != null ){
			builder.append( line ) ;
		}

		this.response.setResponseBody( builder.toString() ) ;

		// FINISH POPULATING this.response 
	}

	public void doGet() throws UnknownHostException, SocketException, 
										 IOException, MalformedURLException{

		
		sendRequest( HTTPMethod.GET) ;
		
	}

	public void doPost() throws UnknownHostException, SocketException, 
										 IOException, MalformedURLException{


		sendRequest( HTTPMethod.POST ) ;
	}

	// Same comment as the function doPostWithRedirect()
	public void doGetWithRedirect() throws UnknownHostException, SocketException, 
										 IOException, MalformedURLException{


		sendRequest( HTTPMethod.GET ) ;

		handleRedirect() ;

	}


	public void doPostWithRedirect()  throws UnknownHostException, SocketException, 
										 IOException, MalformedURLException{


		sendRequest( HTTPMethod.POST ) ;
		handleRedirect() ;
	}


	private void sendRequest( HTTPMethod method) throws UnknownHostException, SocketException, 
										 IOException, MalformedURLException{

		
		SocketClient sock = null ;
		try{
			int port = this.request.getURL().getPort() ;
			sock = new SocketClient( this.request.getURL().getHost(), 
									 port == -1 ? 80 : port,
									 false ) ;
			sock.connect() ;

			String getMessage = this.request.serializeRequest( method ) ;
			
			InputStream input = null ;
			input = sock.sendMessage( getMessage ) ;			
			deserializeResponse( input ) ;			
		}
		catch(Exception ex ){
			System.out.println( "Problem with sending request: " + ex.toString() ) ;
		}
		finally{
			sock.disconnect() ;
		}

	}

	private void handleRedirect() throws UnknownHostException, SocketException, 
										 IOException, MalformedURLException{

		if( response.getStatusCode() == StatusCode.MOVED_TEMPORARILY ||
				response.getStatusCode() == StatusCode.MOVED_PERMANENTLY ){
			Map<String, String> cookies = this.request.getCookies() ;
			cookies.put("sessionid" , this.response.getCookies().get("sessionid") ) ; // GRAB the new session ID
			this.request.getHeaders().removeAll("Content-Length") ;
			this.request.getHeaders().removeAll("Content-Type") ;
			this.request.getHeaders().removeAll("Cookie") ;

			Multimap<String,String> newHeaders = this.request.getHeaders() ;


			//System.out.println( "\nREDIRECT RESPONSE: \n" ) ;
			//System.out.println( response.toString() ) ;

			Iterator<String> iter = response.getHeaders().get("Location").iterator() ;
			if ( iter.hasNext() ){
				String newLocation  =  iter.next(); 
				// CONSTRUCT a new HTTPRequest
				this.request = new HTTPRequest( new URL( newLocation )  ) ;
				request.setHeaders( newHeaders ) ;
				request.addCookies( cookies ) ;
				sendRequest( HTTPMethod.GET ) ;

			}
			else{
				throw new RuntimeException("Expect a redirect URL but found none.") ;
			}
		}

	}

	public HTTPRequest getRequest(){
		return this.request ;
	}

	public void setRequest(HTTPRequest request){
		this.request = request ;
	}

	public HTTPResponse getResponse(){
		return this.response ;
	}

	public enum HTTPMethod{

		POST("POST"),
		GET("GET");

		private final String value ;
		HTTPMethod( String value ){
			this.value = value ;
		}

		String value(){
			return value ;
		}
	}

	public enum StatusCode {
		OK(200),
		BAD_REQUEST(400),
		FORBIDDEN(403),
		INTERNAL_SERVER_ERROR(500),
		MOVED_PERMANENTLY(301),
		MOVED_TEMPORARILY(302);

		private final int value ;
		StatusCode( int value ){
			this.value = value ;
		}

		int value(){
			return value ;
		}
	}


}

