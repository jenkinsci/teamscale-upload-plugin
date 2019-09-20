/*-----------------------------------------------------------------------+
 | com.teamscale.index
 |                                                                       |
   $Id$            
 |                                                                       |
 | Copyright (c)  2009-2015 CQSE GmbH                                 |
 +-----------------------------------------------------------------------*/
package eu.cqse.teamscale.client;


/**
 * Enumeration of all supported report formats that can be uploaded to the
 * ExternalReportUploadService. Each format must be registered in the
 * ReportParserFactory.
 */
public enum EReportFormat {

	/** Astree xml report format. */
	ASTREE("Astree"),

	/** JaCoCo (Java Code Coverage) xml report format. */
	JACOCO("JaCoCo"),

	/** Cobertura (Java test coverage) xml report format. */
	COBERTURA("Cobertura"),

	/** Gcov (Profiling tool for code compiled with gcc) report format. */
	GCOV("Gcov"),

	/** Lcov (Linux Test Project (LTP) front-end for Gcov) report format. */
	LCOV("Lcov"),

	/** Ctc (Testwell CTC++ coverage for C/C++) report format. */
	CTC("CTC"),

	/** XR.Baboon (code coverage for C# on Mono) report format. */
	XR_BABOON("XR.Baboon"),

	/** MS Coverage report format (CQSE Coverage Merger). */
	MS_COVERAGE("MS Coverage"),

	/** MS Coverage report format (Visual Studio Coverage Merger). */
	VS_COVERAGE("VS Coverage"),

	/** dotCover (Jetbrains coverage tool for .NET) report format. */
	DOT_COVER("dotCover"),

	/** Roslyn (Microsoft .NET) report format. */
	ROSLYN("Roslyn"),

	/** Simple coverage report format for testing. */
	SIMPLE("Teamscale Simple Coverage"),

	/** Cppcheck (static analysis for C/C++) results in XML format. */
	CPPCHECK("Cppcheck"),

	/** PClint (C/C++) coverage report format. */
	PCLINT("PClint"),

	/** Clang (C, C++, Objective C/C++) coverage report format. */
	CLANG("Clang"),

	/** Pylint (static analysis for Python) findings report format. */
	PYLINT("Pylint"),

	/**
	 * FindBugs (static analysis for Java) findings report format. SpotBugs is the
	 * successor fork of the now unmaintained FindBugs.
	 * 
	 * SpotBugs is almost completely compatible to the old FindBugs reports. The
	 * only change so far was
	 * https://github.com/spotbugs/spotbugs/commit/ea791376d60c92bf83c634bb6ea84c699fa7b453#diff-c673820b9f0388af8baf2703ef924c6b
	 * (they removed a dependency on jFormatString, can't generate some findings any
	 * more, and removed them from messages.xml and findbugs.xml) and changes of
	 * finding messages ("FindBugs" to "SpotBugs"). We handle these renamings in
	 * EAnalysisProfileVersion#ANALYSIS_PROFILE_VERSION_52. We support the old
	 * FindBugs-only findings, new SpotBugs-only findings, and common findings with
	 * the improved messages from SpotBugs.
	 */
	FINDBUGS("FindBugs/SpotBugs"),

	/** Bullseye (C++) coverage report format. */
	BULLSEYE("Bullseye"),

	/** FxCop (.NET) findings report format. */
	FXCOP("FxCop"),

	/** SpCop (Sharepoint Code Analysis) findings report format. */
	SPCOP("SpCop"),

	/** JUnit (Java unit tests) report format. */
	JUNIT("JUnit"),

	/** XUnit (.NET unit tests) report format. */
	XUNIT("XUnit"),

	/** MS Test report format. */
	MS_TEST("MSTest"),

	/** Istanbul (JavaScript coverage) report format. */
	ISTANBUL("Istanbul"),

	/** C# Compiler warnings format */
	CS_COMPILER_WARNING("C# Compiler Warning"),

	/** Simulink Model Advisor report format. */
	MODEL_ADVISOR("Simulink Model Advisor"),

	/** CSV issues report format */
	ISSUE_CSV("CSV Issues"),

	/** Our own export format for SAP code inspector findings. */
	SAP_CODE_INSPECTOR("SAP Code Inspector Export"),

	SAP_COVERAGE("SAP Coverage", false),

	/** Custom testwise coverage report format. */
	TESTWISE_COVERAGE("Testwise Coverage"),

	/** JSON output format of compiler warnings reported by the Closure Compiler. */
	CLOSURE_COMPILER("Closure Compiler Warnings"),

	/** Line coverage data in txt format from Xcode (xccov). */
	XCODE("Xcode Coverage"),

	/** Clover test coverage */
	CLOVER("Clover"),

	/** OpenCover test coverage */
	OPEN_COVER("OpenCover"),

	/**
	 * Proprietary coverage format developed by Engel for upload of IEC coverage.
	 */
	IEC_COVERAGE("IEC Coverage"),

	/** LLVM coverage report format. */
	LLVM("LLVM Coverage"),

	/** Our own generic finding format. */
	GENERIC_FINDINGS("Teamscale Generic Findings"),

	/** Our own generic non-code metric format. */
	GENERIC_NON_CODE("Teamscale Non-Code Metrics"),

	/** Parasoft C/C++text. */
	PARASOFT_CPP_TEST("Parasoft C/C++test");

	/** Each ReportFormat needs a readable name for the UI */
	private final String readableName;

	/** Whether this is allowed for upload or only for internal processing. */
	private final boolean allowedForUpload;

	private EReportFormat(String readableName) {
		this(readableName, true);
	}

	private EReportFormat(String readableName, boolean allowedForUpload) {
		this.readableName = readableName;
		this.allowedForUpload = allowedForUpload;
	}

	public String getReadableName() {
		return this.readableName;
	}

	public boolean isAllowedForUpload() {
		return allowedForUpload;
	}
}
