package net.rcode.assetserver.standalone;

public class ServeCommand extends MainCommand {
	public static final String DESCRIPTION="Run http server";
	
	@Override
	public void invoke(String[] args) throws Throwable {
		ServerMain.main(args);
	}

}
