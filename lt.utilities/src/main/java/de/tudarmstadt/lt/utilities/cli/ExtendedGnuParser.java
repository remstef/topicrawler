/*
 *   Copyright 2012
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
package de.tudarmstadt.lt.utilities.cli;

import java.util.ListIterator;

import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.ParseException;

/**
 *
 * @author Steffen Remus
 **/
public class ExtendedGnuParser extends GnuParser {

	private boolean ignoreUnrecognizedOption;

	public ExtendedGnuParser(final boolean ignoreUnrecognizedOption) {
		this.ignoreUnrecognizedOption = ignoreUnrecognizedOption;
	}

	@SuppressWarnings("rawtypes")
	@Override
	protected void processOption(final String arg, final ListIterator iter) throws ParseException {
		boolean hasOption = getOptions().hasOption(arg);
		// TODO: do something with the unrecognized options
		
		if (hasOption || !ignoreUnrecognizedOption) {
			super.processOption(arg, iter);
		}
	}

}
