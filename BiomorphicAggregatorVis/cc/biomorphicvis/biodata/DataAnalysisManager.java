/**
 * 
 */
package cc.biomorphicvis.biodata;

import java.util.HashMap;
import JSci.maths.ArrayMath;

/**
 * @author carlos
 *
 */
public class DataAnalysisManager implements Runnable
{
	private static HashMap<String, HashMap<String, Object>> analyzedData = new HashMap<String, HashMap<String, Object>>();
	//private static HashMap<String, Object> rawData = new HashMap<String, Object>();
	private Thread runner;
	private boolean running;
	private static final double stdDevThresh = 5.0;
	
	// constructor
	public DataAnalysisManager() {
		running = false;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	public void run()
	{
		// pulse vars
		int[] hrArr = new int[30];
		int currHr = 0;
		boolean hrflag = false;
		StopWatch timeSw = new StopWatch();

		// resp vars
		int[] respArr = new int[30];
		double[] respAmpArr = new double[30];
		int currResp = 0;
		boolean respflag = false;
		
		while(runner != null) { //while the thread is running
            // ---- set-up the data structures for raw data ---- //
           // rawData.put("pulse", new Integer(LabPro.getHeartRate()));
           // rawData.put("respiration_rate", new Integer(LabPro.getRespirationRate()));
           // rawData.put("respiration_amplitude", new Double(LabPro.getRespiration()));
            
            
            /*
            // ---- set-up the data structures for analyzed data ---- //
            // --- 30 secs of data will be gathered for analysis --- //
            */
            
            // --- pulse --- //
            // pulse data structure
            HashMap<String, Object> pulse = new HashMap<String, Object>();
            hrArr[currHr] = LabPro.getHeartRate();
            currHr++;
            if(currHr >= 30) { // if roughly 30 secs have passed
            	currHr = 0; // reset so oldest array items get replaced first
            	hrflag = true;
            }
                
            if(hrflag) {
            	// perform std deviation
            	double stddev = ArrayMath.standardDeviation(hrArr);
            	pulse.put("stddev", new Double(stddev));
                System.out.println("pulse stddev:"+stddev);
            	// std dev time over the threshold
            	if(stddev < stdDevThresh) {
            		if(!timeSw.isRunning()) {
            			timeSw.start();
            		}
            		pulse.put("time", new Long(timeSw.getElapsedTime()));
            		System.out.println("time:"+timeSw.getElapsedTime());
            	} else {
            		timeSw.stop();
            		pulse.put("time", new Long(0));
            		System.out.println("time:0");
            	}
            	// add pulse stddev to the analyzed data HashMap
            	analyzedData.put("pulse", new HashMap<String, Object>(pulse));
            }
    		
            // --- respiration --- //
            // respiration data structure
    		HashMap<String, Object> resp = new HashMap<String, Object>();
    		respArr[currResp] = LabPro.getRespirationRate();
    		respAmpArr[currResp] = LabPro.getRespiration();
    		currResp++;
            if(currResp >= 30) {// roughly 30 secs
            	currResp = 0; // reset so oldest array items get replaced first
            	respflag = true;
            }
                
            if(respflag) {
            	// perform std deviation
            	double stddev_rate = ArrayMath.standardDeviation(respArr);
            	double stddev_amp = ArrayMath.standardDeviation(respAmpArr);
            	resp.put("stddev_rate", new Double(stddev_rate));
            	resp.put("stddev_amplitude", new Double(stddev_amp));
                
            	System.out.println("resp stddev_rate:"+stddev_rate);
            	System.out.println("stddev_amplitude:"+stddev_amp);
            	// add respiration stddev to the analyzed data HashMap
            	analyzedData.put("respiration", new HashMap<String, Object>(resp));
            }
    		
            // --- correlation --- //
            // correlation data structure
    		HashMap<String, Object> corr = new HashMap<String, Object>();
    		double correlation;
    		if(hrflag && respflag) { // both arrays are filled / roughly 30 secs
    			correlation = ArrayMath.correlation(hrArr, respArr);
    			corr.put("correlation", new Double(correlation));
    			
                // add correlation to the analyzed data HashMap
        		analyzedData.put("correlation", new HashMap<String, Object>(corr));
    		}
    		
    		
    		// sleep for 1 sec
    		try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {}
		} // end while loop
	} // end run() method
	
	// re-initializes all of the data structures to 0
	public static void resetData() {
        // ---- initialize the data structures for raw data ---- //
        //rawData.put("pulse", new Integer(10));
        //rawData.put("respiration_rate", new Integer(1));
        //rawData.put("respiration_amplitude", new Double(0.44));
        
		// ---- initialize the data structures for analyzed data ---- //
        // --- pulse --- //
        // pulse data structure
        HashMap<String, Object> pulse = new HashMap<String, Object>();
        pulse.put("stddev", new Double(1.101));
        pulse.put("time", new Long(0));
        analyzedData.put("pulse", new HashMap<String, Object>(pulse));
        // --- respiration --- //
        // respiration data structure
		HashMap<String, Object> resp = new HashMap<String, Object>();
    	resp.put("stddev_rate", new Double(0.901));
    	resp.put("stddev_amplitude", new Double(0.001));
    	analyzedData.put("respiration", new HashMap<String, Object>(resp));
        // --- correlation --- //
        // correlation data structure
		HashMap<String, Object> corr = new HashMap<String, Object>();
		corr.put("correlation", new Double(0.0));
		analyzedData.put("correlation", new HashMap<String, Object>(corr));
	}
	
	public void start() {
		// start thread for checking data rate
        runner = new Thread(this);
        runner.start();
        running = true;
        System.out.println("*** DataAnalysisManager thread " + this + " started. ***");
	}
	
    public void stop() {
    	if(runner != null) {
    		if(runner != Thread.currentThread())
                    runner.interrupt();
                runner = null;
                running = false;
                System.out.println("*** DataAnalysisManager thread " + this + " stopped. ***");
    	}
    }
	
    public boolean isRunning() {
    	return running;
    }
    
	// returns the curent data analysis numbers
	public static HashMap<String, HashMap<String, Object>> getAnalyzedData()
	{
		return analyzedData;
	}
	
	// returns the curent raw sensor data
	//public static HashMap<String, Object> getRawData()
	//{
	//	return rawData;
	//}
}
