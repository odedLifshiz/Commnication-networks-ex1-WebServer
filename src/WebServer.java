import java.net.*;

public final class WebServer {

	public static String s_root;
	public static String s_defaultPage;
	private static int s_port;
	private static int s_maxThreads;

	public static void main(String argv[]) {
		try {
			readConfigFile();
			Socket connection = null;
			ServerSocket socket = null;
			
			ThreadPool threadPool = new ThreadPool(s_maxThreads);
			System.out.println("WebServer: Creating thread pool");

			// Establish the listen socket.
			socket = new ServerSocket(s_port);

			// Process HTTP service requests in an infinite loop.
			while (true) {

				// Listen for a TCP connection request.
				System.out.println("WebServer listening on port " + s_port);
				connection = socket.accept();
				System.out.println("WebServer: Accepted a new connection");
				threadPool.execute(new HttpRequest(connection));

			}
		} catch (BadConfigFileException e) {
			e.printStackTrace();
		} catch (NumberFormatException e) {
			System.out.println("Bad config file. exiting");
		} catch (Exception e) {
			System.out.println("An error occured");
		}

	}

	private static void readConfigFile() throws Exception {
		ConfigFileParser parser = new ConfigFileParser("config.ini");
		s_root = parser.getRoot();
		s_port = parser.getPortNum();
		s_defaultPage = parser.getDeafaultPage();
		s_maxThreads = parser.getMaxThreads();
	}

}
