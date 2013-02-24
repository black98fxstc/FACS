package edu.stanford.facs.panel;

import java.util.*;

public class PopulationStaining
		extends ArrayList<MarkerStaining>
{
	public final String name;

	public PopulationStaining(String name, Collection<MarkerStaining> staining)
	{
		super(staining);
		this.name = name;
	}
}
