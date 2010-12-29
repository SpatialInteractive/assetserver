package net.rcode.assetserver.optimizer;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;

import net.rcode.assetserver.core.AssetLocator;
import net.rcode.assetserver.core.BufferAssetLocator;
import net.rcode.assetserver.core.FilterChain;
import net.rcode.assetserver.core.ResourceFilter;
import net.rcode.assetserver.util.BlockOutputStream;
import net.rcode.assetserver.util.YuiCompressor;

/**
 * Invoke the YUI JavaScript optimizer on the resource
 * @author stella
 *
 */
public class YuiOptimizeJsResourceFilter extends ResourceFilter {
	private YuiCompressor compressor;
	
	public YuiOptimizeJsResourceFilter() {
		super("yuioptimizejs");
		compressor=new YuiCompressor();
	}

	@Override
	public AssetLocator filter(FilterChain context, AssetLocator source)
			throws Exception {
		String encoding=source.getCharacterEncoding();
		if (encoding==null) encoding="UTF-8";
		Reader input=new BufferedReader(new InputStreamReader(source.openInput(), encoding));
		
		BlockOutputStream blockOut=new BlockOutputStream();
		Writer output=new OutputStreamWriter(blockOut, encoding);
		
		compressor.compressJs(input, output);
		output.flush();
		
		BufferAssetLocator ret=new BufferAssetLocator(blockOut);
		ret.setCharacterEncoding(encoding);
		ret.setContentType("text/javascript");
		ret.setShouldCache(true);
		
		return ret;
	}

}
