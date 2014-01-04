//    ******************************************
//    *                                        *
//    *  JAVA source code for Vernier LabPro   *
//    *  data collection, single sensor mode   *
//    *                                        *
//    *  communicationAPI required             *
//	  *  real-time data collection			   *
//    *  binary data (serial port)             *
//    *                                        *
//	  *  based on code by Jochen Viehoff   	   *
//	  *  http://java.khm.de/                   *
//	  *										   *
//    ******************************************

// -----------------------------------------------

//    ******************************************
//    * Sensor: LabPro 2 analog channels       *
//    *                                        *
//    * class:    LabPro                       *
//    *                                        *
//    * methods:  (int) getHeartRate()         *
//    *           (int) getRespirationRate     *
//    *           (double) getRespiration()    *
//    *                                        *
//    * parameter: sampleRate                  *
//    *            threadDelay                 *
//    *                                        *
//    ******************************************

package cc.biomorphicvis.biodata;

import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;
import gnu.io.UnsupportedCommOperationException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.ListIterator;
import java.util.TooManyListenersException;

import cc.averages.ExponentialMovingAverage;



/**
 * @author carlos
 *
 */
public class LabPro implements Runnable, SerialPortEventListener
{
    
    static Enumeration        portList;       // list for connected ports
    static CommPortIdentifier portId;
    
    static float sampleRate  = 0.02f;         // sample rate for real-time data collection
    
    int			  threadDelay;        		  // thread delay
    int           count;
    static int    raw_heart,raw_resp;//,raw3,raw4;        // raw binary data
    static double ch1,ch2;//,ch3,ch4;            // channel data
    //static double beat;                       // heart beat
    //static double breath;					  // breath
    static int respRate = 1;				  // respiration rate (defaults to 5)
    static int heartRate = 10;				  // heart rate (defaults to 10)
    
    final static double k0 = 0.0;					  // gauge parameters
    final static double k1 = 1.0/65536.0;
    final static double k1_hr = 0.00000762939; //17-bit
    
    static String message0;                   // commands for LabPro setup
    static String message1;
    //static String message2;
    //static String message3;
    static String message4;
    static String messageStart;
    static String messageReset;
    static String messageWake;
    //static String message1A;
    
    static SerialPort   serialPort;
    static OutputStream outputStream;
    static InputStream  inputStream;
    static boolean portOpen;	// status of the port
    
    static boolean userConnected = false;
    private static ArrayList listeners = new ArrayList(); // connection listeners
    
    private Thread readData;
    private boolean running;
    
    private static ExponentialMovingAverage emaRespRate = new ExponentialMovingAverage(0.5f);
    private static ExponentialMovingAverage emaHeartRate = new ExponentialMovingAverage(0.5f);
    
    //    ******************************************
    //      constructors
    //    ******************************************
    
    public LabPro(int delay) {
    	this.threadDelay = delay;
    	running = false;
    }
    
    public LabPro() {
    	this(10000); // default thread delay of 10 secs
    }
    
    //    ******************************************
    //      public methods
    //    ******************************************
    private static double getChannel1() {
        // send back channel 1 data (heart rate)
        ch1 = (double)((k1_hr*raw_heart)+k0);
        //System.out.println("Beat:" + ch1);
        return ch1;
    }
    
    // returns a double from 0.0 to 1.0
    // corresponding to 0 - 65535 (16-bit)
    private static double getChannel2() {
        // send back channel 2 data (respiration)
        ch2 = (double)((k1*raw_resp)+k0);
        return ch2;
    }
    
    /*
    public static double getChannel3() {
        
        // send back channel 3 data
        ch3 = (double)((k1*raw3)+k0);
        return ch3;
    }
    
    public static double getChannel4() {
        
        // send back channel 4 data
        ch4 = (double)((k1*raw4)+k0);
        return ch4;
    }
    */

    
    // --- Heart rate --- //
    /**
     * return heart rate in bpm
     * @return heart rate in bpm
     */
    public static int getHeartRate() {
        // send back heartRate in bpm
    	//double hz = getChannel1() / sampleRate;
    	//System.out.println("Heart Rate:" + hz);
    	//return (int) (hz * 60);
    	//System.out.println("Heart Rate:" + heartRate);
    	return heartRate;
    }
    
