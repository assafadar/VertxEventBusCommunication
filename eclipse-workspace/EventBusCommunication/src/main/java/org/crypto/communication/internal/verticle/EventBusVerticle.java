package org.crypto.communication.internal.verticle;

import org.crypto.communication.internal.net.EventBusNetworking;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;

public class EventBusVerticle extends AbstractVerticle implements IEventBusUser {
	private static VertxDeployment vertxDeployment;
	@Override
	public void start(Future<Void> startFuture) {
		try {
			EventBusNetworking.init(vertx, null);
			startFuture.complete();
		} catch (Exception e) {
			startFuture.fail(e);
		}
	}

	public static void main(String[] args) {
		AbstractVerticle verticle = new EventBusVerticle();
		vertxDeployment = new VertxDeployment(verticle);
		VertxOptions vertxOptions = new VertxOptions().setClustered(true);
		Vertx.clusteredVertx(vertxOptions, res -> {
			if (res.succeeded()) {
				vertxDeployment.deploy(res.result());
			} else {
				res.cause().printStackTrace();
				System.exit(500);
			}
		});
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				vertxDeployment.closeVertx();
			}
		});

	}

	@Override
	public String verticleName() {
		return EventBusVerticle.class.getSimpleName();
	}

}
