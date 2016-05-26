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
package de.tudarmstadt.lt.utilities;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;

/**
 *
 * @author Steffen Remus
 */
public class ReflectionUtils {

	private ReflectionUtils() { /* do not instantiate */ }

	public static Collection<Method> listAvailableMainMethods(String package_prefix, String exclude_regex) {

		ClassLoader[] classLoaders = new ClassLoader[] {
				// ReflectionUtilsTest.class.getClassLoader(),
				ClasspathHelper.contextClassLoader(),
				ClasspathHelper.staticClassLoader()
		};

		FilterBuilder filter = new FilterBuilder().include(package_prefix + ".*");
		if (exclude_regex != null)
			filter.exclude(exclude_regex).exclude("org\\.reflections.*");

		Reflections reflections = new Reflections(
				new ConfigurationBuilder()
				.setScanners(new SubTypesScanner(false /* don't exclude Object.class */))
				.setUrls(ClasspathHelper.forClassLoader(classLoaders))
				.filterInputsBy(filter));

		Set<Class<? extends Object>> types = reflections.getSubTypesOf(Object.class);

		Set<Method> main_methods = new HashSet<Method>();
		for (Class<? extends Object> clazz : types) {
			for (Method method : clazz.getMethods()) {
				if (isMainMethod(method))
					main_methods.add(method);
			}
		}

		return main_methods;
	}

	public static boolean isMainMethod(Method method) {
		return "main".equals(method.getName()) &&
				Modifier.isStatic(method.getModifiers()) &&
				Modifier.isPublic(method.getModifiers()) &&
				method.getParameterTypes().length == 1 &&
				String[].class.equals(method.getParameterTypes()[0]);
	}

}
