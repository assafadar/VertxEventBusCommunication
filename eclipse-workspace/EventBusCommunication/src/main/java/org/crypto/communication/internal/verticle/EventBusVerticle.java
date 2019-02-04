package org.crypto.communication.internal.verticle;

import org.crypto.communication.internal.messages.EventBusMessage;
import org.crypto.communication.internal.net.EventBusNetworking;
import org.crypto.communication.internal.utils.MembersManager;

import com.hazelcast.config.Config;
import com.hazelcast.core.MembershipEvent;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.spi.cluster.ClusterManager;
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager;

public class EventBusVerticle extends AbstractVerticle implements IEventBusUser {
	private static VertxDeployment vertxDeployment;
	@Override
	public void start(Future<Void> startFuture) {
		try {
			EventBusNetworking.init(vertx, null);
			EventBusNetworking.getNetworking().getRouter()
			.get("clients", this::getEventBusClients);
			startFuture.complete();
		} catch (Exception e) {
			startFuture.fail(e);
		}
	}

	public static void main(String[] args) {
		AbstractVerticle verticle = new EventBusVerticle();
		vertxDeployment = new VertxDeployment(verticle);
		VertxOptions vertxOptions = new VertxOptions().setClustered(true);
		
		HazelcastClusterManager clusterManager = new HazelcastClusterManager();
		vertxOptions.setClusterManager(clusterManager);
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
	
	public void getEventBusClients(EventBusMessage message, Future<Object> future) {
		future.complete(MembersManager.getAllClients());
	}

}
