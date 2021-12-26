package com.pranav.ide.build.exception;

public class CompilationFailedException extends Exception {

	public CompilationFailedException(Throwable e) {
		super(e);
	}

	public CompilationFailedException(Exception exception) {
		super(exception);
	}

	public CompilationFailedException(String message) {
		super(message);
	}
}
