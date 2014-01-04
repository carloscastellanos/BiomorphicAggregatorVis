/**
 * This code is based off of Cristian Gramada's
 * Amazon.com tags 3D cloud
 */
package cc.biomorphicvis;


import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import processing.core.PApplet;
import processing.core.PFont;
import processing.core.PImage;
import cc.biomorphicvis.biodata.DataAnalysisManager;
import cc.biomorphicvis.biodata.LabPro;
import cc.biomorphicvis.biodata.StopWatch;
import cc.biomorphicvis.biodata.LabProConnectionListener;
import cc.biomorphicvis.data.OSCHandler;
import de.sciss.net.OSCBundle;
import de.sciss.net.OSCMessage;

/**
 * @author carlos
 *
 */
public class BAVis extends PApplet implements LabProConnectionListener
{
	static final long serialVersionUID = 8077109551486196568L;
	private BAVisImage[] baImages;
	private BAVisImage baVisRunningImg;
	//PImage[] pImages;
	private ArrayList<HashMap<String,String>> data;
	private ArrayList<HashMap<String,Object>> imgData;
	private float x;
	private float y;
	private int range;
	private int incr_update;
	private float scaleVal;
	private float ax;
	private float ay;
	private int numImgs;
	private int TOTAL_IMGS;
	private int VISIBLE;
	//private int centerX; // center of the screen (x coord)
	//private int centerY; // center of the screen (y coord)
	private PFont font;
	private boolean connected; // is user hooked up?
	private boolean running;
	private static final long analyzeTime = 60000; // 60 secs
	private static final long imageTime = 30000; // 30 secs
	private static final long pulseTimeThreshold = 120000; // 2 mins
	private double pulseOpac;
	private static final String path = "downloads/";
	//private float scaleDivisor = 20.0f;
	private boolean faded = false;
	private int currVisImage;
	//private boolean imgChanged = true;
	private StopWatch imgStopWatch;
	private StopWatch runningStopWatch;
	private static DataAnalysisManager dataAnalysis = new DataAnalysisManager();
	// --------- OSC stuff ----------- //
    private static final String respRateAddr = "/respRate";
    private static final String respAmpAddr = "/respAmp";
    private static final String pulseAddr = "/pulse";
    //private static final String gsrAddr = "/gsr";
    private static final String oscHost = "127.0.0.1";
    private OSCHandler oscHandler;
    
	public BAVis(ArrayList<HashMap<String,String>> a) {
		this.data = new ArrayList<HashMap<String,String>>(a);
		Collections.shuffle(this.data); //randomize the list
		this.imgData = new ArrayList<HashMap<String,Object>>(this.data.size());
	}
	
