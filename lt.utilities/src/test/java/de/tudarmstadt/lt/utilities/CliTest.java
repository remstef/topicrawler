package de.tudarmstadt.lt.utilities;

import java.util.Map;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.lt.utilities.cli.CliUtils;
import de.tudarmstadt.lt.utilities.cli.ExtendedGnuParser;


public class CliTest {

	private final static Logger LOG = LoggerFactory.getLogger(CliTest.class);
	private final static String USAGE_HEADER = "Options:";

	
	@SuppressWarnings("static-access")
	public static void main(String[] args) {
		Options opts = new Options();
		opts.addOption(new Option("?", "help", false, "display this message"));
		opts.addOption(OptionBuilder.withLongOpt("host").withArgName("hostname").hasArgs(1).withDescription("specifies the hostname (default: localhost)").create("h"));
		opts.addOption(OptionBuilder.withLongOpt("port").withArgName("port-number").hasArg().withDescription("specifies the port (default: 0, which means a random port)").create("p"));
		opts.addOption(OptionBuilder.withLongOpt("dir").withArgName("directory").isRequired().hasArg().withDescription("specify the directory that contains '.txt' files that are used as source for this language model").create("d"));
		opts.addOption(OptionBuilder.withLongOpt("parallel").withArgName("num-threads").hasArg().withDescription("specify number of parallel threads").create());
		opts.addOption(OptionBuilder.withLongOpt("exists").withDescription("specify existence").create());
		opts.addOption(OptionBuilder.withLongOpt("").hasArg().create());
		
		try {
			CommandLine cmd = new ExtendedGnuParser(true).parse(opts, args);
			if (cmd.hasOption("help")) 
				CliUtils.print_usage(System.err, CliTest.class.getSimpleName(), opts, USAGE_HEADER, null);

			Map<String, String> map = CliUtils.getOptionsMap(cmd.getOptions());
			
			System.out.println(map);
			
			System.out.println(cmd.getOptionValue("dir", "{}"));

		} catch (Exception e) {
			LOG.error("{}: {}", e.getClass().getSimpleName(), e.getMessage());
			CliUtils.print_usage(System.err, CliTest.class.getSimpleName(), opts, USAGE_HEADER, String.format("%s: %s%n", e.getClass().getSimpleName(), e.getMessage()));
		}
		
		


	}
	
	@Test
	public void cliTest1(){
		CliTest.main("-d test --parallel 8".split(" "));
	}
	
	@Test
	public void cliTest2(){
		CliTest.main("-d test -hostname localhost -blum=blua --aiso=jdaoj asd".split(" "));
	}
	
	@Test
	public void cliTest3(){
		CliTest.main(new String[]{"-d", "{hello=\"world, earth\"}", "--exists"});
	}

}
