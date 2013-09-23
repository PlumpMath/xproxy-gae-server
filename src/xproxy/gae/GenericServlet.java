package xproxy.gae;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class GenericServlet extends HttpServlet {
    public void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
		resp.setContentType("text/plain");
        resp.getWriter().println("Hello, xProxy GAE server is working.");
	}
}