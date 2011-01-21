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

public class YuiOptimizeCssResourceFilter extends ResourceFilter {
	private YuiCompressor compressor;
	
	public YuiOptimizeCssResourceFilter() {
		super("yuioptimizecss");
		compressor=new YuiCompressor();
	}

	@Override
	public AssetLocator filter(FilterChain context, AssetLocator source)
			throws Exception {
		if (context.getServer().isGlobalDisableOptimization()) return source;
		
		String encoding=source.getCharacterEncoding();
		if (encoding==null) encoding="UTF-8";
		Reader input=new BufferedReader(new InputStreamReader(source.openInput(), encoding));
		
		BlockOutputStream blockOut=new BlockOutputStream();
		Writer output=new OutputStreamWriter(blockOut, encoding);
		
		compressor.compressCss(input, output);
		output.flush();
		
		BufferAssetLocator ret=new BufferAssetLocator(blockOut);
		ret.setCharacterEncoding(encoding);
		ret.setContentType("text/css");
		ret.setShouldCache(true);
		
		return ret;
	}
	
	@Override
	public String toString() {
		return "YUI CSS Optimizer";
	}
}
