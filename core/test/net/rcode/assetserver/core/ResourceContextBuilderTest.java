package net.rcode.assetserver.core;

import net.rcode.assetserver.ejs.EjsResourceFilter;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class ResourceContextBuilderTest {
	private ResourceContextBuilder builder;
	private ResourceContext context;
	
	@Before
	public void setUp() {
		builder=new ResourceContextBuilder();
		
		context=new ResourceContext(null);
		FilterChainInitializerLookup lookup=new FilterChainInitializerLookup();
		lookup.addBuiltins();
		
		context.setFilterLookup(lookup);
	}
	
	@Test
	public void testInitialization() {
		new ResourceContextBuilder();
	}
	
	@Test
	public void testFilterMethod() {
		builder.evaluateAsAccess(context,
				"logger.info(String(filter.lookup('ejs')));",
				"test");
	}
	
	@Test
	public void testFilterOnMethod() {
		builder.evaluateAsAccess(context, 
				"filter.on('*.js', 'ejs')",
				"test");
		assertEquals(1, context.getFilters().size());
		assertEquals("NamePatternPredicate(*.js)", context.getFilters().get(0).predicate.toString());
		assertTrue(context.getFilters().get(0).initializer instanceof EjsResourceFilter);
	}
}
