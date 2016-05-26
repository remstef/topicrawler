/*
 *   Copyright 2013
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


import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Scanner;

import de.tudarmstadt.lt.utilities.ReflectionUtils;

/**
 *
 * @author Steffen Remus
 */
public class MainFinder {

	public final static String _exit_error_text = String.format("%s finished with errors.", MainFinder.class);

	private static String _lt_search_path = "de.tudarmstadt.lt";

	public String getDefaultSearchPath() {
		return _lt_search_path;
	}

	public static void main(String[] args) {
		MainFinder f = new MainFinder();
		int exit_status = f.search(f.getDefaultSearchPath(), args);
		if (exit_status != 0)
			System.err.println(_exit_error_text);
		System.exit(exit_status);
	}

	int search(String prefix, String[] args) {
		System.out.format("%s searching for main entry points with prefix '%s'.%n", MainFinder.class.getName(), prefix);
		List<Method> main_methods = new ArrayList<Method>(ReflectionUtils.listAvailableMainMethods(prefix, "(de\\.tudarmstadt\\.lt\\.utilities\\..*)|(MainFinder.*)"));
		Collections.sort(main_methods, new Comparator<Method>() {
			@Override
			public int compare(Method o1, Method o2) {
				return o1.getDeclaringClass().getName().compareTo(o2.getDeclaringClass().getName());
			}
		});

		Scanner input_reader = new Scanner(System.in);
		if (main_methods.isEmpty()) {
			System.out.format("Found no classes with main entry points with prefix '%s'. Check your classpath.%n", prefix);
			return search_again(args);
		}

		System.out.println("Please choose one of the following numbers to run the respective main method. Arguments will be passed to this method. Enter 'q' to quit.");
		System.out.format(" %d: %s %n", 0, "search again with different class prefix.");
		for (int i = 0; i < main_methods.size(); i++)
			System.out.format(" %d: %s %n", i + 1, main_methods.get(i).getDeclaringClass().getName());
		System.out.format("Enter [%d-%d] or 'q': ", 0, main_methods.size());
		String input = input_reader.next();
		if (input.matches("[qQ]"))
			return 0;

		while (!(input.matches("\\d+") && Integer.parseInt(input) <= main_methods.size() && Integer.parseInt(input) >= 0)) {
			System.out.format("Enter [%d-%d] or 'q': ", 0, main_methods.size());
			input = input_reader.next();
		}
		int num_chosen = Integer.parseInt(input) - 1;
		if (num_chosen < 0)
			return search_again(args);

		Method chosen_main = main_methods.get(num_chosen);
		try {
			chosen_main.invoke(null, (Object) args);
		} catch (Exception e) {
			System.err.format("Could not invoke '%s': %n  %s: %s%n", chosen_main.toGenericString(), e.getClass().getName(), e.getMessage());
			if (e.getCause() != null)
				System.err.format("  Cause: %s: %s%n", e.getCause().getClass().getName(), e.getCause().getMessage());
			System.err.flush();

			System.out.format("Print stack trace? [y/n]");
			if (input_reader.next().matches("[yY]")) {
				if (e.getCause() != null) e.getCause().printStackTrace();
				else e.printStackTrace();
			}
			return 1;
		}
		return 0;
	}

	int search_again(String[] args) {
		Scanner input_reader = new Scanner(System.in);
		// System.out.format("Try searching with different prefix? [y/n]: ");
		// String input = input_reader.nextLine();
		// if (!input.matches("[yY]"))
		// return 0;
		System.out.format("Please enter class prefix: ");
		String prefix = input_reader.nextLine();
		return search(prefix, args);
	}

}