    // --- Respiration rate --- //
    /**
     * return resp rate in breaths per min
     * @return resp rate in breaths per min
     */
    public static int getRespirationRate() {
        // send back respiration rate in breaths per min
    	//double hz = getBreath() / sampleRate;
    	//return (int) (hz * 60);
    	//System.out.println("Respiration Rate:" + respRate);
    	return respRate;
    }
    
    /*
    private static double getBreath() {
    	double resp = getChannel2();

        breath = (double)((k1_hr*raw_resp)+k0);
        return breath;
    }
    */
    
    public static double getRespiration() {
    	//System.out.println("Respiration:" + getChannel2());
    	return getChannel2();
    }
    
    
    //    ******************************************
    //      public thread-specific methods
    //    ******************************************
    public void start() {
    	// start thread for checking data rate
        readData = new Thread(this);
        //readData.setPriority(Thread.MAX_PRIORITY);
        readData.start();
        running = true;
        DataAnalysisManager.resetData(); // zero out the analyzed data
        System.out.println("*** LabPro thread " + this + " started. ***");
    }
    
    public void stop() {
    	if(readData != null) {
    		if(readData != Thread.currentThread())
                    readData.interrupt();
                readData = null;
                running = false;
                System.out.println("*** LabPro thread " + this + " stopped. ***");
    	}
    }
    
    public void run() {
        
        // wake and reset LabPro
    	messageWake = "s\n";
        messageReset = "s{0}\n";
        System.out.println("Reset LabPro");
        
        // build commands for channel set up
        message0 = "s{1,1,1,0,0,1}\n";
        message1 = "s{1,2,1,0,0,1}\n";
        //message1A = "s{10,2,1,60,70,0.0001}"; 
        //message2 = "s{1,3,1,0,0,1}\n";
       // message3 = "s{1,4,1,0,0,1}\n";
        message4 = "s{4,0,-1}\n";            //request binary data !!!!
        
        // start LabPro: real time sampling
        messageStart = "s{3,"+sampleRate+",-1}\n";
        System.out.println("start real time sampling: t="+sampleRate);
        
        // send commands to LabPro
        try {
        	outputStream.write(messageWake.getBytes());
            outputStream.write(messageReset.getBytes());
            outputStream.write(message0.getBytes());
            outputStream.write(message1.getBytes());
            //outputStream.write(message2.getBytes());
            //outputStream.write(message3.getBytes());
            outputStream.write(message4.getBytes());
            outputStream.write(messageStart.getBytes());
        } catch (IOException e) {}
        
        //  check sampling rate
        while (readData != null) {
            try {
                Thread.sleep(threadDelay);
            } catch (InterruptedException e) {}
            
            // if user is connected check for disconnection
            // else check for connection
            if(userConnected) {
            	if(heartRate <= 10 || respRate <= -1) {
            		userConnected = false;
            		notifyLabProConnectionListeners();
            		System.out.println("*** User disconnected from LabPro ***");
            	}
            } else {
            	if(heartRate > 10 || respRate > 1) {
            		userConnected = true;
            		notifyLabProConnectionListeners();
            		System.out.println("*** LabPro connection established. ***");
            	}
            }

            System.out.println("Heart Rate:"+getHeartRate());
            System.out.println("Respiration Rate:"+getRespirationRate());
            System.out.println("Respiration:"+getRespiration());
            System.out.println(count);
            count=0;
        }
        
        //  close port if thread has died
        serialPort.close();
        
        running = false;
    }
    
    public boolean isRunning() {
    	return running;
    }
    
    
    //    ******************************************
    //      private calculation methods
    //    ******************************************
    
    // --- heart rate --- //
    private static int hrcount = 0;
    private static double[] hrReadings = new double[3000]; // 60 secs
    private static boolean hrflag=false;
    private static void setHeartRate() {
    	double reading = getChannel1();
    	hrReadings[hrcount] = reading;
    	hrcount++;
    	
    	// if the array is filled up
    	if(hrcount >= hrReadings.length) {
    		hrcount = 0;
    		hrflag = true; // can now calculate
    	}
    	
    	if(hrflag)
    		heartRate = calculateHeartRate(hrReadings);
    }
    
