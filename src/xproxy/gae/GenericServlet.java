package xproxy.gae;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Author: kelvin_hu
 * Email:  ini.kelvin@gmail.com
 * Date:   09/23/2013
 */

public class GenericServlet extends HttpServlet {
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
		response.setContentType("text/plain");
        response.getWriter().println("Hello, xProxy GAE server is working.");
	}
}