	public void setup() {
		//the amount of memory allocated so far (usually the -Xms setting)
		//long allocated = Runtime.getRuntime().totalMemory();
		//System.out.println("allocated: " + allocated);
		
		//size(screen.width, screen.height, OPENGL);
		size(800, 600, OPENGL);
		frameRate(30);
		background(0);
	  
		x = width/2;
		y = height/2;
		//pImages = new PImage[TOTAL_IMGS];
		range = 48;
		incr_update = 40;
		scaleVal = 1.0f;
		connected = false;
		running = false; // boolean to test whether the single image stage has been reached
		currVisImage = 0;
		pulseOpac = 255.0;
		//centerX = screen.width/2;
		//centerY = screen.height/2;
		imgStopWatch = new StopWatch(); // timer for images
		runningStopWatch = new StopWatch(); // timer for running time
		font = loadFont("HelveticaCY-Bold-48.vlw");

		// perspective
		float fov = PI/1.8f;
		float aspect = width / height;
		float znear = 200.0f;
		float zfar = -5000.0f;
		perspective(fov, aspect, znear, zfar);

		for(int i=0; i<data.size(); i++) {
			//pImages[i] = loadImage("http://ccastellanos.com/china/china-Thumbnails/" + i + ".jpg");
			HashMap<String,String> hash = data.get(i);
			String imgUrl = hash.get("imageUrl");
			String title = hash.get("title");
			String searchTerm = hash.get("searchTerm");
			String fileName = hash.get("fileName");
			PImage pimg;
			try {
				pimg = loadImage(path+fileName);
				// make sure te image isn't too big
				if (pimg.width > 1024 || pimg.height > 768)
					pimg = null;
			} catch(Exception e) {
				pimg = null;
			}
			if(pimg != null) {
				pimg.format=RGB;
				HashMap<String,Object> newHash= new HashMap<String, Object>(5);
				newHash.put("imageUrl", imgUrl);
				newHash.put("title", title);
				newHash.put("searchTerm", searchTerm);
				newHash.put("fileName", fileName);
				newHash.put("image", pimg);
				imgData.add(new HashMap<String, Object>(newHash));
			}
		}

		// Free memory out of the amount allocated (value above minus used)
		//long free = Runtime.getRuntime().freeMemory();
		//System.out.println("free:"+free);
		
		TOTAL_IMGS = imgData.size();
		System.out.println("TOTAL_IMGS="+TOTAL_IMGS);
		VISIBLE = TOTAL_IMGS-10;
		if(VISIBLE < 10) {
			System.out.println("Too few images!");
			System.exit(2);
		}
		numImgs = TOTAL_IMGS/2-1;
		baImages = new BAVisImage[VISIBLE];
	  
		for (int i = 0; i < VISIBLE; i++) {
			HashMap<String,Object> hash = imgData.get(i);
			String imgUrl = (String)hash.get("imageUrl");
			String title = (String)hash.get("title");
			String searchTerm = (String)hash.get("searchTerm");
			PImage pimg = (PImage)hash.get("image");
		  
			//Float fl = new Float(random(numImgs));
			// int r = fl.intValue();
			//int count = r*2;
			int count = 0;
			baImages[i] = new BAVisImage(pimg, imgUrl, title, searchTerm, count);
		}
	  
		// set up running image class
		HashMap<String,Object> hash = imgData.get(currVisImage);
		String imgUrl = (String)hash.get("imageUrl");
		String title = (String)hash.get("title");
		String searchTerm = (String)hash.get("searchTerm");
		PImage pimg = ((PImage)hash.get("image"));
		baVisRunningImg = new BAVisImage(pimg, imgUrl, title, searchTerm, 0);
	  
		// Free memory out of the amount allocated (value above minus used)
		//long free2 = Runtime.getRuntime().freeMemory();
		//System.out.println("free:"+free2);
		
		// OSC
		// start the OSCHandler thread
		oscHandler = new OSCHandler(oscHost);
		oscHandler.setDaemon(true);
		oscHandler.start();
	} // end setup
	
	public void draw() {
		background(0);
		//fill(204, 80);
		//stroke(255);
		//rect(-300, height+300, screen.width+400, 60);
		//textFont(font, 54);
		//fill(204, 0, 0, 100);
		//text("El Papi", -185, height+350, 200);
	  
		// loops while its in "kiosk" mode
		// or user is hooked up for less than
		// analyzeTime
		if(!running) {
			for(int i=0; i<VISIBLE; i++) {
				// Brownian Motion
				ax = random(-range, range);
				ay = random(-range, range);
				// check to see if user is connected
				// then check to see how long s/he has been connected
				if(connected) {
					// started fading after the alloted time
					if(runningStopWatch.getElapsedTime() > analyzeTime) {
						if(faded) {
							baImages[i].scaling = false;
							running = true;
							imgStopWatch.start(); //start the timer for the images
							baImages = null; // kill the default/initial images
							perspective(); // reset the perspective
							break;
						} else {
							baImages[i].fade();
							baImages[i].display(ax, ay);
							baImages[i].update();
						}
					} else {
						if(baImages[i].scaling != true)
							baImages[i].scaling = true;
						updateAnalyzing();
						baImages[i].display(ax, ay);
						baImages[i].update();
					}
				} else {
					// "attractor loop"
					baImages[i].display(ax, ay);
					baImages[i].update();
				}
			} // end for loop
		} else {
			if(currVisImage < imgData.size()) {
				baVisRunningImg.displayRunning();
				baVisRunningImg.checkImageTime();
			}
			// OSC
			sendOSC();
		}
		//long allocated = Runtime.getRuntime().totalMemory();
		//System.out.println("allocated: " + allocated);
		//long free2 = Runtime.getRuntime().freeMemory();
		//System.out.println("free:"+free2);
	}
	
