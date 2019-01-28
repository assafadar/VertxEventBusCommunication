package org.crypto.communication.internal.net;




import java.util.logging.Level;

import org.crypto.communication.internal.log.EventBusLogger;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.vertx.core.Vertx;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
@RunWith(VertxUnitRunner.class)
public class EventBusNetworkingTest {
	
//	@BeforeClass
	public static void setUp(TestContext testContext) {
		Vertx vertx = Vertx.vertx();
		EventBusNetworking.init(vertx,null,null);
		testContext.async().complete();
	}
	
	@Test
	public void logger(TestContext ctx) {
		EventBusLogger.createLogger(getClass(),Level.SEVERE);
	}
	
	
	

}
