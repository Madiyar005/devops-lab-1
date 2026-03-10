package com.example;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import com.google.gson.Gson;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.io.*;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.Executors;

public class App {
    private static final Gson gson = new Gson();
    private static JedisPool jedisPool;
    private static String hostname;
    
    public static void main(String[] args) throws IOException {
        // Хост атын алу
        try {
            hostname = java.net.InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            hostname = "unknown";
        }
        
        int port = 5000;
        System.out.println("Қолданба іске қосылуда... Порт: " + port);
        System.out.println("Хост: " + hostname);
        
        // Redis-ке қосылу
        connectToRedis();
        
        // HTTP серверін құру
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        
        // Маршруттарды тіркеу
        server.createContext("/", new RootHandler());
        server.createContext("/health", new HealthHandler());
        server.createContext("/info", new InfoHandler());
        server.createContext("/redis", new RedisHandler());
        
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();
        
        System.out.println("Сервер іске қосылды!");
        System.out.println("Маршруттар:");
        System.out.println("   - http://localhost:5000/");
        System.out.println("   - http://localhost:5000/health");
        System.out.println("   - http://localhost:5000/info");
        System.out.println("   - http://localhost:5000/redis");
    }
    
    /**
     * Redis-ке қосылу
     */
    private static void connectToRedis() {
        String redisHost = System.getenv().getOrDefault("REDIS_HOST", "localhost");
        int redisPort = Integer.parseInt(System.getenv().getOrDefault("REDIS_PORT", "6379"));
        
        System.out.println("Redis-ке қосылу: " + redisHost + ":" + redisPort);
        
        try {
            JedisPoolConfig poolConfig = new JedisPoolConfig();
            poolConfig.setMaxTotal(10);
            poolConfig.setTestOnBorrow(true);
            
            jedisPool = new JedisPool(poolConfig, redisHost, redisPort, 5000);
            
            // Қосылымды тексеру
            try (Jedis jedis = jedisPool.getResource()) {
                jedis.ping();
                System.out.println("Redis қосылды!");
            }
        } catch (Exception e) {
            System.out.println("Redis қосылмады: " + e.getMessage());
            jedisPool = null;
        }
    }
    
    /**
     * Негізгі бет
     */
    static class RootHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            Map<String, Object> response = new HashMap<>();
            
            // Redis счетчигін өсіру
            int hits = 0;
            if (jedisPool != null) {
                try (Jedis jedis = jedisPool.getResource()) {
                    hits = (int) jedis.incr("hits");
                } catch (Exception e) {
                    hits = -1;
                }
            }
            
            response.put("message", "Сәлем, DevOps! Бұл - Java қолданбасы");
            response.put("hits", hits);
            response.put("hostname", hostname);
            response.put("redis_connected", jedisPool != null);
            response.put("timestamp", System.currentTimeMillis());
            
            sendJson(exchange, 200, response);
        }
    }
    
    /**
     * Health check
     */
    static class HealthHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            Map<String, Object> response = new HashMap<>();
            
            boolean redisOk = false;
            
            // Redis тексеру
            if (jedisPool != null) {
                try (Jedis jedis = jedisPool.getResource()) {
                    jedis.ping();
                    redisOk = true;
                } catch (Exception e) {
                    // Redis жоқ
                }
            }
            
            response.put("status", redisOk ? "healthy" : "degraded");
            response.put("redis", redisOk ? "connected" : "disconnected");
            
            int statusCode = redisOk ? 200 : 503;
            sendJson(exchange, statusCode, response);
        }
    }
    
    /**
     * Ақпарат
     */
    static class InfoHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            Map<String, Object> response = new HashMap<>();
            
            response.put("app", "Java DevOps Demo App");
            response.put("version", "1.0");
            response.put("hostname", hostname);
            response.put("java_version", System.getProperty("java.version"));
            response.put("os_name", System.getProperty("os.name"));
            response.put("available_processors", Runtime.getRuntime().availableProcessors());
            
            sendJson(exchange, 200, response);
        }
    }
    
    /**
     * Redis тест
     */
    static class RedisHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            Map<String, Object> response = new HashMap<>();
            
            if (jedisPool == null) {
                response.put("error", "Redis қосылмаған");
                sendJson(exchange, 503, response);
                return;
            }
            
            try (Jedis jedis = jedisPool.getResource()) {
                // Redis ақпаратын алу
                response.put("ping", jedis.ping());
                response.put("hits", jedis.get("hits"));
                response.put("info", jedis.info("server").split("\n")[0]);
                
                sendJson(exchange, 200, response);
            } catch (Exception e) {
                response.put("error", e.getMessage());
                sendJson(exchange, 500, response);
            }
        }
    }
    
    /**
     * JSON жауап жіберу
     */
    private static void sendJson(HttpExchange exchange, int statusCode, Object data) throws IOException {
        String json = gson.toJson(data);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, json.getBytes().length);
        
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(json.getBytes());
        }
    }
}
