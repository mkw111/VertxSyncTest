/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.example.starter;

import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;

import java.util.function.Consumer;

/**
 *
 * @author enernet
 */
public class Starter {
		public static void run(Class clazz) {

		String str=clazz.getName();
		System.out.println("Class to Start = "+str);
		Consumer<Vertx> runner = vertx -> {
			vertx.deployVerticle(clazz.getName());

		};

		VertxOptions vertx_options = new VertxOptions();

		Vertx vertx = Vertx.vertx(vertx_options);
		runner.accept(vertx);

    }
}
