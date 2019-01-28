package org.crypto.communication.internal.verticle;

import org.crypto.communication.internal.net.EventBusNetworking;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;

public class EventBusVerticle extends AbstractVerticle {
	@Override
	public void start(Future<Void> startFuture) {
		try {
			EventBusNetworking.init(vertx, null, null);
			startFuture.complete();
		}catch (Exception e) {
			startFuture.fail(e);
		}
	}
	
	public static void main(String[] args) {
		AbstractVerticle verticle = new EventBusVerticle();
		VertxOptions vertxOptions = new VertxOptions().setClustered(true);
		Vertx.clusteredVertx(vertxOptions, res ->{
			if(res.succeeded()) {
				Vertx vertx = res.result();
				vertx.deployVerticle(verticle);
			}else {
				res.cause().printStackTrace();
				System.exit(500);
			}
		});
		
	}
}
