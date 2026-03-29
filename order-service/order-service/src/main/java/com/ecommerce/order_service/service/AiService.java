package com.ecommerce.order_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.ecommerce.order_service.dto.ProductDTO;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiService {

    private final ChatClient.Builder chatClientBuilder;
    private final RestTemplate restTemplate;
    private final OrderService orderService;

    public String chat(String userPrompt) {
        try {
            // Step 1: Ask Gemini to extract intent + details as JSON
            String systemPrompt = """
                    You are an AI assistant for an ecommerce platform.
                    Analyze the user's message and respond ONLY with a JSON object like this:
                    
                    {
                      "intent": "PLACE_ORDER" | "CHECK_STATUS" | "CHECK_STOCK" | "ORDER_HISTORY | "CANCEL_ORDER" | "CHECK_AMOUNT",
                      "name": "extracted product name or null",
                      "quantity": extracted quantity as number or null,
                      "orderId": extracted order id as number or null
                    }
                    
                    Examples:
                    "Order 2 iPhones" → {"intent":"PLACE_ORDER","name":"iPhone","quantity":2,"orderId":null}
                    "What is status of order 123?" → {"intent":"CHECK_STATUS","name":null,"quantity":null,"orderId":123}
                    "Is iPhone available?" → {"intent":"CHECK_STOCK","name":"iPhone","quantity":null,"orderId":null}
                    "Show my recent orders" → {"intent":"ORDER_HISTORY","name":null,"quantity":null,"orderId":null}
                    "Cancel order 123" → {"intent":"CANCEL_ORDER","name":null,"quantity":null,"orderId":123}
                    "What is the price of iPhone?" → {"intent":"CHECK_AMOUNT","name":"iPhone","quantity":null,"orderId":null}
                    
                    Respond ONLY with the JSON, no extra text.
                    """;

            ChatClient chatClient = chatClientBuilder.build();

            String aiResponse = chatClient.prompt()
                    .system(systemPrompt)
                    .user(userPrompt)
                    .call()
                    .content();

            log.info("Gemini response: {}", aiResponse);

            // Step 2: Parse and handle intent
            return handleIntent(aiResponse, userPrompt);
        } catch (Exception e) {
            log.error("Error calling Gemini AI", e);
            return "AI Error: " + e.getMessage() + ". Please check if Gemini API key is configured in application.properties";
        }
    }

    private String handleIntent(String aiJson, String originalPrompt) {
        try {
            // Strip markdown code blocks if present (Gemini wraps JSON in ```json ... ```)
            String cleanJson = aiJson.trim();
            if (cleanJson.startsWith("```")) {
                // Remove ```json at start and ``` at end
                cleanJson = cleanJson.replaceAll("^```[a-z]*\\n?", "").replaceAll("\\n?```$", "");
            }
            
            // Parse JSON manually (or use ObjectMapper)
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode node = mapper.readTree(cleanJson);

            String intent = node.get("intent").asText();

            switch (intent) {
                case "PLACE_ORDER" -> {
                    String productName = node.get("name").asText();
                    int quantity = node.get("quantity").asInt();
                    return placeOrderByName(productName, quantity);
                }
                case "CHECK_STATUS" -> {
                    Long orderId = node.get("orderId").asLong();
                    return checkOrderStatus(orderId);
                }
                case "CHECK_STOCK" -> {
                    String productName = node.get("name").asText();
                    return checkStock(productName);
                }
                case "ORDER_HISTORY" -> {
                    return getOrderHistory();
                }
                case "CANCEL_ORDER" -> {
                    Long orderId = node.get("orderId").asLong();
                    return cancelOrder(orderId);
                }
                case "CHECK_AMOUNT" -> {
                    String productName = node.get("name").asText();
                    int quantity = node.get("quantity").asInt();
                    return checkAmount(productName, quantity);
                }
                // TODO: Add more intents like "RETURN_ORDER", "APPLY_COUPON", etc.
                default -> {
                    return "Sorry, I didn't understand that. Please try rephrasing your request.";
                }
            }
        } catch (Exception e) {
            log.error("Error parsing AI response", e);
            return "Sorry, something went wrong. Please try again!";
        }
    }

    private String placeOrderByName(String productName, int quantity) {
        try {
            // Find product by name from product service
            String url = "http://localhost:8081/products/get-all-products";
            ProductDTO[] products =
                    restTemplate.getForObject(url, ProductDTO[].class);

            log.info("Fetched products for placing order: {}", (Object) products);

            if (products == null || products.length == 0) return "No products found!";

            // Find matching product
            for (ProductDTO product : products) {
                if (product.getName().toLowerCase().contains(productName.toLowerCase())) {
                    String result = orderService.placeOrder(product.getId(), quantity);
                    return result;
                }
            }
            return "Product '" + productName + "' not found!";
        } catch (Exception e) {
            return "Failed to place order: " + e.getMessage();
        }
    }

    private String checkOrderStatus(Long orderId) {
        return orderService.getOrderStatus(orderId);
    }

    private String checkStock(String productName) {
        try {
            String url = "http://localhost:8081/products/get-all-products";
            ProductDTO[] products =
                    restTemplate.getForObject(url, ProductDTO[].class);

            if (products == null) return "Could not fetch products!";

            for (ProductDTO product : products) {
                if (product.getName().toLowerCase().contains(productName.toLowerCase())) {
                    return product.getName() + " — Stock: " + product.getStock() + " units available";
                }
            }
            return "Product '" + productName + "' not found!";
        } catch (Exception e) {
            return "Failed to check stock: " + e.getMessage();
        }
    }

    private String getOrderHistory() {
        return orderService.getAllOrders();
    }

    private String cancelOrder(Long orderId) {
        return orderService.cancelOrder(orderId);
    }

    private String checkAmount(String productName, int quantity) {
        try {
            String url = "http://localhost:8081/products/get-all-products";
            ProductDTO[] products =
                    restTemplate.getForObject(url, ProductDTO[].class);

            if (products == null) return "Could not fetch products!";

            for (ProductDTO product : products) {
                if (product.getName().toLowerCase().contains(productName.toLowerCase())) {
                    return product.getName() + " — Price: $" + (product.getPrice() * quantity);
                }
            }
            return "Product '" + productName + "' not found!";
        } catch (Exception e) {
            return "Failed to check amount: " + e.getMessage();
        }
    }
}