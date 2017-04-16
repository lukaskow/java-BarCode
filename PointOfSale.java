import java.awt.*;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterJob;
import java.io.*;
import java.util.*;
import javax.comm.*;
import java.sql.*;

// program need a config file with properties of hardware 
// program use database Products located on localhost
// program was not tested 
// this program was my first experience with databases, com port and printers port in java
// I assume that if input start with character 'e' it will be 'exit'

public class PointOfSale implements Runnable, SerialPortEventListener {
	
    private static CommPortIdentifier myCommPortIdentifier;
    private static String comPort;    

    private Connection myConnection;
    private InputStream myInputStream;
    private SerialPort mySerialPort;
    private Thread myThread;
    
    //for mysql log in
    private String url = "jdbc:mysql://localhost:3306/Products";
    private String user = "PointOfSale";
    private String password = "Sale";

    //for printing
    private ArrayList<String> Recipe = new ArrayList<String>();
    private Graphics g;
    
    public PointOfSale ()
    {	
    	// port init
    	 try {
		mySerialPort = (SerialPort) myCommPortIdentifier.open("ComControl", 2000);
		} catch (PortInUseException e) {
			e.printStackTrace();
		}
    	 
    	//get code
    	 try {
		myInputStream = mySerialPort.getInputStream();
		} catch (IOException e) {
			e.printStackTrace();
		}
    	 
    	 //event listener
    	 try {
		mySerialPort.addEventListener(this);
		} catch (TooManyListenersException e) {
			e.printStackTrace();
		}
    	 mySerialPort.notifyOnDataAvailable(true);
    	 
    	 // port properties
    	 try {
		mySerialPort.setSerialPortParams(9600,
			SerialPort.DATABITS_8,
			SerialPort.STOPBITS_1,
			SerialPort.PARITY_NONE);
			mySerialPort.setDTR(false);
			mySerialPort.setRTS(false);
		} catch (UnsupportedCommOperationException e) {
			e.printStackTrace();
		}
    	 
    	//thread
         myThread = new Thread(this);
         myThread.start();
             
    }
    
    public void run() 
    {
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {}
    }
    
    // on scan
    public void serialEvent(SerialPortEvent event)
    {
        if (event.getEventType() == SerialPortEvent.DATA_AVAILABLE) 
	{
            StringBuilder myStringBuilder = new StringBuilder();
            int c;
            try {

                // append the scanned data onto a string builder
                while ((c = myInputStream.read()) != 10)
		{
                   if (c != 13)  myStringBuilder.append((char) c);
                }               
                if (myStringBuilder.charAt(0) == 'e') 
		{
                	g.setFont(new Font("TimesRoman", Font.PLAIN, 10)); 
                	int x = 5;
                	int y = 5;
                	
                	//make printable graphic from Recipe
                	for(String line:Recipe)
                	{
                		g.drawString(line,x,y);
                		y += 15;
                	}
                	
                	//set graphic to print
                	Graphics2D g2 = (Graphics2D) g;
                	PrinterJob pjob = PrinterJob.getPrinterJob();
                	PageFormat pf = pjob.defaultPage();
                        PageFormat pf2 = pjob.pageDialog(pf);
                    
                   	//printing
                	pjob.setPrintable( (Printable) g2, pf2 );
                	if (pjob.printDialog()){
                	    try {
                		pjob.print();
                	    }
                	    catch(Exception e){
                	    }
                	}
                	
                	Recipe.clear();
                }
                else
                {
	                //look for Barcode in database
	                String BarCode = myStringBuilder.toString();
	                
	                if(BarCode.isEmpty())
	                	System.out.println("INVALID BAR CODE \n");
	                else
	                {
	                	//set connection
	                	myConnection = DriverManager.getConnection(url, user, password);
	                	Statement st = myConnection.createStatement();
		                
		                //execute query
		                final ResultSet resultSet = st.executeQuery("SELECT Name,Price FROM Products WHERE Products.Code = '" + BarCode + "'");
		                
		                //test if barcode exist
		                if(resultSet.next()) 
				{
		                	System.out.println(resultSet.getString(1) + "," + resultSet.getString(2) );
		                	Recipe.add(resultSet.getString(1) + "  :  " + resultSet.getString(2));          
		                }
		                else{
		                	System.out.println("Product not found");
		                }
	                }
                }

                // close the input stream
                myInputStream.close();

            } catch (IOException e) {
                e.printStackTrace();
            } catch (SQLException e) {
				e.printStackTrace();
			}
        }
    }

	public static void main(String[] args) 
	{
	 // read properties
        Properties myProperties = new Properties();
        try {
            myProperties.load(new FileInputStream("config.properties"));
            comPort = myProperties.getProperty("ScanHandler.comPort");
        } catch (IOException e) {
            e.printStackTrace();
        }              

        try {

            // get COM port
            myCommPortIdentifier = CommPortIdentifier.getPortIdentifier(comPort);
            PointOfSale reader = new PointOfSale();

        } catch (Exception e) {
        	//if errors with port print msg
            System.out.println(comPort + " " + myCommPortIdentifier);
            System.out.println(e);
        }

	}

}
