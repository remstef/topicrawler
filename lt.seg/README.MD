    ###
    #   Copyright 2015
    #
    #   Licensed under the Apache License, Version 2.0 (the "License");
    #   you may not use this file except in compliance with the License.
    #   You may obtain a copy of the License at
    #
    #       http://www.apache.org/licenses/LICENSE-2.0
    #
    #   Unless required by applicable law or agreed to in writing, software
    #   distributed under the License is distributed on an "AS IS" BASIS,
    #   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    #   See the License for the specific language governing permissions and
    #   limitations under the License.
    #
    ###

### Prerequisities

* Java v.8
* (optional) bash v.4

### How to install

* Download latest version from the releases site https://github.com/tudarmstadt-lt/seg/releases
* unpack into a directory of choice: `tar -xzvf lt.seg-version-dist.tar.gz -C <your-preferred-directory>`
* executables can be found in the `bin` directory, e.g. `bin/seg`. You can execute it from any directory.
* (optional) to access the `seg` binary from anywhere you can add <your-preferred-directory>/bin to PATH: `PATH=<your-preferred-directory>:$PATH` or symlink `seg` into a directory which is already in your PATH


*Note: the following description is for unix based systems, you cannot run the startup shell scripts on MS Windows, consider using cygwin or run the java commands manually*

### How to use the lt.seg segmenter

basic usage is as simple as

		cat text.txt | seg > segmented_text.txt

or 

		seg < text.txt > segmented_text.txt

or 

		seg -f text.txt > segmented_text.txt


lt.seg comes with a number of parameters, run `seg -?` to get a list of options

*Note: for MS Windows based systems replace* `seg` *with the correct java command, e.g.* `java -cp lt.seg-<version>-with-dependencies.jar de.tudarmstadt.lt.seg.app.Segmenter <options>`

### Options:
* `--sentencesplitter <class>` (`-s`):
	Sepcify the sentence splitter class. Supported values are:
	* `RuleSplitter` (default): Applies (language-specific) rules and heuristics to decide when a sentence ends and a new sentence begins
	* `BreakSplitter`: Java sentence breakiterator instance
	* `LineSplitter`: Start a new sentence segment only when a new line occurs (supported line separators: `\n`, `\r\n`)
	* `NullSplitter`: Convenience splitter, returns the complete input as one segment
* `--tokenizer <class>` (`-t`)
	Sepcify the tokenizer class. Supported values are:
	* `RuleTokenizer` (default): Applies tokenization according to a ruleset specified by the `--token-ruleset` option parameter
	* `DiffTokenizer`: Applies simple rules based on the change on Unicode category of consecutive characters
	* `BreakTokenizer`: Java word breakiterator instance
	* `EmptySpaceTokenizer`: creates a new segment only when empty spaces are found (supported empty spaces include but are not limited to: `<blank>`, `<protected-blank>`, `\t`, `\n`, `\r`, `\f`, ...)
	* `NullTokenizer`: Convenience tokenizer, returns the complete input as one segment
* `--normalize <level>` (`-nl`)
    * `0` (default): no normalization, each segment will be printed as it is in the input
    * `1`: reduce same consecutive non-word characters, e.g. multiple consecutive blanks will be merged to one. Example: "\t\t\n\t\t" -> "\t\n\t"
    * `2`: `1` + replace empty space and punctuation characters with its symbol 
    * `3`: `2` + replace consecutive numbers and digits within words and number segments themselves with the symbol `0`. Example 'He11o World. I am Johnny 5.' -> 'He0o World . I am Johnny 0 .'
    * `4`: `3` + replace all non-word segments with its symbol.
    * `5`: `4` + lowercase words.
* `--filter <level>` (`-fl`): *Note: examples below use normalization level (-nl)* `2` and DiffTokenizer
    * `0`: no filtering, each segment will be printed separated by blanks (this also includes emptyspace segments, in most cases you probably want to use at least `1` or `2`)
	* `1`: filter control character segments
    * `2`: (default): `1` + filter emptyspace segments
    * `3`: `2` + filter unclassified and non-readable segments (attention: results heavily depend on tokenizer)
    * `4`: `3` + filter punctuation characters
    * `5`: `4` + filter meta data like URLs, file descriptors, emails, wiki markup, emoticons, etc.
    * `6`: `4` + filter numbers and words with numbers. Example: "The number is 534 423 or 43. ? :-/ " -> "The number is or" (Only useful with proper token normalization level.)
* `--merge [<level>]` (`-ml`): *Note: examples below use normalization level (-nl)* `2`
	* `0`: no merging (default when not specified)
	* `1`: merge same consecutive token types if they are not words or words with numbers (default when just -ml specified). Example: "The number is 534 423 or 43. ? :-/ " -> "The number is 0 or 0 . "
	* `2`: merge same consecutive words. Example: "Let's go to New New York." -> "Let s go to New York"
* `--sentence-separator <SEP>` (`-seps`): Specify the separator string SEP for sentences (default: '\n').
* `--token-separator <SEP>` (`-sept`): Specify the separator string SEP for tokens (default: ' ').
* `--source-separator <SEP>` (`-sepd`): Specify the separator string SEP for the source description (default: '\t').
* `--onedocperline` (`-l`): Assume one document per line. Also inserts sentence break at line endings.
* `--parallel <num>`: run in parallel mode. *Note: parallelism > 1 can only be applied if `-l` is passed. Also note that the order of input and output sentences can not be guaranteed*
* `--debug`: Enable debugging output.

### Apply segmentation programatically:

programmatic usage is as simple as
		
		new DiffTokenizer().init("This is your text you want to tokenize.").foreach(System.out::println)
		
		new RuleSplitter().init("This is your text you want to split into sentences.").foreach(System.out::println)

or

		new DiffTokenizer().init(new InputStreamReader(new FileInputStream("your-file"))).foreach(System.out::println)

		new RuleSplitter().init(new InputStreamReader(new FileInputStream("your-file"))).foreach(System.out::println)

more complex filtering, normalization and only printing when tokens start with the character `a`:

		new DiffTokenizer().init(...).filteredAndNormalizedTokens(5, 4).forEach(x -> {if(x.startsWith("a")) System.out.println(x);}));

or with sentence splitting before tokenizing:
		
		ITokenizer tokenizer = new DiffTokenizer();
		new RuleSplitter().init(new InputStreamReader(new FileInputStream("your-file"))).foreach(s -> 
			tokenizer.init(...).filteredAndNormalizedTokens(5, 4).forEach(x -> {if(x.startsWith("a")) System.out.println(x);}));

*Note: The supplied Tokenizer and SentenceSplitter are not thread safe, each thread needs its own Tokenizer and SentenceSplitter.*
