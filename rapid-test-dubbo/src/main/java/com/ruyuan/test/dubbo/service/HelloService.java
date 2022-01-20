package com.ruyuan.test.dubbo.service;

public interface HelloService {

	User sayHelloUser(String name);

    User sayHelloUser(User user, String name);

    User sayHelloUser(User user);
    

}