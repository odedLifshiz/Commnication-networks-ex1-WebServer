import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConfigFileParser {

	private HashMap<String, String> m_configFileParamsHashMap;

	public ConfigFileParser(String filePath) throws BadConfigFileException {
		m_configFileParamsHashMap = new HashMap<String, String>();
		parseConfigFile(filePath);
	}

	public int getPortNum() throws BadConfigFileException {
		int port;
		if (m_configFileParamsHashMap.containsKey("port")) {
			port = Integer.parseInt(m_configFileParamsHashMap.get("port"));
		} else {
			throw new BadConfigFileException("Bad port");
		}

		return port;

	}

	public String getRoot() throws BadConfigFileException {
		String root;
		if (m_configFileParamsHashMap.containsKey("root")) {
			root = m_configFileParamsHashMap.get("root");
		} else {
			throw new BadConfigFileException("Bad root");
		}
		return root;

	}

	public String getDeafaultPage() throws BadConfigFileException {
		String defaultPage;
		if (m_configFileParamsHashMap.containsKey("defaultPage")) {
			defaultPage = m_configFileParamsHashMap.get("defaultPage");
		} else {
			throw new BadConfigFileException("Bad default page");
		}
		return defaultPage;

	}

	public int getMaxThreads() throws BadConfigFileException {
		int maxThreads = 0;
		if (m_configFileParamsHashMap.containsKey("maxThreads")) {
			maxThreads = Integer.parseInt(m_configFileParamsHashMap
					.get("maxThreads"));
		} else {
			throw new BadConfigFileException("Bad max threads");
		}
		if (maxThreads <= 0 || maxThreads > 100) {

			throw new BadConfigFileException("Bad max threads");
		}

		return maxThreads;

	}

	public void parseConfigFile(String configFilePath)
			throws BadConfigFileException {
		String configFileContent = readConfigFile(configFilePath);
		parseFileContent(configFileContent);
	}

	@SuppressWarnings("resource")
	private String readConfigFile(String configFilePath)
			throws BadConfigFileException {
		BufferedReader reader = null;
		String line = null;
		String configFileContent = "";

		try {
			reader = new BufferedReader(new FileReader(configFilePath));

			while ((line = reader.readLine()) != null) {
				configFileContent += line + "\n";
			}

		} catch (FileNotFoundException e) {
			throw new BadConfigFileException("Could not find config file");

		} catch (IOException e) {
			throw new BadConfigFileException("Could not read config file");
		}

		return configFileContent;

	}

	private void parseFileContent(String configFileContent) {

		Pattern configFilePattern = Pattern.compile("(.*)=(.*)\\n");
		Matcher configFileMatcher = configFilePattern
				.matcher(configFileContent);
		while (configFileMatcher.find()) {
			m_configFileParamsHashMap.put(configFileMatcher.group(1),
					configFileMatcher.group(2));
		}

	}
}