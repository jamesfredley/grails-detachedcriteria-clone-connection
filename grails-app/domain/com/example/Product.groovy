package com.example

class Product {
    String name
    BigDecimal price

    static mapping = {
        datasource 'ALL'
    }

    static constraints = {
        name nullable: false, blank: false
        price nullable: false
    }
}
