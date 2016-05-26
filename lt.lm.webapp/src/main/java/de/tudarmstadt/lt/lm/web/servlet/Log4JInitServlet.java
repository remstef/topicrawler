//package de.tudarmstadt.lt.lm.web.servlet;
//
//import java.io.File;
//
//import javax.servlet.ServletConfig;
//import javax.servlet.ServletContext;
//import javax.servlet.ServletException;
//import javax.servlet.http.HttpServlet;
//
//import org.apache.log4j.BasicConfigurator;
//import org.apache.log4j.PropertyConfigurator;
//
//public class Log4JInitServlet extends HttpServlet {
//
//	private static final long serialVersionUID = -7668184892187110137L;
//
//	@Override
//	public void init(ServletConfig config) throws ServletException {
//		String log4jLocation = config.getInitParameter("log4j-properties-location");
//		ServletContext sc = config.getServletContext();
//		String webAppPath = sc.getRealPath("/");
//		System.setProperty("webapp-path", webAppPath);
//		String log4jProp = new File(webAppPath, log4jLocation).getAbsolutePath();
//
//		if (log4jLocation == null || !new File(log4jProp).exists()) {
//			System.err.println("*** No log4j-properties, initializing log4j with BasicConfigurator");
//			BasicConfigurator.configure();
//		} else {
//			System.out.println("Initializing log4j with: " + log4jProp);
//			PropertyConfigurator.configureAndWatch(log4jProp);
//		}
//		super.init(config);
//	}
//
//}
