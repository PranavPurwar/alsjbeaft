package com.pranav.ide.build.task.java;

import android.net.Uri;
import com.pranav.ide.FileUtil;
import com.pranav.ide.build.interfaces.*;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import kellinwood.security.zipsigner.ZipSigner;

public class JarTask extends Task {
	private final Builder mBuilder;

	private static Attributes getDefAttrs() {
		Attributes attrs = new Attributes();
		attrs.put(new Attributes.Name("Created-By"), "1.0 (Android)");
		return attrs;
	}

	public JarTask(Builder builder) {
		this.mBuilder = builder;
	}

	@Override
	public void doFullTask() throws Exception {

		// input file
		final File classesFolder = new File(FileUtil.getPackageDataDir(mBuilder.getContext()) + "/bin/classes/");

		// output file
		final String output = (FileUtil.getPackageDataDir(mBuilder.getContext()) + "/bin/classes_unsigned.jar");

		if (classesFolder.listFiles() == null)
			throw new IllegalStateException("Compiled class files not found");
		// Open archive file
		FileOutputStream stream = new FileOutputStream(new File(output));

		Manifest manifest = buildManifest(getDefAttrs());

		// Create the jar file
		JarOutputStream out = new JarOutputStream(stream, manifest);

		// Add the files..
		for (File clazz : classesFolder.listFiles()) {
			add(classesFolder.getPath(), clazz, out);
		}

		out.close();
		stream.close();

		// Sign the jar with kellinwood's ZipSigner
		ZipSigner signer = new ZipSigner();
		signer.setKeymode("testkey");
		signer.signZip(output, output.replace(Uri.parse(output).getLastPathSegment(), "classes.jar"));
	}

	@Override
	public String getTaskName() {
		return "JarTask";
	}

	private Manifest buildManifest(Attributes options) {
		Manifest manifest = new Manifest();
		manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
		if (options != null) {
			manifest.getMainAttributes().putAll(options);
		}
		return manifest;
	}

	private void add(String parentPath, File source, JarOutputStream target) throws IOException {
		String name = source.getPath().substring(parentPath.length() + 1);

		BufferedInputStream in = null;
		try {
			if (source.isDirectory()) {
				if (!name.isEmpty()) {
					if (!name.endsWith("/")) {
						name += "/";
					}
					// Add the Entry
					JarEntry entry = new JarEntry(name);
					entry.setTime(source.lastModified());
					target.putNextEntry(entry);
					target.closeEntry();
				}

				for (File child : source.listFiles()) {
					add(parentPath, child, target);
				}
				return;
			}

			JarEntry entry = new JarEntry(name);
			entry.setTime(source.lastModified());
			target.putNextEntry(entry);
			in = new BufferedInputStream(new FileInputStream(source));
			byte[] buffer = new byte[1024];
			int count = in.read(buffer);
			while (count != -1) {
				target.write(buffer, 0, count);
			}
			target.closeEntry();

		} finally {
			if (in != null) {
				in.close();
			}
		}
	}
}
