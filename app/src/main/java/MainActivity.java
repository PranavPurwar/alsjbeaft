package com.pranav.ide;

import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.button.*;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.pranav.ide.build.exception.CompilationFailedException;
import com.pranav.ide.build.task.JavaBuilder;
import com.pranav.ide.build.task.java.CompileJavaTask;
import com.pranav.ide.build.task.java.DexTask;
import com.pranav.ide.code.formatter.Formatter;
import dalvik.system.DexFile;
import dalvik.system.PathClassLoader;
import io.github.rosemoe.sora.langs.java.JavaLanguage;
import io.github.rosemoe.sora.widget.CodeEditor;
import io.github.rosemoe.sora.widget.schemes.SchemeDarcula;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;

public class MainActivity extends AppCompatActivity {

	private Toolbar toolbar;

	private CodeEditor editor;
	private MaterialButton btn_run;
	private MaterialButton btn_smali;
	private MaterialButton btn_smali2java;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		initialize();
		initializeLogic();
		grantChmod(getFilesDir().getParentFile());
	}

	private void initialize() {
		toolbar = findViewById(R.id._toolbar);
		setSupportActionBar(toolbar);
		getSupportActionBar().setDisplayHomeAsUpEnabled(false);
		getSupportActionBar().setHomeButtonEnabled(false);

		editor = findViewById(R.id.editor);
		btn_run = findViewById(R.id.btn_run);
		btn_smali = findViewById(R.id.btn_smali);
		btn_smali2java = findViewById(R.id.btn_smali2java);

		btn_smali2java.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				decompile();
			}
		});
		btn_smali.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				smali();
			}
		});
		btn_run.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				new AsyncTask<String, String, String>() {
					ProgressDialog pr;
					long ecjTime, jarTime, dxTime;

					@Override
					protected void onPreExecute() {
						pr = new ProgressDialog(MainActivity.this);

						pr.setMessage("Running...");

						pr.setCancelable(false);

						pr.show();
					}

					@Override
					protected String doInBackground(String... params) {

						JavaBuilder builder = new JavaBuilder(getApplicationContext(), getClassLoader());
						// code that prepares the files
						FileUtil.deleteFile(FileUtil.getPackageDataDir(getApplicationContext()).concat("/bin/"));
						FileUtil.makeDir(FileUtil.getPackageDataDir(getApplicationContext()).concat("/bin/"));
						FileUtil.makeDir(FileUtil.getPackageDataDir(getApplicationContext()).concat("/java/"));
						FileUtil.writeFile(
								FileUtil.getPackageDataDir(getApplicationContext()).concat("/java/Main.java"),
								editor.getText().toString());
						// code that copies android.jar and core-lambda-stubs.jar from
						// assets to temp folder (if not exists)
						if (!FileUtil.isExistFile(
								FileUtil.getPackageDataDir(getApplicationContext()).concat("/classpath/android.jar"))) {
							com.pranav.ide.util.ZipUtil.unzipFromAssets(getApplicationContext(), "android.jar.zip",
									FileUtil.getPackageDataDir(getApplicationContext()) + "/classpath");
							try (InputStream input = getAssets().open("core-lambda-stubs.jar");
									OutputStream output = new FileOutputStream(
											FileUtil.getPackageDataDir(getApplicationContext())
													+ "/classpath/core-lambda-stubs.jar");) {
								byte[] buffer = new byte[input.available()];
								int length;
								while ((length = input.read(buffer)) != -1) {
									output.write(buffer, 0, length);
								}
							} catch (Exception e) {

							}
						}
						// code that runs ecj
						long time = System.currentTimeMillis();
						publishProgress("Compiling Java...");
						try {
							CompileJavaTask javaTask = new CompileJavaTask(builder);
							javaTask.doFullTask();
						} catch (Throwable e) {
							if (e instanceof CompilationFailedException) {
								return e.getMessage();
							}
							return Log.getStackTraceString(e);
						}
						ecjTime = System.currentTimeMillis() - time;
						// code that packages classes to a JAR
						time = System.currentTimeMillis();
						publishProgress("Running dx");
						try {
							new DexTask(new com.pranav.ide.build.task.JavaBuilder(getApplicationContext(),
									getClassLoader())).doFullTask();
						} catch (Exception e) {
							return "Dexing jar failed: " + e.toString();
						}
						dxTime = System.currentTimeMillis() - time;
						return "";
					}

					@Override
					protected void onProgressUpdate(String... values) {
						pr.setMessage(values[0]);
					}

					@Override
					protected void onPostExecute(String result) {
						pr.dismiss();
						final String error = result;
						if (result.isEmpty()) {
							final TextView tx = new TextView(MainActivity.this);
							tx.setLayoutParams(new LinearLayout.LayoutParams(-2, -2));
							tx.setTextSize(15);
							tx.setPadding(30, 30, 30, 30);
							tx.setTextIsSelectable(true);

							final ScrollView sc = new ScrollView(MainActivity.this);
							sc.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));
							sc.addView(tx);
							// code that loads the final dex
							{
								try {
									DexFile file = new DexFile(FileUtil.getPackageDataDir(getApplicationContext())
											.concat("/bin/classes.dex"));
									Enumeration<String> f = file.entries();
									final ArrayList<String> classes = Collections.list(file.entries());
									listDialog("Select a class to execute", classes.toArray(new String[0]),
											new DialogInterface.OnClickListener() {
												@Override
												public void onClick(DialogInterface dialog, int pos) {
													exec(classes.get(pos),
															"Success! Ecj took: " + String.valueOf(ecjTime) + " " + "ms"
																	+ ", Dx took: " + String.valueOf(dxTime));
												}
											});
								} catch (Throwable e) {
									final String stack = Log.getStackTraceString(e);
									dialog("Failed...", stack, true);
								}
							}
						} else {
							Snackbar.make(findViewById(R.id.container), "An error occurred", Snackbar.LENGTH_INDEFINITE)
									.setAction("Show error", new View.OnClickListener() {
										@Override
										public void onClick(View view) {
											dialog("Failed..", error, true);
										}
									}).show();
						}
					}
				}.execute("");
			}
		});
	}

	private void initializeLogic() {
		editor.setTypefaceText(Typeface.MONOSPACE);

		editor.setOverScrollEnabled(true);

		editor.setEditorLanguage(new JavaLanguage());

		editor.setColorScheme(new SchemeDarcula());

		editor.setTextSize(12);
		if (FileUtil.isExistFile(FileUtil.getPackageDataDir(getApplicationContext()).concat("/java/Main.java"))) {
			editor.setText(
					FileUtil.readFile(FileUtil.getPackageDataDir(getApplicationContext()).concat("/java/Main.java")));
		} else {
			editor.setText("package com.example;\n\nimport java.util.*;\n\n" + "public class Main {\n\n"
					+ "\tpublic static void main(String[] args) {\n" + "\t\tSystem.out.print(\"Hello, World!\");\n"
					+ "\t}\n" + "}\n");
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, 0, 0, "Prettify");
		menu.add(0, 1, 0, "Settings");
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case 0:
			Formatter formatter = new Formatter(editor.getText().toString());
			editor.setText(formatter.format());
			break;

		case 1:
			final Intent intent = new Intent();
			intent.setClass(getApplicationContext(), SettingActivity.class);
			startActivity(intent);
			break;
		default:
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	public void smali() {
		try {
			DexFile file = new DexFile(FileUtil.getPackageDataDir(getApplicationContext()).concat("/bin/classes.dex"));
			Enumeration<String> f = file.entries();
			final ArrayList<String> classes = Collections.list(file.entries());
			listDialog("Select a class to extract source", classes.toArray(new String[0]),
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int pos) {
							final String claz = classes.get(pos);
							new AsyncTask<String, String, String>() {
								ProgressDialog prog;

								@Override
								protected void onPreExecute() {
									prog = new ProgressDialog(MainActivity.this);

									prog.setMessage("Running baksmali...");

									prog.setCancelable(false);

									prog.show();
								}

								@Override
								protected String doInBackground(String... params) {
									String[] str = new String[] { "-f", "-o",
											FileUtil.getPackageDataDir(getApplicationContext()).concat("/bin/smali/"),
											FileUtil.getPackageDataDir(getApplicationContext())
													.concat("/bin/classes.dex") };
									com.googlecode.d2j.smali.BaksmaliCmd.main(str);
									return "";
								}

								@Override
								protected void onPostExecute(String _result) {
									prog.dismiss();
									CodeEditor edi = new CodeEditor(MainActivity.this);
		
		edi.setTypefaceText(Typeface.MONOSPACE);
		
		edi.setOverScrollEnabled(true);
		
		edi.setEditorLanguage(new JavaLanguage());
		
		edi.setColorScheme(new SchemeDarcula());
		
		edi.setTextSize(10);
		
		edi.setText(
		    formatSmali(FileUtil.readFile(FileUtil.getPackageDataDir(getApplicationContext())
				+ "/bin/smali/" + claz.replace(".", "/") + ".smali"))
		);
		
		final androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(MainActivity.this)
		.setView(edi)
		.create();
		dialog.setCanceledOnTouchOutside(true);
		dialog.show();
//									dialog("Smali source",
//											formatSmali(FileUtil
//													.readFile(FileUtil.getPackageDataDir(getApplicationContext())
//															+ "/bin/smali/" + claz.replace(".", "/") + ".smali")),
//											true);
								}
							}.execute("");
						}

						private String formatSmali(String in) {

							ArrayList<String> lines = new ArrayList<>(Arrays.asList(in.split("\n")));

							boolean insideMethod = false;

							for (int i = 0; i < lines.size(); i++) {

								String line = lines.get(i);

								if (line.startsWith(".method")) {
									insideMethod = true;
								}

								if (line.startsWith(".end method")) {
									insideMethod = false;
								}

								if (insideMethod && !shouldSkip(line)) {
									lines.set(i, line + "\n");
								}
							}

							StringBuilder result = new StringBuilder();

							for (int i = 0; i < lines.size(); i++) {
								if (i != 0) {
									result.append("\n");
								}

								result.append(lines.get(i));
							}

							return result.toString();
						}

						private boolean shouldSkip(String smaliLine) {

							String[] ops = { ".line", ":", ".prologue" };

							for (String op : ops) {

								if (smaliLine.trim().startsWith(op)) {
									return true;
								}
							}

							return false;
						}
					});
		} catch (Throwable e) {
			dialog("Failed to extract smali source", Log.getStackTraceString(e), true);
		}
	}

	public void decompile() {
		try {
			final ArrayList<String> classes = new ArrayList<>();
			org.jf.dexlib2.iface.DexFile dexfile = org.jf.dexlib2.DexFileFactory.loadDexFile(FileUtil.getPackageDataDir(getApplicationContext()).concat("/bin/classes.dex"), org.jf.dexlib2.Opcodes.forApi(21));
			for (org.jf.dexlib2.iface.ClassDef f : dexfile.getClasses().toArray(new org.jf.dexlib2.iface.ClassDef[0])) {
				final String name = f.getType().replace("/", ".");
				classes.add(name.substring(1, name.length() - 1));
			}
			listDialog("Select a class to extract source", classes.toArray(new String[0]),
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int pos) {
							final String claz = classes.get(pos).replace(".", "/");
							new AsyncTask<String, String, String>() {
								ProgressDialog prog;

								@Override
								protected void onPreExecute() {
									prog = new ProgressDialog(MainActivity.this);

									prog.setMessage("Running CFR...");

									prog.setCancelable(false);

									prog.show();
								}

								@Override
								protected String doInBackground(String... params) {
									String[] args = {
											//	"-jar",
											FileUtil.getPackageDataDir(getApplicationContext())
													.concat("/bin/classes/" + claz + ".class"),
											"--extraclasspath",
											FileUtil.getPackageDataDir(getApplicationContext()).concat("/bin/cp.jar"),
											"--outputdir",
											FileUtil.getPackageDataDir(getApplicationContext()).concat("/bin/cfr/")
									};
									try {
										org.benf.cfr.reader.Main.main(args);
									} catch (final Exception e) {
										dialog("Failed to decompile...", Log.getStackTraceString(e), true);
									}
									return "";
								}

								@Override
								protected void onPostExecute(String result) {
									prog.dismiss();
									CodeEditor edi = new CodeEditor(MainActivity.this);
		
		edi.setTypefaceText(Typeface.MONOSPACE);
		
		edi.setOverScrollEnabled(true);
		
		edi.setEditorLanguage(new JavaLanguage());
		
		edi.setColorScheme(new SchemeDarcula());
		
		edi.setTextSize(10);
		
		edi.setText( 
		    FileUtil.readFile(FileUtil.getPackageDataDir(getApplicationContext()).concat("/bin/cfr/" + claz + ".java"))
		);
		
		final androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(MainActivity.this)
		.setView(edi)
		.create();
		dialog.setCanceledOnTouchOutside(true);
		dialog.show();
//									dialog("Decompiled code",
//											FileUtil.readFile(FileUtil.getPackageDataDir(getApplicationContext())
//													+ "/bin/cfr/" + claz + ".java"),
//											true);
								}
							}.execute("");
						}
					});
		} catch (Throwable e) {
			dialog("Failed to decompile code", Log.getStackTraceString(e), true);
		}
	}

	public void exec(String clazz, String title) {
		final StringBuilder output = new StringBuilder();
		final OutputStream outstream = new OutputStream() {
			@Override
			public void write(int b) {
				output.append(String.valueOf((char) b));
			}

			@Override
			public String toString() {
				return output.toString();
			}
		};
		System.setOut(new PrintStream(outstream));
		System.setErr(new PrintStream(outstream));
		try {
			final PathClassLoader loader = new PathClassLoader(
					FileUtil.getPackageDataDir(getApplicationContext()).concat("/bin/classes.dex"), getClassLoader());

			final Class calledClass = loader.loadClass(clazz);

			final java.lang.reflect.Method method = calledClass.getDeclaredMethod("main", String[].class);

			final String[] param = {};
			final Object res = method.invoke(null, new Object[] { param });
		} catch (java.lang.reflect.InvocationTargetException e) {
			dialog("Failed..", "Runtime error: " + e.getCause().toString(), true);
			return;
		} catch (Exception e) {
			dialog("Failed..",
					"Couldn't execute the dex: " + e.toString() + "\n\nSystem logs:\n" + outstream.toString(), true);
			return;
		}
		dialog(title, output.toString(), false);
	}

	public void listDialog(String title, String[] items, DialogInterface.OnClickListener listener) {
		final MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(MainActivity.this).setTitle(title)
				.setItems(items, listener);
		dialog.create().show();
	}

	public void dialog(String title, final String message, boolean copyButton) {
		final MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(MainActivity.this).setTitle(title)
				.setMessage(message).setPositiveButton("GOT IT", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialogInterface, int i) {
					}
				}).setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialogInterface, int i) {
					}
				});
		if (copyButton)
			dialog.setNeutralButton("COPY", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialogInterface, int i) {
					((ClipboardManager) getSystemService(getApplicationContext().CLIPBOARD_SERVICE))
							.setPrimaryClip(ClipData.newPlainText("clipboard", message));
				}
			});
		dialog.create().show();
	}
	
	public void grantChmod(File file) {
		try {
			if (file.isDirectory()) {
				Runtime.getRuntime().exec("chmod 777 " + file.getAbsolutePath());
				for (File f : file.listFiles()) {
					grantChmod(f);
				}
			} else {
				Runtime.getRuntime().exec("chmod 777 " + file.getAbsolutePath());
			}
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}
}