	private static double prevPulseReading = 0.0;
	private static final double pulseThreshold = 0.03;
    private static int calculateHeartRate(double[] readings) {
    	double peak = 0.0;
    	int numPeaks = 0;
    	
    	for(int i = 0; i < readings.length; i++) {
    		if(readings[i] >= pulseThreshold) {
    			if(readings[i] >= prevPulseReading)
    				peak = readings[i];
    		} else {
    			if(peak >= pulseThreshold) {
    				numPeaks++;
    			}
    			peak = 0.0;
    		}
    		prevPulseReading = readings[i];
    	}
    	//System.out.println("numPeaks:"+ numPeaks);
    	double avg = emaHeartRate.update((double)numPeaks);
    	int avgint = Math.round((float)avg); //1 minute
    	return avgint;
    }
    
    //  --- respiration rate --- //
    private static int respcount = 0;
    private static double[] respReadings = new double[3000]; // 60 secs
	private static boolean respflag=false;
    private static void setRespirationRate() {
    	//System.out.println("respcount:"+respcount);
    	double reading = getChannel2();
    	respReadings[respcount] = reading;
    	respcount++;
    	
    	// if the array is filled up
    	if(respcount >= respReadings.length) {
    		respcount = 0;
    		respflag = true; // can now calculate
    	}
    	
    	if(respflag)
    		respRate = calculateRespirationRate(respReadings);
    }
    
    private static double prevRespReading = 0.0;
    private static final double respThreshold = 0.3;
    private static int calculateRespirationRate(double[] readings) {
    	double peak = 0.0;
    	int numPeaks = 0;
    	
    	for(int i = 0; i < readings.length; i++) {
    		if(readings[i] >= respThreshold) {
    			if(readings[i] >= prevRespReading)
    				peak = readings[i];
    		} else {
    			if(peak >= respThreshold) {
    				numPeaks++;
    			}
    			peak = 0.0;
    		}
    		prevRespReading = readings[i];
    	}
    	//System.out.println("numPeaks:"+ numPeaks);
    	double avg = emaRespRate.update((double)numPeaks);
    	int avgint = Math.round((float)avg); //1 minute
    	return avgint;
    }
    
    
    //    ******************************************
    //      SerialEvent handling
    //    ******************************************
    
    public void serialEvent(SerialPortEvent e) {
        
        switch (e.getEventType()) {
            
            case SerialPortEvent.OUTPUT_BUFFER_EMPTY:
                break;
            case SerialPortEvent.DATA_AVAILABLE:
                
                final int BUFFSIZE=6;
                int lenserial=0;
                byte bufserial [] = new byte[BUFFSIZE];
                
                // Read binary data until -1 is returned
                try {
                    while (inputStream.available() > 0) {
                        lenserial = inputStream.read(bufserial);
                         if (lenserial==6){
                            raw_heart = ((0xff & bufserial[0])<<8) | ((0xff & bufserial[1])); // heart rate
                            raw_resp = ((0xff & bufserial[2])<<8) | ((0xff & bufserial[3])); // respiration
                            //raw3 = ((0xff & bufserial[4])<<8) | ((0xff & bufserial[5]));
                            //raw4 = ((0xff & bufserial[6])<<8) | ((0xff & bufserial[7]));
                            setRespirationRate();
                            setHeartRate();
                            count++;
                            //System.out.println("LabPro: data received...");
                         }
                    }
                }
                catch (IOException ex) {
                    System.err.println(ex);
                    return;
                }
                break;
                
                // If break event append BREAK RECEIVED message.
                
            case SerialPortEvent.BI:
                System.out.println("\n--- BREAK RECEIVED ---\n");
        }
    }
  
