/**
 * 
 */
package cc.biomorphicvis.filters;

import processing.core.PApplet;
import processing.core.PImage;

/**
 * @author carlos
 * background/foreground subtraction
 */

public class PixelRemover
{
	private static PImage img;
	
	public static PImage swapBackground(PImage pimg, PImage oldBg, PImage newBg, double threshold) {
		int currPixel;
		int oldPixel;
		int newPixel;
		img = pimg;
		img.format = PImage.RGB;
		img.loadPixels(); // load the pixels
		
		// loop horizontally
		for(int x=0; x<img.width; x++) {
			// loop vertically
			for(int y=0; y<img.height; y++) {
				// get the current pixel and old bg pixel
				// faster to get it right form the pixels[] array
				// than using get(x, y)
				currPixel = img.pixels[y*img.width+x];
				oldPixel = oldBg.pixels[y*oldBg.width+x];
				/*
				 * If the distance between the current pixel
				 * and the olg bg pixel color is less than
				 * the respThreshold value, swap in the new pixel
				 */
				if(colorDistance(currPixel, oldPixel) < threshold) {
					newPixel = newBg.pixels[y*newBg.width+x];
					int[] colors = getColor(newPixel);
					//create the full rgb value for color
				    // create a 32 bit int with alpha, red, green blue from left to right
					currPixel = 0xff000000 | (colors[0] << 24) | (colors[1] << 16) | (colors[2] << 8) | (colors[3]);                    
				}
			}
		}
		return newBg;
	}
	
	// basically these two medthods are forms of edge detection
	
	public static PImage removeForeground(PImage pimg, float threshold) {
		int topPixel;
		int botPixel;
		float topAvg = 0.0f;
		float botAvg = 0.0f;
		int black = 0xff000000 | (0 << 16) | (0 << 8) | 0;
		img = pimg;
		img.format = PImage.RGB;
		img.loadPixels(); // load the pixels
		
		// loop vertically
		for(int y=0; y<img.height-1; y++) {
			// loop horizontally
			for(int x=0; x<img.width; x++) {
				// get the top and bottom pixels
				// faster to get it right from the pixels[] array
				// than using get(x, y)
				topPixel = img.pixels[y*img.width+x];
				botPixel = img.pixels[(y+1)*img.width+x];
				
				// get the color averages for the two pixels
				topAvg = getAverage(topPixel);
				botAvg = getAverage(botPixel);
				
				// check if the absolute valueof the difference
				// is MORE than the respThreshold
				if(Math.abs(topAvg-botAvg) > threshold) {
					img.pixels[y*img.width+x] = black;
				}
			}
		}
		return img;
	}
	
	public static PImage removeBackground(PImage pimg, float threshold) {
		int topPixel;
		int botPixel;
		float topAvg = 0.0f;
		float botAvg = 0.0f;
		int black = 0xff000000 | (0 << 16) | (0 << 8) | 0;
		img = pimg;
		img.format = PImage.RGB;
		img.loadPixels(); // load the pixels
		
		// loop vertically
		for(int y=0; y<img.height-1; y++) {
			// loop horizontally
			for(int x=0; x<img.width; x++) {
				// get the top and bottom pixels
				// faster to get it right from the pixels[] array
				// than using get(x, y)
				topPixel = img.pixels[y*img.width+x];
				botPixel = img.pixels[(y+1)*img.width+x];
				
				// get the color averages for the two pixels
				topAvg = getAverage(topPixel);
				botAvg = getAverage(botPixel);
				
				// check if the absolute valueof the difference
				// is LESS than the respThreshold
				if(Math.abs(topAvg-botAvg) < threshold) {
					img.pixels[y*img.width+x] = black;
				}
			}
		}
		return img;
	}
	
	private static float colorDistance(int pixel1, int pixel2) {
		float r1 = (pixel1 >> 16) & 0xff;   //get red value
		float g1 = (pixel1 >> 8) & 0xff;     //get green value
		float b1 = pixel1 & 0xff;      //get blue value
		
		float r2 = (pixel2 >> 16) & 0xff;   //get red value
		float g2 = (pixel2 >> 8) & 0xff;     //get green value
		float b2 = pixel2 & 0xff;      //get blue value
		
		// Use euclidean distance to compare colors
		return PApplet.dist(r1,g1,b1,r2,g2,b2);
	}
	
	private static int[] getColor(int apixel) {
		int a1 = (apixel >> 24) & 0xff;   //get alpha value
		int r1 = (apixel >> 16) & 0xff;   //get red value
		int g1 = (apixel >> 8) & 0xff;     //get green value
		int b1 = (apixel & 0xff);      //get blue value
		
		return new int[]{a1, r1, g1, b1};
	}
	
	private static float getAverage(int apixel) {
		int r1 = (apixel >> 16) & 0xff;   //get red value
		int g1 = (apixel >> 8) & 0xff;     //get green value
		int b1 = apixel & 0xff;      //get blue value
		
		return (r1 + g1 + b1) / 3.0f;
	}
	
}

