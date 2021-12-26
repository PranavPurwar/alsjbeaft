package com.pranav.ide.build.task.java;

import android.content.Context;
import android.content.SharedPreferences;
import com.pranav.ide.FileUtil;
import com.pranav.ide.build.exception.CompilationFailedException;
import com.pranav.ide.build.interfaces.*;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.compiler.org.eclipse.jdt.internal.compiler.batch.Main;

public class CompileJavaTask extends Task {

	private final Builder mBuilder;
	private final StringBuilder errs = new StringBuilder();
	private final SharedPreferences prefs;

	public CompileJavaTask(Builder builder) {
		this.mBuilder = builder;
		prefs = mBuilder.getContext().getSharedPreferences("compiler_settings", Context.MODE_PRIVATE);
	}

	@Override
	public String getTaskName() {
		return this.getClass().getSimpleName();
	}

	@Override
	public void doFullTask() throws Exception {

		final ArrayList<String> args = new ArrayList<>();

		args.add("-" + prefs.getString("javaVersion", "7"));
		args.add("-nowarn");
		args.add("-deprecation");
		args.add("-d");
		args.add(FileUtil.getPackageDataDir(mBuilder.getContext()) + "/bin/classes");
		args.add("-g");
		args.add("-cp");
		StringBuilder classpath = new StringBuilder();
		classpath.append(FileUtil.getPackageDataDir(mBuilder.getContext()) + "/classpath/android.jar");
		if (prefs.getString("javaVersion", "7").equals("8")) {
			classpath.append(":");
			classpath.append(FileUtil.getPackageDataDir(mBuilder.getContext()) + "/classpath/core-lambda-stubs.jar");
		}
		final String clspath = prefs.getString("classpath", "");
		if (clspath != "") {
		classpath.append(":");
		classpath.append(clspath);
		}
		args.add(classpath.toString());
		args.add("-proc:none");
		args.add("-sourcepath");
		args.add("ignore");
		args.add(FileUtil.getPackageDataDir(mBuilder.getContext()).concat("/java/"));
		args.add("-log");
		args.add(FileUtil.getPackageDataDir(mBuilder.getContext()).concat("/bin/debug.xml"));

		PrintWriter writer = new PrintWriter(new OutputStream() {
			@Override
			public void write(int p1) throws IOException {
				errs.append((char) p1);
			}
		});

		Main main = new Main(writer, writer, false, null, null);

		main.compile(args.toArray(new String[0]));

		if (main.globalErrorsCount > 0) {
			throw new CompilationFailedException(errs.toString());
		}
	}
}
