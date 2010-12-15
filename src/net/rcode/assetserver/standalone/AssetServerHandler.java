package net.rcode.assetserver.standalone;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

/**
 * Jetty handler for implementing the asset server
 * 
 * @author stella
 *
 */
public class AssetServerHandler extends AbstractHandler {

	@Override
	public void handle(String target, Request baseRequest, HttpServletRequest request,
			HttpServletResponse response) throws IOException, ServletException {
		System.out.println("Target: " + target);
		response.setStatus(404);
	}

}
