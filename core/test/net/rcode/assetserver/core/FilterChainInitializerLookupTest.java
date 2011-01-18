package net.rcode.assetserver.core;

import static org.junit.Assert.assertTrue;
import net.rcode.assetserver.ejs.EjsResourceFilter;
import net.rcode.assetserver.optimizer.YuiOptimizeCssResourceFilter;
import net.rcode.assetserver.optimizer.YuiOptimizeJsResourceFilter;

import org.junit.Test;

public class FilterChainInitializerLookupTest {

	@Test
	public void testInstantiateBuiltins() {
		FilterChainInitializerLookup lookup=new FilterChainInitializerLookup();
		lookup.addBuiltins();
		
		assertTrue(lookup.lookup("ignore") instanceof IgnoreResourceFilter);
		assertTrue(lookup.lookup("#std-ignore") instanceof IgnoreResourceFilter);

		assertTrue(lookup.lookup("ejs") instanceof EjsResourceFilter);
		assertTrue(lookup.lookup("#std-ejs") instanceof EjsResourceFilter);

		assertTrue(lookup.lookup("jsoptimize") instanceof YuiOptimizeJsResourceFilter);
		assertTrue(lookup.lookup("#yui-jsoptimize") instanceof YuiOptimizeJsResourceFilter);

		assertTrue(lookup.lookup("cssoptimize") instanceof YuiOptimizeCssResourceFilter);
		assertTrue(lookup.lookup("#yui-cssoptimize") instanceof YuiOptimizeCssResourceFilter);
		
	}
}
