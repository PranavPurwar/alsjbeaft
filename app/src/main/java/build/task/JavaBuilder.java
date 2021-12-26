package com.pranav.ide.build.task;

import android.content.Context;
import com.pranav.ide.build.interfaces.*;
import com.pranav.ide.build.task.java.*;
import java.util.ArrayList;

public class JavaBuilder extends Builder {

	public final ClassLoader classloader;

	public Context mContext;

	public JavaBuilder(Context context, ClassLoader loader) {
		mContext = context;
		classloader = loader;
	}

	@Override
	public Task[] getTasks() {
		ArrayList<Task> tasks = new ArrayList<>();
		tasks.add(new CompileJavaTask(this));
		tasks.add(new DexTask(this));
		//tasks.add(new ExecuteDexTask());
		return tasks.toArray(new Task[0]);
	}

	@Override
	public Context getContext() {
		return mContext;
	}

	@Override
	public ClassLoader getClassloader() {
		return this.classloader;
	}
}
