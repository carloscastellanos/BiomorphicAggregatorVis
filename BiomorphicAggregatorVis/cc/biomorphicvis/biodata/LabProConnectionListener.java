/**
 * 
 */
package cc.biomorphicvis.biodata;

import java.util.EventListener;

/**
 * @author carlos
 *
 */
public interface LabProConnectionListener extends EventListener
{
	public abstract void labProConnectionEvent(boolean connected);
}
