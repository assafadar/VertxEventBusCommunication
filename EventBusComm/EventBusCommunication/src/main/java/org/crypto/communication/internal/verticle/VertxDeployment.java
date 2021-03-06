package org.crypto.communication.internal.verticle;

import org.crypto.communication.internal.log.EventBusLogger;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;

public class VertxDeployment {
	private AbstractVerticle abstractVerticle;
    private Vertx vertx;
    private String groupName;
	
    public VertxDeployment(AbstractVerticle abstractVerticle) {
		super();
		this.abstractVerticle = abstractVerticle;
		
	}
    
    public void deploy(Vertx vertx) {
    	this.vertx = vertx;
    	this.vertx.deployVerticle(abstractVerticle);
    }
    
    public void closeVertx() {
    	System.out.println("Start closing: "+this.vertx.deploymentIDs());
    	EventBusLogger.close();
    	this.vertx.deploymentIDs().forEach(verticle -> {
    		this.vertx.undeploy(verticle);
    	});
    	this.vertx.close();
    }
    
    
}
