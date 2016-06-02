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

- Required: Oracle JDK or OpenJDK v. 1.8

- download and unpack heritrix 3.2:

   - crawler.archive.org

<!-- -->

		wget http://builds.archive.org/maven2/org/archive/heritrix/heritrix/3.2.0/heritrix-3.2.0-dist.tar.gz
      
		tar -xvzf heritrix-*.tar.gz
   
- Download and unpack the latest topicrawler-plugin binaries into the heritrix directory:

   - [topicrawler-releases](https://github.com/tudarmstadt-lt/topicrawler/releases)

<!-- -->

		wget https://github.com/tudarmstadt-lt/topicrawler/releases/download/v0.4.0c/lt.ltbot-0.4.0c-dist.tar.gz
    
		tar -xvzf lt.ltbot-*.tar.gz --strip-components 1 -C heritrix-3.2.0
      
- Start Language Model Server:

   - have a corpus ready in a directory which contains .txt files. Ideally txt files are in the format one sentence per line with space separated tokens.
   - start LM Server:

<!-- -->

		bin/lm -Xmx10g -Dlt.lm.knUnkLog10Prob=-10 de.tudarmstadt.lt.lm.app.StartLM -n 5 -d <dir-to-txt-files> -i <service-name>
   
   - for an explanation of options run 

<!-- -->

		bin/lm de.tudarmstadt.lt.lm.app.StartLM -?
   
   - the first time the language model is started a file named <dirname>.arpa.gz will be created in the specified directory. This file is used for faster loading of the language model. It can be created using the BerkeleyLM or SRILM framework. The languagemodel is loaded from this file if it exists.
   - If you encounter OutOfMemory Exceptions or want to set different Java Options you can specify a JAVA_OPTS environment variable, e.g.:

<!-- -->

		export JAVA_OPTS='-Xmx20g'
         
- Start Heritrix v.3.2
   
   - Consider to increase the number of open file handles before the first start of Heritrix (see Heritrix Documentation and http://crawler.archive.org/faq.html#toomanyopenfiles)
   - Consider to increase the heap space memory used by Heritrix, e.g. 10 GB or more:
   
<!-- -->

		export JAVA_OPTS='-Xmx10g'
   
   - run Heritrix start command:

<!-- -->
   
		bin/heritrix -a <user>:<password> -b / 
      
   - open a web browser and go to 'https://server:8443'. Login with the provided user and password (if you run heritrix for the first time with that browser you also have to accept the certificate). 
   - find and click the link 'ltbot-profile'
   - copy the profile to create a new crawl job (click 'Copy Job' in the upper part of the page) and give it a proper name.
   - click 'Configuration' in order to configure the new CrawlJob:
      
      - find the line starting with '#metadata.operatorContactUrl='. Set a proper URL and uncomment this line. Website administrators will be able see this URL in their server logs.
      - find the lines starting with 'perplexityProducer.' and set the options like host and service id according to the language model service that is running.  
      - find the line starting with 'perplexityPrioritizer.assignmentBoundaries='. Set some perplexity boundaries that make sense (according to your experiments from above). Hyperlinks from websites with perplexity values lower than the specified boundary will be downloaded first. Three boundaries are possible: high priority, medium priority, rest. By setting rest not to infinity links from websites with higher perplexity values will be discarded. Typical values are 100, 5000, 50000.
      - set 'perplexityPrioritizer.maxvalue' to a value that makes sense, since perplexity values will be normalized by that value internally for setting the precedence costs. Usually boundary for 'rest'.
      - find the line starting with '# INITIAL URLS HERE' and enter your seed URLs within this XML block as exemplified in the config file.
      - for further fine tuning, e.g. accept or reject specific URLS (SURTS) you can specify these in the followed XML blocks. 
      - for more options see Heritrix Documentation at 'https://webarchive.jira.com/wiki/display/Heritrix/Heritrix3' and 'http://crawler.archive.org/articles/developer_manual/'
      - save the configuration and return to the Job page 
      
   - click 'Build' to check if your configuration contains syntax errors
   - click 'Launch' to run the job and start crawling (reload the page to see process, click on unpause if crawl is paused)
   - click on 'Terminate' and 'Teardown' to end the crawl manually
   - check the directory 'heritrix-3.2.0/jobs/<jobname>/warcs/' for the download files in warc archives (default setting, you can also provide another writer, see Heritrix Documentation for details)
   
