/**
 * 
 */
package edu.stanford.facs.panel;

import java.io.IOException;

/**
 * @author wmoore
 * 
 */
public interface PanelFactory
{
	public Marker getMarker (String name);

	public Hapten getHapten (String name);

	public Fluorochrome getFluorochrome (String name)
			throws IOException;
	
	public Instrument getInstrument (String name)
			throws IOException;
}