	//void displayAnalyzing() {
	//	
	//}
	
	// this is called when the paticiapnt first hooks up
	void updateAnalyzing() {
		// get the current analyzed data
		HashMap<String, HashMap<String, Object>> analyzedData = DataAnalysisManager.getAnalyzedData();
		
		//respiration
		HashMap<String, Object> respHash = analyzedData.get("respiration");
		double respRateSD = ((Double)respHash.get("stddev_rate")).doubleValue();
		double respAmpSD = ((Double)respHash.get("stddev_amplitude")).doubleValue();

		//System.out.println("***Analyzing "+runningStopWatch.getElapsedTimeSecs()+ "...***");
		//System.out.println("respRateSD:"+respRateSD);

		//--------------------------------------------//
		// this is where the speed and "chaos" of the 
		// visualization is affected
		//--------------------------------------------//
		// incr_update
		incr_update = (int)(respRateSD * 10); // speed
		// range
		range = (int)(respAmpSD * 1000); // brownian motion
		
		//incr_update = 10;
		//range=2;
		
		// scale images based on breathing (amplitude)
		double respAmp = LabPro.getRespiration();
		// map 0.43 - 0.47 -->  0 - 10
		double x = (respAmp*100) - 43.0;
		Double finRespAmp = new Double((x/4.0) * 10.0);
		scaleVal = finRespAmp.floatValue();
		
		// OSC
		sendOSC();
	}
	
	// messgae that signals a connection
	// or a disconnection from the LabPro
	public void labProConnectionEvent(boolean connected) {
		this.connected = connected;
		
		// if user is connected
		if(this.connected) {
			// start the data analysis thread if it's
			// not already running
			if(!dataAnalysis.isRunning()) {
				dataAnalysis.start();
				runningStopWatch.start();
			}
		} else {
			// user is not connected
			// stop the data analysis thread if it's running
			if(dataAnalysis.isRunning()) {
				dataAnalysis.stop();
				runningStopWatch.stop();
			}
		}
	}
	
	/*
	 *========================================
	 * Inner class that holds an image object
	 *========================================
	 */
	class BAVisImage
	{
		float m_x, m_y, m_z;
		float m_kz;
		int m_count;
		float prev_x, prev_y;
		int urange;
		PImage m_img;
		String imgUrl;
		String imgTitle;
		String searchTerm;
		int opacity;

		boolean scaling;
		
		BAVisImage(PImage img, String url, String title, String term, int cnt) {
			m_img = img;
			imgUrl = url;
			imgTitle = title;
			searchTerm = term;
			m_count = cnt;
			    
			m_x = random(1000) - 1000;
			m_y = random(1000) - 1000;
			m_z = -random(5000);
			    
			prev_x = 0.0f;
			prev_y = 0.0f;
			
			opacity = 255;
			
			scaling = false;
		}
		
		// 	default display (when no user is hooked up, like attractor loop in a kiosk)
		void display(float aax, float aay) {
			pushMatrix();
			translate(width - x + m_x, height - y + m_y, m_z);
			//scale(m_count/scaleDivisor, m_count/scaleDivisor, 1);
			if(opacity < 255)
				tint(255,opacity);
			if(scaling) {
				scale(scaleVal);
				displayAnalyzingText(prev_x, prev_y);
			}
			image(m_img, prev_x, prev_y);
			popMatrix();
			
			prev_x = prev_x+aax;
			prev_y = prev_y+aay;
		}
		
