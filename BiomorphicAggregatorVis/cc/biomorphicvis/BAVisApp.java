/**
 * 
 */
package cc.biomorphicvis;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.image.MemoryImageSource;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;

import cc.biomorphicvis.biodata.LabPro;
import cc.biomorphicvis.data.StoredData;

/**
 * @author carlos
 *
 */
public class BAVisApp extends Window
{
	private Frame parentFrame;
	private BAVis vis;
	private static final String dbPath = "data/biomorphic.db";
	private static final String dbDriver = "org.sqlite.JDBC";
	private String[] terms;
	private int limit;
	static LabPro labPro;
	
	public BAVisApp(Frame f, String[] terms, int limit) {
		super(f);
		parentFrame = f;
		this.terms = terms;
		this.limit = limit;
	}

    public void init()
    {
      // MS JView requires .x .y .width .height (doesn't know getWidth() etc) (thanks to Mark Napier)
     setBounds(parentFrame.getBounds().x, parentFrame.getBounds().y, 
    		  parentFrame.getBounds().width, parentFrame.getBounds().height);

      setVisible(true);
      setBackground(Color.black);
      setLayout(null);
      
      // db retrival object
      StoredData sd = new StoredData(dbPath, dbDriver);
      // retrive from db
      ArrayList<HashMap<String,String>> data = sd.retrieve(terms, limit);
      vis = new BAVis(data);
      vis.setLocation(0,0);
      add(vis);
      vis.init();
      labPro.addLabProConnectionListener(vis);
    }
    
	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		System.out.println("Welcome to Biomorphic Aggregator 0.1 alpha...\n");
		
		// ---- Serial port and Lab pro stuff ---- //
        int serialPortNum = 0;	// the serial port we'll open
        ArrayList portList;		// the list of available serial ports;
        BufferedReader inStream;
		
    	// --- LabPro --- //
        labPro = new LabPro();
        portList = labPro.getPorts();
        
        System.out.println("\nPick the number of a serial port to open.");
        
        try {
            // open input from keyboard
            inStream = new BufferedReader(new InputStreamReader(System.in));
            String inputText;
            
            // read as long as we have data at stdin
            while((inputText = inStream.readLine()) != null) {
                // if port is not open, assume user is typing a number
                // and open the corresponding port:
                if (!labPro.isPortOpen()) {
                    serialPortNum = getNumber(inputText);
                    // if serialPortNum is in the right range, open it:
                    if (serialPortNum >= 0) {
                        if (serialPortNum < portList.size()) {
                            String whichPort = (String)portList.get(serialPortNum);
                            if (labPro.openPort(whichPort)) {
                            	labPro.start(); // start the LabPro thread
                                break;
                            }
                        } else {
                            // You didn't ge a valid port:
                            System.out.println(serialPortNum + " is not a valid serial port number. Please choose again");
                        }
                    }
                } /*else {
                    // port is open:
                    // if user types +++, close the port and repeat the port selection dialog:
                    if (inputText.equals("+++")) {
                        bodyDSerial.closeSerial();
                        System.out.println("Serial port closed.");
                        portList = bodyDSerial.getSerialPorts();                  
                        System.out.println("\nPick the number of a serial port to open.");
                    }            
                }*/
            }
            // if stdin closes, close port and quit:
            //inStream.close();
            //bodyDSerial.closeSerial();
            //System.out.println("Serial port closed; thank you, have a nice day.");
            System.out.println(" ");
        } catch(IOException e) {
            System.out.println(e);
        }
        
        
        // ---- Screen stuff ---- //
        
		Dimension screenDim = Toolkit.getDefaultToolkit().getScreenSize();
		//System.setProperty("apple.laf.useScreenMenuBar", "false"); 
		GraphicsEnvironment environment = GraphicsEnvironment.getLocalGraphicsEnvironment();
	    GraphicsDevice displayDevice = environment.getDefaultScreenDevice();
	    
	    // Make the full screen frame
		Frame frame = new Frame(displayDevice.getDefaultConfiguration());
		frame.setUndecorated(true);
		frame.setBackground(Color.black);
		//frame.setBounds(0,0,(int)screenDim.width,(int)screenDim.height);
		frame.setBounds(0,0,800,600);
		// make the cursor invisible
		int[] pixels = new int[16*16];
		Image image = Toolkit.getDefaultToolkit().createImage(
		        new MemoryImageSource(16, 16, pixels, 0, 16));
		Point hotspot = new Point(0, 0);
		try {
			Method mCustomCursor =
		        Toolkit.class.getMethod("createCustomCursor",
		                                new Class[] { Image.class,
		                                              Point.class,
		                                              String.class, });
			Cursor transparentCursor =
		        (Cursor)mCustomCursor.invoke(Toolkit.getDefaultToolkit(),
		                                     new Object[] { image,
		                                                    hotspot,
		                                                    "no cursor" });
			// set the cursor
			frame.setCursor(transparentCursor);
	    } catch (NoSuchMethodError e) {
	        System.err.println("cursor() is not available " +
	                           "when using this version of Java ");
	    } catch (IndexOutOfBoundsException e) {
	        System.err.println("cursor() error: the hotspot " + hotspot +
	                           " is out of bounds for the given image.");
	    } catch (Exception e) {
	        System.err.println(e);
	    }
	    
		frame.setVisible(true);
		
		// get the search terms file
		ArrayList<String> items = new ArrayList<String>();
		// read from a text file
		//FileInputStream fin = null;
		try {
			//fin =  new FileInputStream("cc.biomorphicvis.data.terms.txt");
			//Reader in = new InputStreamReader(fin);
			BufferedReader bin = new BufferedReader(new FileReader("data/terms.txt"));
			//BufferedReader bin = new BufferedReader(in);
			System.out.println("Search terms file loaded...");
			String entry = null;
			while((entry = bin.readLine()) != null) {
				items.add(entry);
			}
		} catch(IOException e) {
			System.out.println("Exception: " + "error retrieving file " + e);
			System.exit(1);
			
		}
		
		// get random numbers (w/no repeats) to use for getting 10 unique search terms
		// String array to hold the serch terms
		/*
		int size = 10;
		String[] terms = new String[size];
		int[] randomInts = RandomNoRepeat.generate(size, items.size()-1);
		for(int i=0; i<randomInts.length; i++) {
			terms[i] = items.get(randomInts[i]);
			System.out.println("Added "+items.get(randomInts[i])+" to the search list");
		}
		*/
		
		// get all of the items
		String[] terms = new String[items.size()];
		for(int i=0; i<items.size(); i++) {
			terms[i] = items.get(i);
			System.out.println("Added "+terms[i]+" to the search list");
		}
		
		// Make the Window from frame
		BAVisApp app = new BAVisApp(frame, terms, 1); // 1 image limit (per search term)
		//displayDevice.setFullScreenWindow(app);
	    app.setVisible(true);
		app.init();
	}
	
    /**
     * @return an int from a string that's a valid number.
     **/   
    private static int getNumber(String inString) {
        int value = -1;
        try {
            value = Integer.parseInt(inString);
        } catch (NumberFormatException ne) {
            System.out.println("not a valid number");
        }
        return value;
    }
    
}
