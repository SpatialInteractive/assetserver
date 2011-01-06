package net.rcode.assetserver.svg;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

import net.rcode.assetserver.core.AssetLocator;
import net.rcode.assetserver.core.AssetPath;
import net.rcode.assetserver.core.BufferAssetLocator;
import net.rcode.assetserver.core.FilterChain;
import net.rcode.assetserver.core.ResourceFilter;
import net.rcode.assetserver.util.BlockOutputStream;

import com.kitfox.svg.SVGDiagram;
import com.kitfox.svg.SVGUniverse;

/**
 * Perform SVG rendering from an SVG file when parameters are present indicating rendering.
 * @author stella
 *
 */
public class SvgRenderResourceFilter extends ResourceFilter {
	public SvgRenderResourceFilter() {
		super("svgrender");
	}
	
	@Override
	public AssetLocator filter(FilterChain context, AssetLocator source)
			throws Exception {
		AssetPath ap=context.getAssetPath();
		String renderFormat=ap.getParameter("render"), mimeType;
		if (renderFormat==null) {
			// Do nothing
			return source;
		}
		mimeType=context.getServer().getMimeMapping().lookup(renderFormat);
		
		
		// Load SVG
		SVGUniverse svg=new SVGUniverse();
		InputStream svgIn=source.openInput();
		try {
			svg.loadSVG(svgIn, ap.getBaseName());
		} finally {
			svgIn.close();
		}
		
		SVGDiagram diagram=svg.getDiagram(context.getRootFile().toURI());
		BufferedImage image=new BufferedImage((int)diagram.getWidth(), (int)diagram.getHeight(), BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d=image.createGraphics();
		diagram.setIgnoringClipHeuristic(true);
		diagram.render(g2d);
		
		return outputImage(image, renderFormat, mimeType);
	}

	private AssetLocator outputImage(BufferedImage image, String renderFormat, String mimeType) throws IOException {
		Iterator<ImageWriter> iter=ImageIO.getImageWritersBySuffix(renderFormat);
		if (!iter.hasNext()) return null;	// Not found
		
		BlockOutputStream buffer=new BlockOutputStream();
		ImageWriter imageWriter=iter.next();
		try {
			ImageOutputStream iout=ImageIO.createImageOutputStream(buffer);
			imageWriter.setOutput(iout);
			imageWriter.write(image);
			iout.flush();
		} finally {
			imageWriter.dispose();
		}
		
		BufferAssetLocator ret=new BufferAssetLocator(buffer);
		ret.setContentType(mimeType);
		ret.setShouldCache(true);
		
		return ret;
	}

	@Override
	public String toString() {
		return "SVG Render Filter";
	}
}