		void update() {
			m_z += incr_update;
			if (m_z > 300) {
				m_x = random(-range, range);
				m_y = random(-range, range);
				m_z = -random(5000) - 500; // -random(1000) - 5000;
				Float fl = new Float(random(numImgs));
				int r = fl.intValue();
			    //m_count = r*2;
			    m_count = 0;
			    //update image and its associated terms
			    HashMap<String, Object> hash = imgData.get(r*2+1);
			    PImage pimg = (PImage)hash.get("image");
				String imgUrl = (String)hash.get("imageUrl");
				String title = (String)hash.get("title");
				String searchTerm = (String)hash.get("searchTerm");
				this.imgUrl = imgUrl;
				this.imgTitle = title;
				this.searchTerm = searchTerm;
				this.m_img = pimg;
			    //m_img = pImages[r*2+1];
			}
			m_count++;
		}
		
		void fade() {
			if(opacity > 0) {
				opacity--;
			} else {
				faded = true;
				// move image one out of way
				image(m_img, screen.width+1000, screen.height+1000);
			}
		}
		
		void displayRunning() {
			// text
			displayText();
			
			// get the current analyzed data
			HashMap<String, HashMap<String, Object>> analyzedData = DataAnalysisManager.getAnalyzedData();
			// get the current raw data
			//HashMap<String, Object> rawData = DataAnalysisManager.getRawData();
			
			//System.out.println("***Running..***");
			
			//pulse/heart rate
			HashMap<String, Object> pulseHash = analyzedData.get("pulse");
			//double pulseSD = ((Double)pulseHash.get("stddev")).doubleValue();
			long pulseTime = ((Long)pulseHash.get("time")).longValue();
			// if the standard dev of heart rate has been below a certain 
			// number for a certain amount of time, reduce the opacity
			if(pulseTime > pulseTimeThreshold)
				pulseOpac = pulseOpac - 0.01;
			else
				pulseOpac = 255.0;
			// --- set the opacity based on this time --- //
			//setOpac((int)pulseOpac);
			
			//respiration
			//HashMap respHash = analyzedData.get("respiration");
			double respAmp = LabPro.getRespiration();
			
			// --------------------------------------------------- //
			// mosaic filter based on resp amplitude (breathing)   //
			// --------------------------------------------------- //
			// map 0.43 - 0.47 -->  0 - 20
			double x = (respAmp*100) - 43.0;
			Double finRespAmp = new Double((x/4.0) * 20.0);
			int blockSize = Math.round(finRespAmp.floatValue());
			// do mosaic
			mosaic(blockSize);
			/*
			//m_img = Mosaic.mosaic(m_img.get(), blockSize);
			int imgX = width/2 - m_img.width/2;
			int imgY = height/2 - m_img.height/2;
			scale(2.0f);
			image(m_img, imgX, imgY);
			*/
			
			//0-1 20-100; scale images
			//Double d3 = new Double(respAmp);
			//scaleDivisor = d3.floatValue() * 100.0f + 10.0f;
			
			// correlation between heart rate and respiration
			// -------------------------------------------------- //
			// scale image accroding to the amount of correlation //
			// -------------------------------------------------- //
			//HashMap correlationHash = analyzedData.get("correlation");
			//double correlation = ((Double)correlationHash.get("correaltion")).doubleValue();
			
			//gsr
			//HashMap<String, Object> gsrHash = analyzedData.get("gsr");
			//double gsrTime = ((Double)gsrHash.get("time")).doubleValue();
			//double gsrSpike = ((Double)gsrHash.get("spike")).doubleValue();
			//explode
			
			
			// ------ explode ------ //
			// respiration rate std deviation
			//HashMap<String, Object> respHash = analyzedData.get("respiration");
			//double respRateSD = ((Double)respHash.get("stddev_rate")).doubleValue();
			//explode(blockSize);
		}
		
