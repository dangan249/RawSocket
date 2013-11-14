/*
	Programmers: An Dang + Pedro Benedicte
	Network Fundamentals
	Project 2 - Fakebook Web Crawler

*/

package ccs.neu.edu.andang ;

import java.io.*;
import java.util.Iterator ;
import com.google.common.collect.Multimap ;
import com.google.common.collect.HashMultimap ;


import java.net.UnknownHostException ;
import java.net.SocketException ;
import java.net.MalformedURLException;

import java.net.URL ;

public class RawHTTPGet {

	private String hostURL  ;
	private HTTPClient client ;

	public RawHTTPGet( String hostURL ){
		this.client = new HTTPClient() ;
        this.hostURL = hostURL ;
	}

	public void downloadPage(){

        HTTPRequest request;
        try {
            request = new HTTPRequest( new URL( this.hostURL ) );
            Multimap<String,String> headers = HashMultimap.create() ;
            headers.put( "From" , "dang.an249@gmail.com" ) ;
            request.setHeaders( headers ) ;
            //request.addCookies( this.cookies ) ;
            this.client.setRequest( request ) ;
            client.doGetWithRedirect() ;
        }
        catch( UnknownHostException ex){
            System.out.println("Unable to connect to " + client.getRequest().getURL() + ". Unknown host" ) ;
        }
        catch( SocketException ex){
            System.out.println( "Error with underlying protocol: " + ex.toString() ) ;
        }
        catch( MalformedURLException ex ){
            System.out.println( "Invalid URL: " + ex.toString() ) ;
        }
        catch( IOException ex){
            System.out.println( ex.toString() ) ;
        }

        HTTPClient.StatusCode stat = client.getResponse().getStatusCode();


        if (stat == HTTPClient.StatusCode.INTERNAL_SERVER_ERROR) {
            System.out.println( "Unable to get data from remote system.  Returned tatus code: 500" ) ;
        }

        // Everything OK, parse HTML, find keys and add URLs
        else if (stat == HTTPClient.StatusCode.OK) {
            String htmlBody = client.getResponse().getResponseBody() ;
            FileOutputStream fop = null;
            File file;

            try {

                String fileName = null ;

                if (hostURL.contains(".html")){
                    String[] stuffs = hostURL.split("/") ;
                    fileName = stuffs[ stuffs.length - 1 ] ;
                }
                else
                    fileName = "./index.html" ;

                file = new File(fileName);
                fop = new FileOutputStream(file);

                // if file doesnt exists, then create it
                if (!file.exists()) {
                    file.createNewFile();
                }

                // get the content in bytes
                byte[] contentInBytes = htmlBody.getBytes();

                fop.write(contentInBytes);
                fop.flush();
                fop.close();

                System.out.println("Done");

            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (fop != null) {
                        fop.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        else {
            System.out.println("Unknown Status Code");
        }

	}


	public static void main(String args[]){

		RawHTTPGet crawler = new RawHTTPGet( args[0] ) ;
		crawler.downloadPage();
		
	}
}
