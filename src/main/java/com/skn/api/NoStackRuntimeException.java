package com.skn.api;

public class NoStackRuntimeException extends RuntimeException
{
	private static final long serialVersionUID = 1L;

	public NoStackRuntimeException()
	{
		super();
	}

	public NoStackRuntimeException(String message, Throwable cause)
	{
		super(message, cause);
	}

	public NoStackRuntimeException(String message)
	{
		super(message);
	}

	public NoStackRuntimeException(Throwable cause)
	{
		super(cause);
	}
	
	@Override
	public synchronized Throwable fillInStackTrace()
	{
		return this;
	}
}
