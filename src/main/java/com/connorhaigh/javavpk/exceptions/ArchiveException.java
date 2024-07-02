package com.connorhaigh.javavpk.exceptions;

public class ArchiveException extends Exception
{
	/**
	 * Creates a new VPK archive exception.
	 * @param message the message
	 */
	public ArchiveException(String message)
	{
		super(message);
	}
	
	public static final long serialVersionUID = 1;
}
