package com.pranav.ide.build.task.java;

import com.pranav.ide.FileUtil;
import com.pranav.ide.build.interfaces.*;
import com.pranav.ide.dx.command.dexer.Main;
import java.io.File;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

public class DexTask extends Task {

	private final Builder mBuilder;

	public DexTask(Builder builder) {
		this.mBuilder = builder;
	}

	@Override
	public void doFullTask() throws Exception {
		final File f = new File(FileUtil.getPackageDataDir(mBuilder.getContext()) + "/bin/classes/");
		List<String> args = Arrays.asList("--debug", "--verbose", "--min-sdk-version=21", "--output=" + f.getParent(),
				f.getAbsolutePath());
		Main.clearInternTables();
		Main.Arguments arguments = new Main.Arguments();
		Method parseMethod = Main.Arguments.class.getDeclaredMethod("parse", String[].class);
		parseMethod.setAccessible(true);
		parseMethod.invoke(arguments, (Object) args.toArray(new String[0]));
		Main.run(arguments);
	}

	@Override
	public String getTaskName() {
		return "Dex Task";
	}
}
