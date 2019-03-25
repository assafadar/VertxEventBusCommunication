package org.crypto.communication.internal.verticle;

import org.crypto.communication.internal.utils.AppConfig;

import com.hazelcast.config.Config;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.spi.cluster.ClusterManager;
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager;

public class RouterVerticle extends AbstractVerticle {
	private static VertxDeployment vertxDeployment;
	
	@Override
	public void start(Future<Void> staFuture) {
		try {
			Class<?> clazz = Class.forName(AppConfig.appParams.get("RouterName"));
			clazz
					.getDeclaredConstructor(Vertx.class,Future.class)
					.newInstance(vertx,staFuture);
			
		}catch (Exception e) {
			// TODO: handle exception
			staFuture.fail(e);
		}
	}

	public static void main(String[] args) {
		try {
			AppConfig.setArgs(args);
			vertxDeployment = new VertxDeployment(new RouterVerticle());
			Config config = new Config();
			try {
				config.getGroupConfig().setName(AppConfig.appParams.get("ClusterGroup"));
			} catch (Exception e) {
				// TODO: handle exception
			}
			ClusterManager clusterManager = new HazelcastClusterManager(config);
			VertxOptions vertxOptions = new VertxOptions().setClustered(true).setClusterManager(clusterManager);

			Vertx.clusteredVertx(vertxOptions, resultHandler -> {
				if (resultHandler.succeeded()) {
					vertxDeployment.deploy(resultHandler.result());
				} else {
					resultHandler.cause().printStackTrace();
					System.out.println("-----------------ERROR deploying RouterVerticle---------------: "
					+resultHandler.cause().getMessage());
					System.exit(500);
				}
			});
		} catch (Exception e) {
			System.out.println("-----------------ERROR deploying RouterVerticle---------------");
			System.exit(500);
		}
	}

}
