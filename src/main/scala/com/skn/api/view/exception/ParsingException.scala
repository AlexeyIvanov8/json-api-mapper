package com.skn.api.view.exception

import com.skn.api.NoStackRuntimeException

trait ParsingException extends NoStackRuntimeException

object ParsingException
{
	def apply() = new NoStackRuntimeException() with ParsingException
	def apply(message: String, cause: Throwable) = new NoStackRuntimeException(message, cause) with ParsingException
	def apply(message: String) = new NoStackRuntimeException(message) with ParsingException
	def apply(cause: Throwable) = new NoStackRuntimeException(cause) with ParsingException
}