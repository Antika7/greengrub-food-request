package com.greengrub.food_request.exception;

public class FoodNotFoundException extends RuntimeException {
    public FoodNotFoundException(String id) {
        super("Food request not found with id: " + id);
    }
}
