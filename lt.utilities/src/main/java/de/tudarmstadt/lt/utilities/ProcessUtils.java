/*
 *   Copyright 2014
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package de.tudarmstadt.lt.utilities;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

/**
 * @author Steffen Remus
 *
 */
public class ProcessUtils {
	
	private ProcessUtils(){ /* DO NOT INSTANTIATE */ }
	
	public static void run_process(String command, File working_directory, boolean print_output){
		run_process(command, working_directory, print_output, true);
	}
	
	public static void run_process(String command, File working_directory, boolean print_output, boolean wait){
		try {
			ProcessBuilder b = new ProcessBuilder();
			b.directory(working_directory.getAbsoluteFile());
			b.redirectErrorStream(print_output);
			b.command("/bin/bash", "-c", command);
			Process p = b.start();
			if(print_output && wait){
				BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
				for(String line = r.readLine(); line != null; line = r.readLine())
					System.out.println(line);
				r.close();
			}
			if(wait)
				p.waitFor();
		}
		catch (Exception err) {
			err.printStackTrace();
		}
	}

}