		// cheks to see if the current image has been on screen
		// for its alloted time
		// if so it goes to the next image and resets the timer
		void checkImageTime() {
			long elapsedTime = imgStopWatch.getElapsedTime();
			if(elapsedTime > imageTime) {
				// change the image
				currVisImage++; // go to the next image
				HashMap<String,Object> h = imgData.get(currVisImage);
				String imgUrl = (String)h.get("imageUrl");
				String title = (String)h.get("title");
				String searchTerm = (String)h.get("searchTerm");
				PImage pimg = (PImage)h.get("image");
				this.imgUrl = imgUrl;
				this.imgTitle = title;
				this.searchTerm = searchTerm;
				this.m_img = pimg;
				  
				//imgChanged = true; // flag to tell us image is changing
				imgStopWatch.start(); // reset the imgStopWatch;
				int temp = currVisImage+1;
				System.out.println("Changing images... " + temp + " of " + TOTAL_IMGS);
			}
		}
		
		//float ex_incr = 0.0f;
		//float zz = 0.0f;
		void explode(int blockSize) {
			if(blockSize < 1) blockSize = 1;
			int COLS = m_img.width/blockSize;  // Calculate # of columns          
			int ROWS = m_img.height/blockSize; // Calculate # of rows
			
			// calculate how respRateStdDev affects the amount of explosion
			// 0.0 --> 1.0 = 0 --> 200
			//float zz = (float)(respRateStdDev);
			// scale images based on breathing (amplitude)
			/*
			double respAmp = LabPro.getRespiration();
			// map 0.43 - 0.46 -->  0 - 10
			double rx = (respAmp*100) - 43.0;
			Double finRespAmp = new Double((rx/3) * 10.0);
			float scaleValue = finRespAmp.floatValue();
			*/

			// Begin loop for columns
			for (int i = 0; i < COLS; i++) {
				// Begin loop for rows
				for ( int j = 0; j < ROWS;j++) {
					int xloc = i*blockSize + blockSize/2; // x position
					int yloc = j*blockSize + blockSize/2; // y position

					int loc = xloc + yloc*m_img.width; // Pixel array location
					int c = m_img.pixels[loc];       // Grab the color
					// Calculate a z position as a function pixel brightness
					//float zloc = (ex_incr / (float) m_img.width) * brightness(m_img.pixels[loc]) - zz;
					// Calculate a z position as a function of respiration
					//float zloc = (ex_incr / (float) m_img.width) * scaleValue;
					// Translate to the location, set fill and stroke, and draw the rect
					//float zloc = 0.0f;
					pushMatrix();
					//translate(xloc,yloc,zloc);
					//translate(xloc,yloc);
					fill(c, (int)pulseOpac);
					noStroke();
					//rectMode(CENTER);
					int imgX = width/2 - (m_img.width/2) - xloc;
					int imgY = height/2 - (m_img.height/2) - yloc;
					scale(2.0f);
					//rotate(PI);
					rect(imgX,imgY,blockSize,blockSize);
					popMatrix();
				}
			}
			//ex_incr += 0.0001f;
			//zz += 0.000001f;
		}

		void displayText() {
			// top strip
			/*
			noStroke();
			fill(204, 56); //light gray 1/4 opacity
			rect(0,0,width,30);

			// bottom strip
			noStroke();
			fill(204, 56); //light gray 1/4 opacity
			rect(0,height-30,width,30);
			*/
			
			textAlign(CENTER);
			
			// top text
			textFont(font, 40);
			fill(255);
			text(this.searchTerm, width/2, 45);

			// bottom text
			textFont(font, 28);
			fill(255);
			text(this.imgTitle, width/2, height-30);
		}
		
		void displayAnalyzingText(float locx, float locy) {
			//int imgw = this.m_img.width;
			int imgh = this.m_img.height;
			//float xloc = locx - (imgw/2);
			//float yloc = locy + (imgh/2) + 10;
			
			textFont(font, 24);
			fill(255);
			text(this.searchTerm, locx, locy + (imgh+25));
		}
		
