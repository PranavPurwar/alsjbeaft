package com.pranav.ide.build.interfaces;

import android.content.Context;

public abstract class Builder {

    public abstract Task[] getTasks();

    public abstract Context getContext();

    public abstract ClassLoader getClassloader();
}
