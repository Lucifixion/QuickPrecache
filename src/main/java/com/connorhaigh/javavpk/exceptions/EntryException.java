package com.connorhaigh.javavpk.exceptions;

public class EntryException extends Exception
{
	/**
	 * Creates a new VPK archive entry exception.
	 * @param message the message
	 */
	public EntryException(String message)
	{
		super(message);
	}
	
	public static final long serialVersionUID = 1;
}