		// Opacity
		void setOpac(int opac) {
			tint(255, opac);
			
			// display the image on screen
			int imgX = (width/2) - (this.m_img.width/2);
			int imgY = (width/2) - (this.m_img.height/2);
			image(m_img, imgX, imgY);
		}
		
		//METHOD MOSAIC
		void mosaic(int squareSize) {
			//capture the average colour value in the region of interest
			//draw rectangles across the screen using those averages

			//int rows = height/squareSize;
			//int columns = width/squareSize;
			//int numBlocks = rows*columns;
			
			int imgX = (width/2) - (this.m_img.width/2);
			int imgY = (height/2) - (this.m_img.height/2);
			
			if (squareSize < 1)
				squareSize = 1;

			for(int i = 0; i < (this.m_img.width - squareSize); i += squareSize) {
				for(int j = 0; j < (this.m_img.height - squareSize); j += squareSize) {
					fill(getColour(squareSize, i, j), (int)pulseOpac);
					noStroke();
					rect(i+imgX,j+imgY,squareSize, squareSize);
				}
			}
		}

		int getColour(int squareSize, int xPos, int yPos){   
			int avgRed = 0;
			int avgGreen = 0;
			int avgBlue = 0;

			//From processing.org:
			/*
			 * Colors are 32 bits of information ordered as AAAAAAAARRRRRRRRGGGGGGGGBBBBBBBB 
			 * where the A's contain the alpha value, the R's are the red/hue value, 
			 * G's are green/saturation, and B's are blue/brightness.
			*/
			for(int i = xPos; i < xPos + squareSize; i++) {
				for(int j = yPos; j < yPos + squareSize; j++) {
					// the following bitwise method of retrieving the colour channel from a color primitve
					int colour = m_img.pixels[i + j*m_img.width];
			        //taken from: http://www.processing.org/reference/rightshift.html
			        avgRed = avgRed + (colour >> 16 & 0xFF); // Bitwise right shift to get red value from the colour variable
			        avgGreen = avgGreen + (colour >> 8 & 0xFF); // Bitwise right shift to get green from the colour variable
			        avgBlue = avgBlue + (colour & 0xFF);   // Bitwise right shift to get blue
				}
			}
			return color(avgRed/(sq(squareSize)),avgGreen/(sq(squareSize)),avgBlue/(sq(squareSize)));
		}
		
	} // end class BAVisImage
	
	private void sendOSC() {
		// get the current raw data
		//HashMap<String, Object> rawData = DataAnalysisManager.getRawData();
		
		OSCBundle bundle = new OSCBundle();
		
		// respiration rate
		Object[] respRateVal = new Object[] {new Integer(LabPro.getRespirationRate())};
		bundle.addPacket(new OSCMessage(respRateAddr, respRateVal));
		// respiration amplitude
		Object[] respAmpVal = new Object[] {new Double(LabPro.getRespiration())};
		bundle.addPacket(new OSCMessage(respAmpAddr, respAmpVal));
		// heart rate
		Object[] heartRateVal = new Object[] {new Integer(LabPro.getHeartRate())};
		bundle.addPacket(new OSCMessage(pulseAddr, heartRateVal));
		// gsr
		//Object[] gsrVal = new Object[] {(Double)rawData.get("gsr")};
		//bundle.addPacket(new OSCMessage(gsrAddr, gsrVal));
		
		// send it!
		oscHandler.sendOSC(bundle);
	}
	
	/**
	 * @param args
	 */
	/*
	public static void main(String[] args) {
		//PApplet.main(new String[] { "--display=1", "--present", "cc.biomorphic.vis.DefaultVis" });
		PApplet.main(new String[] { "cc.biomorphicvis.BAVis" });
	}
	*/
	
} //end class BAVis
