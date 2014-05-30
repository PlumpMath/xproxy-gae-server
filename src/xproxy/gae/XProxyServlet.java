package xproxy.gae;

import com.google.appengine.api.urlfetch.*;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
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
                sb.append(line).append("\r\n");
            }

            String rawRequest = sb.toString();
            log.info("------------------------------------\n"
                    + rawRequest
                    + "\n------------------------------------");

            int eoi = rawRequest.indexOf("\r\n"); // eoi means end of initial line
            if(eoi == -1) {
                writeBadRequestResponse(response, rawRequest);
                log.severe("Cannot locate initial line.");
                return;
            }
            String initialLine = rawRequest.substring(0, eoi);

            rawRequest = rawRequest.substring(eoi + 2);

            int eoh = rawRequest.indexOf("\r\n\r\n"); // eoh means end of headers
            if(eoh == -1) {
                writeBadRequestResponse(response, rawRequest);
                log.severe("Cannot locate header string.");
                return;
            }
            String headerString = rawRequest.substring(0, eoh);

            rawRequest = rawRequest.substring(eoh + 4);
            String body = rawRequest;

            int eom = initialLine.indexOf(' '); // eom: end of method
            if(eom == -1) {
                writeBadRequestResponse(response, rawRequest);
                log.severe("Invalid initial line: " + initialLine);
                return;
            }
            String method = initialLine.substring(0, eom);
            initialLine = initialLine.substring(eom + 1);
            int eou = initialLine.indexOf(' '); // eou: end of uri
            if(eou == -1) {
                writeBadRequestResponse(response, rawRequest);
                log.severe("Invalid initial line: " + initialLine);
                return;
            }
            String uri = initialLine.substring(0, eou);

            List<HTTPHeader> headers = new ArrayList<HTTPHeader>();
            String[] headerLines = headerString.split("\r\n");
            String host = null;
            for(String h : headerLines) {
                int eon = h.indexOf(": "); // eon: end of name
                if(eon == -1) {
                    log.warning("Invalid header found: " + h);
                    continue;
                }
                HTTPHeader header = new HTTPHeader(h.substring(0, eon), h.substring(eon + 2));
                headers.add(header);
                if(header.getName().equalsIgnoreCase("Host")) {
                    host = header.getValue();
                }
            }

            String url;
            String schema = request.getHeader("XProxy-Schema");
            if(schema == null)
                schema = "http://";
            if(host == null || uri.startsWith(schema)) {
                url = uri;
            } else {
                url = schema + host + uri;
            }
            log.info("The remote url is: " + url);

            HTTPRequest req = new HTTPRequest(new URL(url), HTTPMethod.valueOf(method));
            for(HTTPHeader header : headers) {
                req.setHeader(header);
            }
            if(!body.isEmpty()) {
                req.setPayload(body.getBytes());
            }

            req.getFetchOptions().doNotFollowRedirects().disallowTruncate().validateCertificate();
            HTTPResponse resp = URLFetchServiceFactory.getURLFetchService().fetch(req);

            out.write(buildResponseStatusLine(resp));
            out.write(buildResponseHeaders(resp));

            if(resp.getContent() != null) {
                log.info("The remote response content length: " + resp.getContent().length);
                out.write(resp.getContent());
            }
            out.flush();
            log.info("The response has been written.");
        } catch(Exception e) {
            log.severe(e.toString());
        } finally {
            in.close();
            out.close();
        }
    }

    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        writeSpecificResponse(response,
                HttpServletResponse.SC_METHOD_NOT_ALLOWED,
                "Method not allowed:\n" +
                "===================\n" +
                "Sorry, xProxy does not support HTTP GET method.");
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

    private static byte[] buildResponseStatusLine(HTTPResponse response) {
        StringBuilder sb = new StringBuilder();
        sb.append("HTTP/1.1 "); // TODO do not hard code

        log.info("The remote response status code: " + response.getResponseCode());

        sb.append(response.getResponseCode());
        sb.append(" OK"); // TODO do not hard code the response status
        sb.append("\r\n");

        return sb.toString().getBytes();
    }

    private static byte[] buildResponseHeaders(HTTPResponse response) {
        StringBuilder sb = new StringBuilder();
        for(HTTPHeader header : response.getHeaders()) {
            log.info("The remote response header: " + header.getName() + ", value: " + header.getValue());
            if(header.getName().equalsIgnoreCase("Transfer-Encoding"))
                continue;
            sb.append(header.getName()).append(": ").append(header.getValue()).append("\r\n");
        }
        sb.append("\r\n");

        return sb.toString().getBytes();
    }
}
