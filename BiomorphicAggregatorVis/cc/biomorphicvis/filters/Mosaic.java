/**
 * 
 */
package cc.biomorphicvis.filters;

import processing.core.PImage;


/**
 * @author carlos
 *
 */
public class Mosaic
{
	//private static PImage img;
	
	// kinda cheating since Processing automatically resizes source pixels
	// to fit the specified target region if they dont match (ie does the
	// block resizing and color averaging for you)
	// based on code by Ji-Dong Yim
	public static PImage mosaic(PImage buff, int blockSize) {
		if(blockSize < 1) blockSize = 1;
		PImage temp = new PImage(buff.width, buff.height);
		temp.copy(buff, 0, 0, buff.width, buff.height, 0, 0, buff.width/blockSize, buff.height/blockSize);
		temp.copy(temp, 0, 0, buff.width/blockSize, buff.height/blockSize, 0, 0, buff.width, buff.height);
		return temp;
	}
	
	
	/*
	//METHOD MOSAIC
	public static PImage mosaic(PImage pimg, int blockSize) {
		img = pimg;
		img.format = PImage.RGB;
		//blocksize is the size of the side of the mosaic tile
		img.loadPixels();
		//go through every pixel block in the image and convert it
		for(int i=0;i<img.height;i+=blockSize) {
			for(int j =0;j<img.width; j+=blockSize) {
				int color1 = getAverage(j,i,blockSize); //function to retrieve average color from a square of pixels on the screen
		        fillBlock(j,i,color1, blockSize);
			}
		}
		img.updatePixels();
		return img;
	}

		  //FUNCTION GETAVERAGE - Finds the average value of a blockSize x blocsksize set of pixels
	private static int getAverage(int startX, int startY, int blockSize) {
		int currColor;
		int arraySize = blockSize*blockSize;  //total number of pixels in block
		int index;  //tracks the current screen pixels  (width *height)
		int blockIndex; //tracks the current block pixel (blocksize x blocksize)
		int a;   //alpha value of final color
		int r;  //red value of final color
		int g;  //green value of final color
		int b;  //blue value of final color
		int tempa;   //storage for most recent alpha value
		int tempr;   //storage for most recent red value
		int tempb;   //storage for most recent blue value
		int tempg;  //storage for most recent green value
		int[] colorIndex = new int[arraySize]; //array to store all the color values for a particular block

		//iterate through the block starting at the set x,y value and get all the color values
		for(int j = 0;j<blockSize;j++) {     //vertical location
			for(int k = 0; k<blockSize; k++){  //horizontal location
				blockIndex = j*blockSize+k;      //current location on the index of block pixels
		        index = (startY+j)*img.width +(startX+k);  //current location on the index of screen pixels
		        if(index < img.width*img.height){              //ensures that we don't go off the screen and get null exceptions
		          colorIndex[blockIndex] = img.pixels[index];     //get the color of the particular pixel
		        }
			}
		}
		//setup initial values for r,g,b,and a
		a = (colorIndex[0] >> 24) & 0xff;   //get alpha value
		r = (colorIndex[0] >> 16) & 0xff;   //get red value
		g = (colorIndex[0] >> 8) & 0xff;     //get green value
		b = (colorIndex[0] & 0xff);      //get blue value

		//loop through the remaining values to next color value
		for(int m =1; m< arraySize; m++){
			tempa = (colorIndex[m] >> 24) & 0xff;   //get alpha value
			tempr = (colorIndex[m] >> 16) & 0xff;   //get red value
			tempg = (colorIndex[m] >> 8) & 0xff;     //get green value
			tempb = (colorIndex[m] & 0xff);      //get blue value

			//compute averages
			a=(a+tempa)/2; //alpha
			r=(r+tempr)/2;  //red
			g = (g+tempg)/2;  //green
			b= (b+tempb)/2;  //blue
		}
		currColor = 0x00000000 | (a << 24) | (r << 16) | (g << 8) | (b);   //create the full rgb value for color
		return currColor;
	}

		  //METHOD TO FILL A BLOCK WITH A SOLID COLOR
	private static void fillBlock(int startX, int startY, int c, int blockSize){
		//int arraySize = blockSize*blockSize;   //get total number of pixels to change
		for(int j = 0;j<blockSize;j++){         //vertical value
			for(int k = 0; k<blockSize; k++){     //horizontal value
		        if(startX+k < img.width && startY < img.height){    //make sure we don't exceed the size of the image
		          img.set(startX+k, startY+j, c);          //set pixel to color
		        }
			}
		}
	}
	*/
}
