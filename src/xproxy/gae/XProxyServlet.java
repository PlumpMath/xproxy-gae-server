package xproxy.gae;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Author: kelvin_hu
 * Email:  ini.kelvin@gmail.com
 * Date:   09/23/2013
 */

public class XProxyServlet extends HttpServlet {
    private static final Logger log = Logger.getLogger(XProxyServlet.class.getName());

    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        log.info("A request received, remote host: " + request.getRemoteHost());

        StringBuilder sb = new StringBuilder();
        BufferedReader in = request.getReader();
        ServletOutputStream out = response.getOutputStream();
        try {
            String line;
            while((line = in.readLine()) != null) {
                sb.append(line).append('\n');
            }

            String rawRequest = sb.toString();
            log.info("------------------------------------\n"
                    + rawRequest
                    + "\n------------------------------------");
            String[] lines = rawRequest.split("\n"); // only split by \n, not \r\n
            String[] initLineItems = lines[0].split(" "); // TODO verification here
            String host = null;
            Map<String, String> headers = new HashMap<String, String>();
            for(int i = 1; i < lines.length; ++i) {
                log.info("line: " + lines[i]);
                if(lines[i].equals("\r")) // the headers are end
                    break;
                String[] headerItems = lines[i].split(": "); // TODO verification here
                headers.put(headerItems[0], headerItems[1]);
                if(headerItems[0].equals("Host"))
                    host = headerItems[1];
            }
            String uri = initLineItems[1];
            if(host == null || uri.startsWith("http://")) {
                host = uri;
            } else {
                if(host.endsWith("/"))
                    host = host.substring(0, host.length() - 1) + uri;
                else
                    host = host + uri;
            }
            log.info("now host is: " + host);
            URL url = new URL(host);
            HttpURLConnection connection = (HttpURLConnection)url.openConnection();
            connection.setDoOutput(true);
            log.info("Http method: " + initLineItems[0]);
            connection.setRequestMethod(initLineItems[0]);
            for(Map.Entry<String, String> header : headers.entrySet()) {
                log.info("Header: name: " + header.getKey() + ", value: " + header.getValue());
                connection.setRequestProperty(header.getKey(), header.getValue());
            }
            if(!lines[lines.length - 1].equals("\r")) { // so there is body
                OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream());
                writer.write(lines[lines.length - 1]);
                writer.close();
            }

            log.info("The remote peer returns: " + connection.getResponseCode());
            log.info("returned message: " + connection.getResponseMessage());
            response.setStatus(connection.getResponseCode());
            byte[] temp = new byte[1024];
            int len;
            for(Map.Entry<String, List<String>> header : connection.getHeaderFields().entrySet()) {
                log.info("return header: " + header.getKey() + ", value: " + header.getValue().get(0));
                response.setHeader(header.getKey(), header.getValue().get(0));
            }
            response.setHeader("Content-Encoding", "gzip");
            while((len = connection.getInputStream().read(temp)) >= 0) {
                response.getOutputStream().write(temp, 0, len);
            }
            connection.getInputStream().close();
        } finally {
            in.close();
            out.close();
        }
    }

    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/plain");
        response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        response.getWriter().println("Sorry, xProxy does not support HTTP GET method.");
    }

    private static void writeSpecificResponse(HttpServletResponse response, int statusCode, String message)
            throws IOException {
        response.setStatus(statusCode);
        response.setContentType("text/plain");
        response.getOutputStream().print(message);
    }

    private static void writeBadRequestResponse(HttpServletResponse response, String message)
            throws IOException {
        writeSpecificResponse(response, HttpServletResponse.SC_BAD_REQUEST,
                "Bad request:\n" + "============\n" + message);
    }
}
