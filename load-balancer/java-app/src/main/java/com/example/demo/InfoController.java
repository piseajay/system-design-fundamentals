package com.example.demo;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@RestController
public class InfoController {
  private final String instanceId = System.getenv().getOrDefault("INSTANCE_ID", UUID.randomUUID().toString());
  private final AtomicInteger counter = new AtomicInteger();

  @GetMapping("/")
  public Map<String, Object> info() throws UnknownHostException {
    return Map.of(
        "instanceId", instanceId,
        "requestNumber", counter.incrementAndGet(),
        "hostname", InetAddress.getLocalHost().getHostName()
    );
  }
}
