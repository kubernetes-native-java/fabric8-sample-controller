package com.example.operator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.nativex.hint.TypeHint;

import static org.springframework.nativex.hint.TypeAccess.*;

/**
	* This example was lifted <a href="https://itnext.io/writing-kubernetes-sample-controller-in-java-c8edc38f348f">
	* from the following blog</a>. Thank you, Rohan Kumar.
	*
	* @author Rohan Kumar
	* @author Josh Long
	*/
@TypeHint(
	access = {
		DECLARED_CLASSES, DECLARED_CONSTRUCTORS, DECLARED_FIELDS, DECLARED_METHODS,
		PUBLIC_CLASSES, PUBLIC_CONSTRUCTORS, PUBLIC_FIELDS, PUBLIC_METHODS
	},
	types = {
		Foo.class, FooList.class, FooStatus.class, FooSpec.class
	})
@SpringBootApplication
public class OperatorApplication {

	public static void main(String[] args) {
		SpringApplication.run(OperatorApplication.class, args);
	}

	@Bean
	Runner runner() {
		return new Runner();
	}

}


