package org.crypto.communication.internal.verticle;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;

public class VertxDeployment {
	private AbstractVerticle abstractVerticle;
    private Vertx vertx;
	
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
    	this.vertx.deploymentIDs().forEach(verticle -> {
    		this.vertx.undeploy(verticle);
    	});
    	this.vertx.close();
    }
    
    
}
