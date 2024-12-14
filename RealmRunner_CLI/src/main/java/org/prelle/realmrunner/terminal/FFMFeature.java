package org.prelle.realmrunner.terminal;

import org.graalvm.nativeimage.hosted.Feature;
import org.graalvm.nativeimage.hosted.RuntimeForeignAccess;

import static java.lang.foreign.ValueLayout.*;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;

/**
 * 
 */
public class FFMFeature implements Feature {
		
		public void duringSetup(DuringSetupAccess access) {
			System.out.println("\n\nFFMFeature\n\n");
			// Linux
		    RuntimeForeignAccess.registerForDowncall(FunctionDescriptor.of(JAVA_INT, JAVA_INT, ADDRESS));
		    RuntimeForeignAccess.registerForDowncall(FunctionDescriptor.of(JAVA_INT, JAVA_INT, JAVA_INT, ADDRESS));
		    RuntimeForeignAccess.registerForDowncall(FunctionDescriptor.of(JAVA_INT, JAVA_LONG, JAVA_INT, JAVA_LONG));
		    RuntimeForeignAccess.registerForDowncall(FunctionDescriptor.of(JAVA_INT, JAVA_INT, JAVA_INT, ADDRESS));
		    // Windows
		    RuntimeForeignAccess.registerForDowncall(FunctionDescriptor.of(ADDRESS, JAVA_INT));
		    RuntimeForeignAccess.registerForDowncall(FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS));
		    RuntimeForeignAccess.registerForDowncall(FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT));
		    RuntimeForeignAccess.registerForDowncall(FunctionDescriptor.of(JAVA_INT, JAVA_LONG, ADDRESS));
		    RuntimeForeignAccess.registerForDowncall(FunctionDescriptor.of(JAVA_INT));
		    RuntimeForeignAccess.registerForDowncall(FunctionDescriptor.of(JAVA_INT, JAVA_INT));
		}

}
