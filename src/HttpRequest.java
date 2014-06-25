import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class HttpRequest implements Runnable {
	public enum ResponseCode {
		OK, NotFound, NotImplemented, BadRequest, InternalServerError
	}

	private static final String r_CRLF = "\r\n";
	private static final String r_parameterFilePath = "params_info.html";
	private static final String r_okString = "200 OK";
	private static final String r_badRequestString = "400 Bad Request";
	private static final String r_notFoundString = "404 Not Found";
	private static final String r_internalServerError = "500 Internal Server Error";
	private static final String r_notImplementedString = "501 Not Implemented";
	private static final String r_defaultVersion = "1.0";
	private static final String r_contentTypeHttp = "message/http";
	private static final String r_contentTypeHtml = "text/html";
	private static final String r_contentTypeJPG = "image/jpeg";
	private static final String r_contentTypeBMP = "image/bmp";
	private static final String r_contentTypeGIF = "image/gif";
	private static final String r_contentTypePNG = "image/png";
	private static final String r_contentTypeIcon = "image/x-icon";
	private static final String r_contentTypeApplicationOctetStream = "application/octet-stream";
	private static final String r_pathToDefaultPage = WebServer.s_root
			+ WebServer.s_defaultPage;
	private HashMap<String, String> m_getPostParamsHashMap,
			m_requestHeadersHashMap, m_responseHeadersHasMap;
	private Socket m_socket;
	private DataOutputStream m_os;
	private DataInputStream m_is;
	private String m_httpMethod = null;
	private String m_filePathString = null;
	private String m_httpVersion = null;
	private String m_requestHeaders = null;
	private String m_initialLineParams;
	private String m_requestFromUser = null;
	private String m_initialLine = null;
	private File m_fileToReturn = null;
	private BufferedReader m_bufferedReader = null;
	private boolean m_connectionClosed = false;

	// Constructor
	public HttpRequest(Socket connection) {
		m_socket = connection;
		m_responseHeadersHasMap = new HashMap<String, String>();
		m_requestHeadersHashMap = new HashMap<String, String>();
	}

	// Implement the run() method of the Runnable interface.
	public void run() {
		try {
			processRequest();
		} catch (FileNotFoundException e) {
			try {
				buildHttpResponse(ResponseCode.InternalServerError);
			} catch (Exception e1) {
			}
			closeResources();
		} catch (IOException e) {
			closeResources();
		} catch (NumberFormatException e) {
			try {
				buildHttpResponse(ResponseCode.BadRequest);
			} catch (Exception e1) {
			}
			return;
		} catch (Exception e) {
			closeResources();
		}
		// this finally block will call closeGracefully only if another method
		// did not call it already
		finally {
			try {
				closeResources();
			} catch (Exception t) {
			}
		}
	}

	private void closeResources() {

		try {
			if (!m_connectionClosed) {
				if (m_bufferedReader != null) {
					m_bufferedReader.close();
				}

				if (m_is != null) {
					m_is.close();
				}

				if (m_os != null) {
					m_os.close();
				}

				if (m_socket != null) {
					m_socket.close();
				}
			}
		} catch (Exception e) {
		} finally {
			m_connectionClosed = true;
		}

	}

	private void processRequest() throws Exception {
		String requestedPagePath = null;
		m_requestFromUser = readRequest();
		System.out.println(m_requestFromUser);
		if (!checkValidityAndparseRequestStructure(m_requestFromUser)) {
			buildHttpResponse(ResponseCode.BadRequest);
		} else {
			getParametersOfInitialLine(m_initialLineParams);
			requestedPagePath = WebServer.s_root + m_filePathString;
			if (!pageIsValid(requestedPagePath)) {
				buildHttpResponse(ResponseCode.BadRequest);
			} else {

				if (m_httpMethod.equals("GET")) {
					handleReadingGET(m_initialLine);
				} else if (m_httpMethod.equals("POST")) {
					handleReadingPOST(m_initialLine);
				} else if (m_httpMethod.equals("HEAD")) {
					m_fileToReturn = null;
					m_responseHeadersHasMap.remove("Content-Length");
					handleReadingHEAD(m_initialLine);
				} else if (m_httpMethod.equals("TRACE")) {
					handleReadingTRACE(m_initialLine);
				} else if (m_httpMethod.equals("OPTIONS")) {
					handleReadingOPTIONS(m_initialLine);
				} else {
					buildHttpResponse(ResponseCode.NotImplemented);
				}
			}
		}
	}

	private boolean checkValidityAndparseRequestStructure(
			String requestFromUser2) throws Exception {
		boolean valid = false;
		Pattern checkRequestPattern = Pattern.compile(
				"(.*) /(.*) HTTP/(1\\.[0-1])\\n(.*)", Pattern.MULTILINE
						| Pattern.DOTALL);
		Matcher checkRequestMatcher = checkRequestPattern
				.matcher(m_requestFromUser);

		valid = checkRequestMatcher.matches();

		if (valid) {
			m_initialLine = m_requestFromUser.substring(0,
					m_requestFromUser.indexOf("\n"));
			m_httpMethod = checkRequestMatcher.group(1);
			m_filePathString = checkRequestMatcher.group(2);
			int indexOfQuestionMark = m_filePathString.indexOf('?');
			if (indexOfQuestionMark > -1) {
				m_initialLineParams = m_filePathString
						.substring(indexOfQuestionMark + 1);
				m_filePathString = m_filePathString.substring(0,
						indexOfQuestionMark);

			}
			m_httpVersion = checkRequestMatcher.group(3);
			m_requestHeaders = checkRequestMatcher.group(4);

			parse_request_headers();
			if (m_httpVersion.equals("1.1")
					&& m_requestHeadersHashMap.get("Host") == null) {
				valid = false;
			}

			if (m_requestHeadersHashMap.containsKey("Chunked")) {
				if (!m_httpVersion.equals("1.1"))
					buildHttpResponse(ResponseCode.BadRequest);
			}

		}
		return valid;
	}

	private void handleReadingOPTIONS(String initialLine) throws Exception {
		buildHttpResponse(ResponseCode.OK);

	}

	private void handleReadingTRACE(String initialLine) throws Exception {
		m_responseHeadersHasMap.put("Content-Type", r_contentTypeHttp);
		buildHttpResponse(ResponseCode.OK);
	}

	private void handleReadingHEAD(String initialLine) throws Exception {
		String contentType = determineContentType(m_filePathString);
		m_responseHeadersHasMap.put("Content-Type", contentType);
		buildHttpResponse(ResponseCode.OK);
	}

	private void handleReadingPOST(String initialLine) throws Exception {

		String postParamsString = null;
		int contentLength = Integer.parseInt(m_requestHeadersHashMap
				.get("Content-Length"));
		int byteRead = 0;
		byte[] postParamsByteArray = new byte[contentLength];
		int currentByte = 0;

		while (currentByte < contentLength
				&& (byteRead = m_bufferedReader.read()) > 0) {
			postParamsByteArray[currentByte] = (byte) byteRead;
			currentByte++;
		}
		postParamsString = new String(postParamsByteArray);
		parsePostParams(postParamsString);
		String contentType = determineContentType(m_filePathString);
		m_responseHeadersHasMap.put("Content-Type", contentType);
		buildHttpResponse(ResponseCode.OK);

	}

	/***
	 * Checks if file requested by client is valid
	 * 
	 * 
	 * @param requestedPagePath
	 * @return true if file is valid otherwise false
	 * @throws Exception
	 */
	private boolean pageIsValid(String requestedPagePath) throws Exception {

		boolean valid = false;
		File file = new File(requestedPagePath);
		if (!filePathIsUnderRootFolder(requestedPagePath)) {
			valid = false;
		}

		// If the client asked for params_info.html
		else if (file.getCanonicalPath().equalsIgnoreCase(
				WebServer.s_root + r_parameterFilePath)) {
			valid = true;
		} else if (file.isFile()) {
			m_fileToReturn = new File(requestedPagePath);
			valid = true;

		} else if (file.isDirectory()) {
			m_filePathString = r_pathToDefaultPage;
			m_fileToReturn = new File(m_filePathString);
			if (m_fileToReturn.exists()) {
				valid = true;
			} else {
				throw new FileNotFoundException();
			}
		} else {
			valid = false;
		}
		return valid;

	}

	// handles parsing all of the GET parameters
	private void handleReadingGET(String initialLine) throws Exception {
		String contentType = determineContentType(m_filePathString);
		m_responseHeadersHasMap.put("Content-Type", contentType);
		buildHttpResponse(ResponseCode.OK);
	}

	private String determineContentType(String filePathString) {
		String contentType = null;

		if (filePathString.endsWith(".bmp")) {
			contentType = r_contentTypeBMP;

		} else if (filePathString.endsWith(".jpg")) {
			contentType = r_contentTypeJPG;

		} else if (filePathString.endsWith(".gif")) {
			contentType = r_contentTypeGIF;

		} else if (filePathString.endsWith(".png")) {
			contentType = r_contentTypePNG;

		} else if (filePathString.endsWith(".html")) {
			contentType = r_contentTypeHtml;

		} else if (filePathString.endsWith(".ico")) {
			contentType = r_contentTypeIcon;
		} else {
			contentType = r_contentTypeApplicationOctetStream;
		}
		return contentType;
	}

	// parses the request headers (that are after the initial line) and puts
	// them in a hash table
	private void parse_request_headers() {

		Pattern requestParamsPattern = Pattern.compile("(.*): (.*)\\n");
		Matcher requestParamsMatcher = requestParamsPattern
				.matcher(m_requestHeaders);
		while (requestParamsMatcher.find()) {
			m_requestHeadersHashMap.put(requestParamsMatcher.group(1),
					requestParamsMatcher.group(2));
		}

	}

	// parses the parameters that are in the get and put them in a hash table
	private void getParametersOfInitialLine(String paramsOfInitialLine) {

		// if there is a match we should have parameters
		// put the parameters in a hash table
		m_getPostParamsHashMap = new HashMap<String, String>();

		// in order to more easily parse the parameters an & is
		// appended to the parameters part
		String getPostParameters = paramsOfInitialLine + '&';
		Pattern splitGetPostParamsPattern = Pattern.compile("([^=]*)=([^&]*)&");
		Matcher splitGetPostParamsMatcher = splitGetPostParamsPattern
				.matcher(getPostParameters);
		while (splitGetPostParamsMatcher.find()) {
			m_getPostParamsHashMap.put(splitGetPostParamsMatcher.group(1),
					splitGetPostParamsMatcher.group(2));
		}

	}

	private boolean filePathIsUnderRootFolder(String filePath) throws Exception {
		boolean isValid = false;
		File f = new File(filePath);
		String canonical = null;
		canonical = f.getCanonicalPath();

		String[] splitedPath = canonical.split("\\\\");
		String[] splitedRoot = WebServer.s_root.split("\\\\");

		isValid = (splitedPath[0].equalsIgnoreCase(splitedRoot[0]) && splitedPath[1]
				.equalsIgnoreCase(splitedRoot[1]));

		return isValid;

	}

	private void buildHttpResponse(ResponseCode responseCode) throws Exception {

		String httpResponseInitialLine = "";
		String httpResponseParamterLines = "";
		byte[] pageContent = null;
		boolean isChunked = false;
		// build the response based on the responseCode
		switch (responseCode) {
		case OK:
			httpResponseInitialLine = buildInitialLineOfHttpResponse(
					r_okString, m_httpVersion);

			if (m_requestHeadersHashMap.containsKey("Chunked")) {
				String chunkedAnswer = m_requestHeadersHashMap.get("Chunked");
				isChunked = chunkedAnswer.equals("yes");
				httpResponseParamterLines += "Transfer-Encoding: chunked"
						+ r_CRLF;
			}

			if (m_responseHeadersHasMap.containsKey("Content-Type")) {
				httpResponseParamterLines += "Content-Type: "
						+ m_responseHeadersHasMap.get("Content-Type") + r_CRLF;
			}

			if (m_httpMethod.equals("TRACE")) {
				pageContent = m_requestFromUser.getBytes();
			}

			else if (m_httpMethod.equals("OPTIONS")) {
				httpResponseParamterLines += "Allow: OPTIONS, HEAD, TRACE, POST, GET"
						+ r_CRLF;
				if (!isChunked) {
					httpResponseParamterLines += "Content-Length: 0" + r_CRLF;
				}
			}

			else if (m_httpMethod.equals("POST")) {
				String html = buildParamsInfoHtmlForResponse();
				pageContent = html.getBytes();
				if (!isChunked) {
					httpResponseParamterLines += "Content-Length: "
							+ pageContent.length + r_CRLF;
				}
			}

			else if (m_fileToReturn != null) {
				if (!isChunked) {
					httpResponseParamterLines += "Content-Length: "
							+ m_fileToReturn.length() + r_CRLF;
				}
				pageContent = getFileAsByteArray(m_fileToReturn);
			}

			break;
		case NotImplemented:
			httpResponseInitialLine = buildInitialLineOfHttpResponse(
					r_notImplementedString, m_httpVersion);
			break;
		case NotFound: {
			httpResponseInitialLine = buildInitialLineOfHttpResponse(
					r_notFoundString, m_httpVersion);
		}
			break;
		case BadRequest:
			httpResponseInitialLine = buildInitialLineOfHttpResponse(
					r_badRequestString, m_httpVersion);
			break;

		case InternalServerError:
			httpResponseInitialLine = buildInitialLineOfHttpResponse(
					r_internalServerError, m_httpVersion);
			break;
		default:
			httpResponseInitialLine = buildInitialLineOfHttpResponse(
					r_badRequestString, r_defaultVersion);
			break;
		}

		// print the response header
		String response = httpResponseInitialLine + httpResponseParamterLines
				+ r_CRLF;
		System.out.println(response);
		// write the response
		m_os = new DataOutputStream(m_socket.getOutputStream());
		byte[] responseBytes = response.getBytes();
		m_os.write(responseBytes);

		if (pageContent != null) {
			if (isChunked) {
				sendChunkedFile(pageContent);
			} else {
				m_os.write(pageContent);
				m_os.write(r_CRLF.getBytes());
				m_os.flush();
			}
		}

	}

	private void sendChunkedFile(byte[] pageContent) throws Exception {

		int maxSizeChunk = 50;
		String endOfChunkedFileSending = "0" + r_CRLF + r_CRLF;
		int startIndex = 0;
		int contentLengthRemaining = pageContent.length;
		byte[] currChunkContent = null;
		int currChunkContentLength;
		while (contentLengthRemaining > 0) {
			if (contentLengthRemaining >= maxSizeChunk)
				currChunkContent = new byte[maxSizeChunk];
			else
				currChunkContent = new byte[contentLengthRemaining];
			currChunkContentLength = currChunkContent.length;
			System.arraycopy(pageContent, startIndex, currChunkContent, 0,
					currChunkContentLength);
			m_os.write(Integer.toHexString(currChunkContentLength).getBytes());
			m_os.write(r_CRLF.getBytes());
			m_os.write(currChunkContent);
			m_os.write(r_CRLF.getBytes());

			contentLengthRemaining -= currChunkContentLength;
			startIndex += currChunkContentLength;
			currChunkContent = null;
		}
		m_os.write(endOfChunkedFileSending.getBytes());
	}

	private String buildInitialLineOfHttpResponse(String responseCode,
			String version) {
		if (version == null) {
			version = r_defaultVersion;
		}
		String initialLineOfHttpResponse = "HTTP/" + version + " "
				+ responseCode + r_CRLF;
		return initialLineOfHttpResponse;
	}

	private byte[] getFileAsByteArray(File file) throws IOException {
		byte[] bFile = null;

		FileInputStream fis = new FileInputStream(file);
		bFile = new byte[(int) file.length()];

		// read until the end of the stream.
		while (fis.available() != 0) {
			fis.read(bFile, 0, bFile.length);
		}
		fis.close();
		return bFile;
	}

	private String readRequest() throws IOException {
		String request = "";
		m_is = new DataInputStream(m_socket.getInputStream());

		m_bufferedReader = new BufferedReader(new InputStreamReader(m_is));
		String line = null;

		while ((line = m_bufferedReader.readLine()) != null
				&& line.length() > 0) {
			request += line + "\n";
		}

		return request;
	}

	private void parsePostParams(String parametersToParse) {
		String reqired = parametersToParse + "&";

		// in order to more easily parse the parameters an & is
		// appended to the parameters part
		Pattern splitGetPostParamsPattern = Pattern.compile("([^=]*)=([^&]*)&");
		Matcher splitGetPostParamsMatcher = splitGetPostParamsPattern
				.matcher(reqired);
		while (splitGetPostParamsMatcher.find()) {
			String key = splitGetPostParamsMatcher.group(1);
			String value = splitGetPostParamsMatcher.group(2);
			m_getPostParamsHashMap.put(key, value);
		}
	}

	public String buildParamsInfoHtmlForResponse() {
		StringBuilder builder = new StringBuilder();
		builder.append("<html><body><table border=\"1\"><tr><td>name</td><td>value</td></tr>");
		Set<String> keySet = m_getPostParamsHashMap.keySet();
		for (String key : keySet) {
			builder.append("<tr><td>");
			builder.append("" + key);
			builder.append("</td><td>");
			builder.append(m_getPostParamsHashMap.get(key));
			builder.append("</td></tr>");
		}
		builder.append("</table></body></html>");
		return builder.toString();
	}

}
