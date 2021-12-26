package com.pranav.ide.build.task.java;

import android.content.Context;
import com.pranav.ide.build.interfaces.*;
import dalvik.system.PathClassLoader;
import java.io.*;
import java.lang.reflect.*;

public class ExecuteJavaTask extends Task {

	private final Builder mBuilder;
	private String dexFile = "";
	public StringBuilder log = new StringBuilder();

	public ExecuteJavaTask(Builder builder) {
		this.mBuilder = builder;
	}

	@Override
	public String getTaskName() {
		return "Execute java Task";
	}

	@Override
	public void doFullTask() throws Exception {
		OutputStream out = new OutputStream() {
			@Override
			public void write(int b) {
				log.append(String.valueOf((char) b));
			}

			@Override
			public String toString() {
				return log.toString();
			}
		};
		System.setErr(new PrintStream(out));
		System.setOut(new PrintStream(out));
		PathClassLoader loader = new PathClassLoader(dexFile, mBuilder.getClassloader());

		Class calledClass = loader.loadClass("Main");

		Method method = calledClass.getDeclaredMethod("main", String[].class);

		String[] param = {};

		Object result = method.invoke(null, new Object[] { param });
	}

	public String getLogs() {
		return this.log.toString();
	}
}
