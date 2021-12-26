package com.pranav.ide.build.interfaces;

public abstract class Task {

	public abstract String getTaskName();

	public abstract void doFullTask() throws Exception;
}