	/**
	 * @return an Arraylist of the serial ports 
	 */
	public ArrayList getPorts()
	{
	    // Set up an ArrayList to store the ports:
        ArrayList portsAvailable = new ArrayList();
        Enumeration portList = CommPortIdentifier.getPortIdentifiers();
        
        // count the ports:
        int numberOfPorts = 0;
        
        // print out a header for the list of ports:
        System.out.println("No.\tSerial port\t\tOwner");
        
        // print out the list of ports:
        while (portList.hasMoreElements()) {
            
            // get all the ports available:
            CommPortIdentifier portId = (CommPortIdentifier) portList.nextElement();
            
            // if they're serial ports, add them to the Arraylist
            // and print them out:
            if (portId.getPortType() == CommPortIdentifier.PORT_SERIAL) {
                portsAvailable.add(portId.getName());
                System.out.println(numberOfPorts + "\t" +portId.getName() + "\t\t"
                        + portId.getCurrentOwner());
                // increment number of ports:
                numberOfPorts++;
            }
        }
        // return the ArrayList:
        return portsAvailable;
    }

    /**
     * Opens a serial port with default data bits (8), stop bits(1)
     * and parity (none)
     * @param whichPort the serial port to open
     * @return true if a port was successfully opened, false otherwise
     */ 
    public boolean openPort(String whichPort)
    {
        try {
            //find the port
            CommPortIdentifier portId = CommPortIdentifier.getPortIdentifier(whichPort);
            //try to open the port
            serialPort = (SerialPort) portId.open("LabPro" + whichPort, 2000);
            System.out.println("open port: done\n");
            //configure the port
            try {
                serialPort.setSerialPortParams(38400,
                        SerialPort.DATABITS_8, SerialPort.STOPBITS_1,
                        SerialPort.PARITY_NONE);
            } catch (UnsupportedCommOperationException e) {
                System.out.println("Comm parameter error: Probably an unsupported speed");
            }
            //establish streams for reading and writing to the port
            try {
                inputStream = serialPort.getInputStream();
                outputStream = serialPort.getOutputStream();
                System.out.println("Input and output streams established for serial I/O");
            } catch (IOException e) {
                System.out.println("couldn't establish streams for serial I/O");
            }
            // add a listener to trap serial events:
            try {
                serialPort.addEventListener(this);
                serialPort.notifyOnDataAvailable(true);
                System.out.println("LabPro: listening for serial events at a " +
                		"baud rate of " + serialPort.getBaudRate() +  "...");
            } catch (TooManyListenersException e) {
                System.out.println("couldn't add serial I/O listener");
            }

            // port successfully opened; set portOpen to true
            portOpen = true;
            return portOpen;
            
        } catch (Exception e) {
            // if we couldn't open the port, assume it's in use:
            System.out.println("Port in use or does not exist");
            System.out.println(e.getMessage());
            e.printStackTrace();
            return portOpen;
        }
    }
    
	/**
	 * Closes the port and streams, and disposes
	 * @return true if the port was closed.
	 *  
	 */
	public boolean closePort()
	{
		inputStream  = null;
		outputStream  = null;
	    serialPort.close();
	    portOpen = false;
	    return !portOpen;
	}
	
	/**
     * @return a boolean indicating whether or not the port is open.
     */
    public boolean isPortOpen()
    {
        return portOpen;
    }

    /**
     * adds a listener to trap serial data events from this SerialManagaer
     */
    public void addLabProConnectionListener(LabProConnectionListener lpcl)
    {
        if(lpcl != null && listeners.indexOf(lpcl) == -1) {
            listeners.add(lpcl);
            System.out.println("[+ LabProConnectionListener] " + lpcl);
        }
    }

    /**  
     * removes a listener from this SerialManagaer
     */
    public void removeLabProConnectionListener(LabProConnectionListener lpcl)
    {
        if(listeners.contains(lpcl)) {
            listeners.remove(listeners.indexOf(lpcl));
            System.out.println("[- LabProConnectionListener] " + lpcl);
        }
    }
    
	/**
	 * let everyone know a serial data event was received
	 */
	private void notifyLabProConnectionListeners()
	{
	    if(listeners == null) {
	        return;
        } else {
            ListIterator iter = listeners.listIterator();
            while(iter.hasNext()) {
                	((LabProConnectionListener) iter.next()).labProConnectionEvent(userConnected);
            	}
        }
    } 
}
