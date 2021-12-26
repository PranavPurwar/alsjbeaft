package com.pranav.ide.code.formatter;

import java.util.Map;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.formatter.CodeFormatter;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Document;

public class Formatter {

	private String source;

	public Formatter(String source) {
		this.source = source;
	}

	public String format() {
		Map options = DefaultCodeFormatterConstants.getEclipse21Settings();

		options.put(JavaCore.COMPILER_COMPLIANCE, JavaCore.VERSION_1_7);
		options.put(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, JavaCore.VERSION_1_7);
		options.put(JavaCore.COMPILER_SOURCE, JavaCore.VERSION_1_7);
		options.put(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_ENUM_CONSTANTS,
				DefaultCodeFormatterConstants.createAlignmentValue(true,
						DefaultCodeFormatterConstants.WRAP_ONE_PER_LINE,
						DefaultCodeFormatterConstants.INDENT_ON_COLUMN));

		options.put(DefaultCodeFormatterConstants.FORMATTER_ALIGN_TYPE_MEMBERS_ON_COLUMNS,
				DefaultCodeFormatterConstants.createAlignmentValue(true,
						DefaultCodeFormatterConstants.WRAP_ONE_PER_LINE,
						DefaultCodeFormatterConstants.INDENT_ON_COLUMN));

		options.put(DefaultCodeFormatterConstants.FORMATTER_ALIGN_FIELDS_GROUPING_BLANK_LINES,
				DefaultCodeFormatterConstants.createAlignmentValue(true,
						DefaultCodeFormatterConstants.WRAP_ONE_PER_LINE,
						DefaultCodeFormatterConstants.INDENT_ON_COLUMN));

		options.put(DefaultCodeFormatterConstants.FORMATTER_BLANK_LINES_BEFORE_IMPORTS, "1");

		options.put(DefaultCodeFormatterConstants.FORMATTER_BLANK_LINES_AFTER_IMPORTS, "1");

		options.put(DefaultCodeFormatterConstants.FORMATTER_COMMENT_CLEAR_BLANK_LINES_IN_JAVADOC_COMMENT,
				DefaultCodeFormatterConstants.TRUE);

		options.put(DefaultCodeFormatterConstants.FORMATTER_COMMENT_CLEAR_BLANK_LINES_IN_BLOCK_COMMENT,
				DefaultCodeFormatterConstants.TRUE);

		options.put(DefaultCodeFormatterConstants.FORMATTER_COMMENT_PRESERVE_WHITE_SPACE_BETWEEN_CODE_AND_LINE_COMMENT,
				DefaultCodeFormatterConstants.TRUE);

		// instantiate the default code formatter with the given options
		final CodeFormatter codeFormatter = ToolFactory.createCodeFormatter(options);

		final TextEdit edit = codeFormatter.format(CodeFormatter.K_COMPILATION_UNIT | CodeFormatter.F_INCLUDE_COMMENTS, // format a compilation unit
				source, // source to format
				0, // starting position
				source.length(), // length
				0, // initial indentation
				System.lineSeparator() // line separator
		);

		final IDocument document = new Document(source);
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					edit.apply(document);
				} catch (Exception e) {
					throw new RuntimeException(e);
				}

			}
		}).run();

		// return the formatted code
		return document.get();
	}
}
