/**
 * 
 */
package edu.stanford.facs.panel;

/**
 * @author wmoore
 * 
 */
abstract class PanelComponent
		implements Comparable<PanelComponent>
{
	public final String name;

	public PanelComponent(String name)
	{
		this.name = name;
	}

	@Override
	public int compareTo (PanelComponent o)
	{
		return name.compareTo(o.name);
	}

	@Override
	public int hashCode ()
	{
		return name.hashCode();
	}

	@Override
	public boolean equals (Object obj)
	{
		return name.equals(obj);
	}

	@Override
	public String toString ()
	{
		return name;
	}
}
