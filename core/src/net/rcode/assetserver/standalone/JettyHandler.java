package net.rcode.assetserver.standalone;

import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPOutputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.rcode.assetserver.VersionInfo;
import net.rcode.assetserver.core.AssetLocator;
import net.rcode.assetserver.core.AssetRoot;
import net.rcode.assetserver.core.AssetServer;
import net.rcode.assetserver.util.CountingOutputStream;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Jetty handler for implementing the asset server
 * 
 * @author stella
 *
 */
public class JettyHandler extends AbstractHandler {
	private static final Logger logger=LoggerFactory.getLogger(JettyHandler.class);
	private static final Logger accessLog=LoggerFactory.getLogger("accesslog");
	
	private static final Pattern DEFLATE_ACCEPT_PATTERN=Pattern.compile(
			"\\b(gzip|deflate)\\b"
			);
	
	private AssetServer server;
	private String serverHeader;
	
	public JettyHandler(AssetServer server) {
		this.server=server;
		
		VersionInfo vi=VersionInfo.INSTANCE;
		String version=vi.getBuildVersion();
		if ("dev".equals(version)) {
			version=version + "-" + vi.getBuildTime();
		}
		
		serverHeader="AssetServer (" + version + ")";
	}
	
	public String getServerHeader() {
		return serverHeader;
	}
	
	public void setServerHeader(String serverHeader) {
		this.serverHeader = serverHeader;
	}
	
	@Override
	public Server getServer() {
		return super.getServer();
	}
	
	@Override
	public void handle(String target, Request baseRequest, HttpServletRequest request,
			HttpServletResponse response) throws IOException, ServletException {
		server.enterRequestContext();
		try {
			handleInContext(target, baseRequest, request, response);
		} finally {
			server.exitRequestContext();
		}
	}
	
	/**
	 * Handle the request within a server enterRequestContext() / exitRequestContext() block
	 * @param target
	 * @param baseRequest
	 * @param request
	 * @param response
	 * @throws IOException
	 * @throws ServletException
	 */
	protected void handleInContext(String target, Request baseRequest, HttpServletRequest request,
			HttpServletResponse response) throws IOException, ServletException {
		// Set server header
		response.setHeader("Server", serverHeader);
		
		AssetRoot root=server.getRoot();
		if (root==null) {
			// Not found
			baseRequest.setHandled(false);
			return;
		}
		
		// Restrict based on method
		String method=request.getMethod();
		boolean isHead=false;
		if (method.equals("HEAD")) {
			// Handle HEAD request specially
			isHead=true;
		} else if (!method.equals("GET")) {
			// Only allow GET
			baseRequest.setHandled(false);
			return;
		}
		
		AssetLocator locator;
		try {
			locator=root.resolve(request.getRequestURI());
		} catch (ServletException e) {
			throw e;
		} catch (IOException e) {
			throw e;
		} catch (Exception e) {
			logger.warn("Uncaught exception while processing request for " + request.getRequestURI(), e);
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Uncaught exception while processing request: " + e.getMessage());
			logAccess(request, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, -1);
			return;
		}
		
		if (locator==null) {
			// Not found
			baseRequest.setHandled(false);
			return;
		}
		
		// Check etag
		String resourceEtag=locator.getETag();
		if (resourceEtag!=null) {
			String ifNoneMatch=request.getHeader("If-None-Match");
			if (resourceEtag.equals(ifNoneMatch)) {
				// 304 not modified
				baseRequest.setHandled(true);
				response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
				logAccess(request, HttpServletResponse.SC_NOT_MODIFIED, 0);
				//logger.info("Request etag='" + ifNoneMatch + "', Resource etag='" + resourceEtag + "'");
				return;
			}
			
			// Otherwise, send the etag
			response.setHeader("ETag", resourceEtag);
		}
		
		// If here, then the path belongs to a mount
		String contentType=locator.getContentType(), encoding=locator.getCharacterEncoding();
		if (contentType!=null) {
			response.setContentType(contentType);
		}
		if (encoding!=null) {
			response.setCharacterEncoding(encoding);
		}
		
		baseRequest.setHandled(true);
		response.setStatus(200);
		
		// Detect GZIP
		String compressEncoding=null;
		String acceptEncoding=request.getHeader("Accept-Encoding");
		if (acceptEncoding!=null) {
			Matcher acceptMatcher=DEFLATE_ACCEPT_PATTERN.matcher(acceptEncoding);
			if (acceptMatcher.find()) {
				compressEncoding=acceptMatcher.group(1);
			}
		}
		
		// Disable GZIP for non-text resources
		if (compressEncoding!=null && contentType!=null) {
			if (!server.getMimeMapping().isTextualMimeType(contentType))
				compressEncoding=null;
		}
		
		// Add cache headers
		if (server.getConfig().isHttpNoCache()) {
			response.addHeader("Expires", "Fri, 30 Oct 1998 14:19:41 GMT");
			response.addHeader("Cache-Control", "max-age=0");
		}
		
		// Setup output filter for compression
		OutputStream out;
		CountingOutputStream countOut;
		GZIPOutputStream gzipOut=null;
		if (compressEncoding!=null) {
			// Enable gzip
			response.setHeader("Content-Encoding", compressEncoding);
			countOut=new CountingOutputStream(response.getOutputStream());
			out=gzipOut=new GZIPOutputStream(countOut);
		} else {
			out=countOut=new CountingOutputStream(response.getOutputStream());
		}
		
		// Write output
		if (!isHead) {
			locator.writeTo(out);
			if (gzipOut!=null) gzipOut.finish();
			out.flush();
		}
		
		logAccess(request, HttpServletResponse.SC_OK, countOut.size);
	}

	private void logAccess(HttpServletRequest request, int statusCode, long length) {
		SimpleDateFormat fmt=new SimpleDateFormat("dd/MMM/yyyy:HH:mm:ss Z");
		StringBuilder logRecord=new StringBuilder(256);
		logRecord.append(request.getRemoteAddr());
		logRecord.append(" - - [");
		logRecord.append(fmt.format(new Date()));
		logRecord.append("] \"");
		logRecord.append(request.getMethod());
		logRecord.append(" ");
		logRecord.append(request.getRequestURI());
		logRecord.append(" ");
		logRecord.append("HTTP/1.1\" ");
		logRecord.append(String.valueOf(statusCode));
		logRecord.append(" ");
		if (length<0) logRecord.append("-");
		else logRecord.append(String.valueOf(length));
		logRecord.append(" ");
		
		String referer=request.getHeader("Referer");
		if (referer!=null) {
			logRecord.append('"');
			logRecord.append(referer);
			logRecord.append('"');
		} else {
			logRecord.append('-');
		}
		
		logRecord.append(' ');
		String ua=request.getHeader("User-Agent");
		if (ua!=null) {
			logRecord.append('"');
			logRecord.append(ua);
			logRecord.append('"');
		} else {
			logRecord.append('-');
		}
		
		accessLog.info(logRecord.toString());
	}

}
