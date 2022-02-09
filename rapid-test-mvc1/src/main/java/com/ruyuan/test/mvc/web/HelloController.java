package com.ruyuan.test.mvc.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.zyk.rapid.client.RapidInvoker;
import com.zyk.rapid.client.RapidProtocol;
import com.zyk.rapid.client.RapidService;
import com.ruyuan.test.mvc.entity.TestEntity;

@RestController
@RapidService(patternPath = "/test*", protocol = RapidProtocol.HTTP, serviceId = "hello")
public class HelloController {

	private volatile int count;
	
	@RapidInvoker(path = "/testGet")
    @GetMapping("/testGet")
    public String testGet() {
        return "testGet";
    }
    
	@RapidInvoker(path = "/testPost")
    @PostMapping("/testPost")
    public String testPost() {
		count++;
		if(count >= 1e5) {
			System.err.println("<------ ruyuan: ------>");
			count = 0;
		}        
		return "ruyuan";
    }
    
	@RapidInvoker(path = "/testParam")
    @RequestMapping("/testParam")
    public String testParam(@RequestParam String name) {
		count++;
		if(count >= 1e5) {
			System.err.println("<------ testParam收到请求, name:" + name + " ------>");
			count = 0;
		}
    	return name;
    }
    
	@RapidInvoker(path = "/testEntity")
    @RequestMapping("/testEntity")
    public String testEntity(@RequestBody TestEntity testEntity) {
        String result = "testEntity result :" + testEntity.getName() + testEntity.getAge();
        return result;
    }

}
